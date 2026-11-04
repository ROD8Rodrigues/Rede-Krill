# ADR-001 — Sessão com access token curto e refresh token rotativo em cookie `HttpOnly`

**Status:** Aceita
**Data:** 2026-11-02
**Escopo:** backend (Spring Security, `application/service/AuthSessionService`, `infrastructure/security`, `presentation/controller/AuthController`) e frontend (`services/api.js`, `stores/auth.js`)

---

## Contexto

A implementação original usava JWT puramente stateless: o token era emitido no corpo de `POST /api/auth/login`, guardado em `localStorage` pelo navegador e enviado como `Authorization: Bearer`. Esse modelo tem três defeitos conhecidos que a auditoria técnica registrou:

1. **Sem revogação.** Um token assinado vale até expirar; logout não invalidava nada e um token roubado continuava utilizável.
2. **Exposição a XSS.** `localStorage` é legível por qualquer script injetado, e o token era o único segredo da sessão.
3. **Validade longa.** 24 h (`86400s`) para um sistema de operação interna é uma janela de abuso desnecessária.

O desafio exige dois perfis (ADMIN/USER) e trilha de auditoria, o que pressupõe capacidade real de encerrar sessão e de saber qual sessão praticou qual ação.

## Decisão

Adotar sessão **com estado no servidor**, materializada em tokens de curta duração:

- **Access token**: JWT HMAC assinado, validade padrão **15 minutos** (`JWT_ACCESS_EXPIRATION`), entregue **exclusivamente** em cookie `HttpOnly` (`redekrill_access`, path `/`).
- **Refresh token**: opaco, 32 bytes de `SecureRandom`, validade padrão **7 dias** (`JWT_REFRESH_EXPIRATION`), entregue em cookie `HttpOnly` com **path restrito a `/api/auth`**.
- **Binding de revogação**: o JWT carrega a claim `sid` com o id da sessão. A autenticação exige `sessionId != null` **e** `authSessionService.isActive(sessionId)`; o usuário também é recarregado do banco a cada requisição, e `User::active` é revalidado.
- **Rotação de uso único**: cada `POST /api/auth/refresh` revoga a sessão anterior com `revokeIfActive` (UPDATE condicional) e cria uma nova. Reuso do mesmo refresh token falha com `401`. Desde 2026-11-03 o reuso também **revoga a família inteira** de sessões do usuário (`revokeAllByUserId`) e grava `REFRESH_TOKEN_REUSE` na auditoria — ver *Adendo* abaixo.
- **Persistência**: `auth_session` guarda apenas o **hash SHA-256** do refresh token com índice único; IP e User-Agent são registrados para investigação.
- **Corpo da resposta**: o token só aparece no JSON quando `AUTH_EXPOSE_TOKEN_IN_RESPONSE=true`, aceito apenas nos perfis `dev`/`test`. Em produção o validador de configuração recusa iniciar com essa flag ligada.
- **Encerramento**: `logout` revoga por hash e por `sid` e limpa os dois cookies; `change-password` revoga **todas** as sessões do usuário.
- **Frontend**: axios com `withCredentials: true` e **nenhum** uso de `localStorage` para credenciais. A hidratação do estado acontece por `GET /api/auth/me` no guard de rota; um `401` dispara um refresh serializado (uma única `Promise` compartilhada) e, se ele falhar, o handler de desautenticação leva o usuário ao login.

## Consequências

**Positivas**
- Logout, troca de senha, desativação de usuário e revogação em massa passam a ter efeito imediato, mesmo com access token ainda dentro da validade.
- Cookie `HttpOnly` remove o vetor de exfiltração de token por XSS; o path `/api/auth` no refresh reduz a superfície de envio do cookie de longa duração.
- A claim `sid` dá correlação entre token, linha de sessão e registro de auditoria.
- Access token de 15 min limita o valor prático de um token vazado por logs ou `Referer`.

**Negativas / custo**
- Uma consulta ao banco por requisição autenticada (leitura da sessão + do usuário). Aceito: o sistema é de operação interna, com carga baixa, e o ganho de revogabilidade vale o round-trip.
- O refresh exige um endpoint extra, serialização de chamadas no cliente e tratamento de corrida entre abas; a serialização atual é por aba (memória do módulo axios), não entre abas.
- A sessão sobrevive ao restart da aplicação (está no banco), mas o access token emitido com segredo efêmero de desenvolvimento morre — comportamento desejado e documentado.

**Riscos aceitos e registrados**
- ~~`SEC-04` do `SECURITY_REPORT.md`: reuso de refresh token é rejeitado, mas **não** revoga as demais sessões do usuário nem gera auditoria específica. Fechável em iteração futura.~~ **Fechado em 2026-11-03** — ver *Adendo: detecção de reuso com cascata* ao final.
- `SEC-01`: o lockout de login é local à instância. Com mais de uma réplica, o rate limit precisa de store compartilhado.
- Cookies exigem HTTPS em produção (`AUTH_COOKIE_SECURE=true`, imposto pelo validador) e CORS com `allowCredentials(true)` com origens explícitas.

## Evidência no código

| Comportamento | Arquivo |
|---|---|
| Criação/rotação/revogação de sessão | `backend/src/main/java/com/redekrill/scheduling/application/service/AuthSessionService.java:48-104` |
| Exigência de `sid` ativa + revalidação de usuário | `.../infrastructure/security/JwtAuthenticationFilter.java:56-77` |
| Geração de 32 bytes e hash SHA-256 | `.../infrastructure/security/SecureRefreshTokenGenerator.java:16-39` |
| Cookies `HttpOnly`, path e flags | `.../presentation/controller/AuthController.java:163-190` |
| Claim `sid` no access token | `.../infrastructure/security/JwtTokenService.java:47-63` |
| Tabela `auth_session` (hash único, IP, UA, índices) | `backend/src/main/resources/db/migration/V5__security_sessions_and_integrity.sql:13-27` |
| Clientes com credenciais, refresh serializado | `frontend/src/services/api.js` (`withCredentials`, `refreshSession`) |
| Segredo efêmero só em dev/test | `.../infrastructure/security/JwtTokenService.java:89-108` |

## Adendo: detecção de reuso com cascata (2026-11-03)

O risco `SEC-04` deste ADR dizia que o reuso era rejeitado mas inofensivo para as demais sessões. Passou a ser tratado assim, no `application` (sem vazar política para a borda HTTP):

| Passo | Onde |
|---|---|
| `rotate()` abre a transação, consome/valida a sessão e **devolve** um resultado em vez de lançar exceção de reuso | `application/service/AuthSessionService.java:79-92` |
| Reuso detectado (`session.revoked()` em `AuthSessionService.java:102-107` **ou** corrida em `revokeIfActive`, `:118-122`) → `revokeAllByUserId` na família inteira, dentro da mesma transação | `application/service/AuthSessionService.java:99-122` |
| Auditoria `REFRESH_TOKEN_REUSE` gravada **fora** da transação, depois do commit | `AuthSessionService.java:87-89` + `:126-131` |
| Exceção `AuthenticationFailedException` lançada **fora** da transação | `AuthSessionService.java:88-90` |

**Por que a ordem importa (armadilha real, não estilística).** Numa versão anterior, a revogação e a auditoria eram escritas e a exceção lançada *dentro* da mesma transação: `@Transactional` faz rollback em `RuntimeException` e **desfazia a revogação** — o atacante recebia `401` e as sessões roubadas continuavam ativas. O padrão adotado é: escrever o que precisa sobreviver dentro da transação, lançar fora dela. O mesmo critério vale para a auditoria, que não pode ser perdida junto com o rollback.

**Verificação.** Unitária: `AuthSessionServiceTest` (13 casos, incluindo a cascata). De runtime, contra o jar empacotado: replay do refresh consumido → `401 Sessão expirada ou revogada`, `auth_session` da vítima com **3 revogadas / 0 ativas**, a sessão legítima (token novo) também cai, e `REFRESH_TOKEN_REUSE` aparece em `GET /api/audit-logs`. Transcrição em `docs/VALIDACAO_FINAL.md` §3.3.

**Expiração do access token, agora testável.** `JwtTokenService` sempre recebeu um `Clock`, mas só o usava para **emitir**; a verificação comparava `exp` com o relógio interno do JJWT, então nenhum teste exercitava o ramo que rejeita token vencido (`SEC-30`). O parser passou a receber o mesmo `Clock` (`JwtTokenService.java:65-73`), e `JwtTokenServiceTest.rejectsTheTokenOnceTheExpirationHasPassed` cobre os dois lados da fronteira (exp−1 s aceito, exp+1 s rejeitado) em tempo determinístico.

## Alternativas consideradas

- **JWT stateless puro com denylist em Redis** — manteria a revogação, mas exigiria infra nova e ainda deixaria o token em `localStorage` (XSS) ou introduziria cookies de qualquer forma. Descartada por complexidade equivalente e benefício menor.
- **Sessão servlet com `JSESSIONID`** — revogação simples, mas perde a separação de camadas (o `application` ficaria dependente de HTTP), dificulta clientes de linha de comando e não combina com API + SPA com CORS.
- **Access token em memória do SPA + refresh em body** — protegia contra leitura por `localStorage`, mas não contra XSS que execute o próprio app, e não permite renovação transparente após recarregar a página.
