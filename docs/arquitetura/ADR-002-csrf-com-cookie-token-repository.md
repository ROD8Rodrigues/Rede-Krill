# ADR-002 — CSRF habilitado com cookie de token, excecionada apenas a requisições `Bearer`

**Status:** Aceita
**Data:** 2026-11-02
**Escopo:** backend (`infrastructure/security/SecurityConfig`, `BearerAuthorizationRequestMatcher`, `AuthController`) e frontend (`services/api.js`)

---

## Contexto

Enquanto a autenticação era `Authorization: Bearer` com token em `localStorage`, desabilitar CSRF era correto: o navegador não envia o header automaticamente em uma requisição forjada de outro site. Com a ADR-001, a credencial passou a ser **cookie `HttpOnly`** — exatamente o mecanismo que o navegador envia sozinho, o que torna um `POST` forjado de origem cruzada viável se nada impedir.

Ou seja: a melhoria de segurança da sessão **criou** a necessidade de proteção CSRF. Manter `csrf().disable()` seria regressão.

## Decisão

- Habilitar CSRF com **`CookieCsrfTokenRepository.withHttpOnlyFalse()`**, `path=/` e as flags `SameSite`/`Secure` herdadas de `app.auth` (`Lax` em dev, `Strict` em produção).
- O cookie `XSRF-TOKEN` fica legível ao JavaScript por design: é o padrão que permite ao SPA ecoar o valor no header `X-XSRF-TOKEN` sem exposição a CSRF, porque um site externo não consegue ler o cookie nem enviar o header.
- Expor `GET /api/auth/csrf` (público no matcher de autorização) retornando `{ token, headerName, parameterName }` como fallback para a primeira navegação, quando ainda não existe cookie a ser lido.
- **Exceção cirúrgica**: requisições que já portam `Authorization: Bearer` são ignoradas pela validação CSRF, via `BearerAuthorizationRequestMatcher`. Isso mantém utilitários de linha de comando e clientes de API funcionando sem token, e não abre o fluxo usado pelo navegador — que autentica por cookie.
- No frontend, o interceptor de request anexa `X-XSRF-TOKEN` em **todo** método mutante (`post`, `put`, `patch`, `delete`); a chamada de refresh também anexa o header explicitamente.
- A sessão HTTP permanece `SessionCreationPolicy.STATELESS`: não há `JSESSIONID`, portanto o token CSRF não está vinculado a uma sessão servlet.

## Consequências

**Positivas**
- Mutações autenticadas por cookie passam a exigir um segredo que só a própria origem consegue ler, fechando o vetor criado pela ADR-001.
- A exceção é estreita e testável: cabe em uma classe única (`BearerAuthorizationRequestMatcher`) em vez de uma lista de rotas soltas.
- `SameSite` funciona como defesa em profundidade, não como controle primário.

**Negativas / custo**
- Toda primeira mutação de uma sessão nova depende de obter o token (`ensureCsrf()`), adicionando um round-trip; o valor fica cacheado em memória do módulo.
- `withHttpOnlyFalse()` é frequentemente apontado em scanners como padrão a justificar — a justificativa está aqui e no comentário de código.
- Um `XSRF-TOKEN` legível por script é um insumo para ataque caso haja XSS **e** origem atacante capaz de emitir requisição — mas XSS já permite ler o DOM e chamar a API, então o ganho de `HttpOnly` nesse cookie seria ilusório.
- Com `SameSite=Strict` em produção, o cookie CSRF só é enviado em navegação same-site; o fluxo SPA normal não é afetado, mas links de e-mail que abrem o app em nova aba exigem uma navegação prévia antes da primeira mutação.

## Evidência no código

| Comportamento | Arquivo |
|---|---|
| Repositório de token + flags | `backend/src/main/java/com/redekrill/scheduling/infrastructure/security/SecurityConfig.java:56-65` |
| Exceção por header `Bearer` | `.../infrastructure/security/BearerAuthorizationRequestMatcher.java` |
| `GET /api/auth/csrf` público | `.../presentation/controller/AuthController.java:67-75` e `SecurityConfig.java:74-83` |
| Header em toda mutação | `frontend/src/services/api.js` (`MUTATING_METHODS`, `ensureCsrf`) |
| Header liberado no CORS | `SecurityConfig.java:126-129` (`X-XSRF-TOKEN`) |
| Cookie seguro por perfil | `backend/src/main/resources/application-prod.yml:10-16` |

## Alternativas consideradas

- **`SameSite=Strict` puro, sem token CSRF** — bloqueia o envio do cookie em requisições cross-site, mas não é controle: depende de navegador, não cobre subdomínios nem requisições de ferramentas, e a comunidade de segurança trata `SameSite` como reforço, não como substituto.
- **CSRF sincronizador (token em sessão)** — incompatível com `STATELESS` e exigiria estado de sessão servlet.
- **Header custom obrigatório (`X-Requested-With`) sem token** — bloqueia `simple request` de cross-origin, mas não protege contra preflight mal configurado nem oferece valor de seguridade auditável.
- **Validar `Origin`/`Referer` manualmente** — frágil atrás de proxies e com falsos positivos em operações de suporte; ficou reservado como reforço opcional, não como controle.
