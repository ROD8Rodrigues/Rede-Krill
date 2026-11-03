# Relatório de Segurança — Rede Krill Scheduling

**Sistema:** API de agendamento de caminhões e controle de pallets (Rede Krill)
**Documento:** `SECURITY_REPORT.md`
**Data da análise:** 2026-11-02
**Versão analisada:** `main` + alterações do checkpoint de segurança (Spring Boot 4.1.2, migrações até `V6__email_outbox.sql`)
**Método:** análise estática do código-fonte, revisão de configuração por perfil, inspeção de schema/migrações e leitura dos artefatos de teste e cobertura já produzidos no ambiente local.

> **O que este documento NÃO é.** Não é teste de penetração, não é varredura dinâmica contra uma instância em execução e não é certificação. Nenhuma requisição HTTP real foi disparada contra a aplicação durante esta análise. As afirmações abaixo têm origem em código e em artefatos de build verificáveis; onde não há evidência, isso está declarado como lacuna (seções 6, 8 e 9).

---

## 1. Sumário executivo

A superfície crítica de autenticação do sistema foi reformada em relação à auditoria técnica anterior (`AUDITORIA_TECNICA_REDE_KRILL.md`, 2026-11-02 14:00). Os sete problemas classificados como críticos naquela auditoria estão hoje **tratados ou parcialmente tratados**; nenhum segredo padrão permanece versionado em código, configuração ou infraestrutura.

| Dimensão | Postura atual | Confiança da avaliação |
|---|---|---|
| Autenticação e ciclo de sessão | Forte — access token curto (15 min) + refresh token rotativo de uso único, ambos em cookie `HttpOnly`, com revogação persistida | Alta (código lido + testes unitários passando) |
| Autorização (RBAC) | Forte — defesa em profundidade: regras por rota/perfil e mutações restritas a ADMIN | Alta (código lido) |
| Proteção de credencial (força bruta) | Boa em instância única — janela deslizante, lockout progressivo, `Retry-After`; **não compartilhada entre instâncias** | Média (cobertura do componente: 57,4%) |
| CSRF | Forte para o modelo cookie — token em cookie não-`HttpOnly` + header obrigatório em mutações | Alta |
| Gestão de segredos e configuração | Forte — `JWT_SECRET` sem default, validador de produção fail-fast, Swagger desabilitado por padrão | Alta |
| Tratamento de erros / vazamento de informação | Forte — sem stacktrace, sem mensagem técnica, `409` para conflito e lock otimista | Alta |
| Dependências (backend) | **Não verificável neste ambiente** — OWASP Dependency-Check configurado, mas nunca executado | Nula (sem evidência) |
| Dependências (frontend) | Boas — 0 vulnerabilidades em dependências de produção; 12 moderate em dev | Alta (`npm audit` executado) |
| Trilha de auditoria | Boa — 10 ações incluindo falha e bloqueio de login, troca de senha e acesso negado | Alta |
| Prontidão operacional de entrega | Parcial — documentação de entrega e validação de runtime pendentes | Alta (ausência confirmada em disco) |

**Veredito:** a aplicação está em condição razoável para operação interna controlada em instância única com HTTPS obrigatório. **Não** está em condição de afirmar ausência de CVEs em dependências backend (seção 6, `SEC-03`) nem de escala horizontal sem alterações (`SEC-01`).

---

## 2. Escopo e ambiente da análise

**Incluído:** módulo `backend` (Java/Spring), módulo `frontend` (Vue/axios), `docker-compose.yml`, `.env.example`, migrações Flyway `V1`–`V6`, workflows de CI e artefatos de teste/cobertura em `backend/target/`.

**Limitações de ambiente (registradas como fato, não como opinião):**

| Item | Estado real |
|---|---|
| Docker | Indisponível na máquina de análise (`docker` ausente do PATH) → `docker-compose.yml` **não foi validado** |
| OWASP Dependency-Check | Nunca executado: `backend/target/dependency-check` não existe e `NVD_API_KEY` não está definido |
| CI | `.github/workflows/ci.yml` existe, mas o trabalho analisado está não commitado no momento da análise → **nenhum pipeline validou este estado** |
| Testes em runtime | Não executados nesta análise; foram lidos os relatos de uma execução anterior (2026-11-02 22:20) |

---

## 3. Modelo de ameaças e superfície de ataque

### 3.1 Ativos protegidos

1. Credenciais e sessões de usuários (ADMIN/USER).
2. Dados de agendamento (fornecedor: nome, e-mail, ID Pedido, pallets, janela de recebimento).
3. Trilha de auditoria operacional.
4. Capacidade do centro de distribuição (limites 480/dia, 120/faixa).
5. Disponibilidade do endpoint de login.

### 3.2 Superfície exposta por padrão de configuração

| Rota | Acesso | Observação de segurança |
|---|---|---|
| `POST /api/auth/login` | Público | Protegido por rate limit por conta **e** por IP |
| `POST /api/auth/refresh` | Público (usa cookie de refresh) | Rotação de uso único; falha ⇒ `401` e nova autenticação |
| `POST /api/auth/logout` | Público no matcher, exige sessão válida na prática | Revoga sessão, limpa cookies, audita `LOGOUT` |
| `GET /api/auth/csrf` | Público | Entrega apenas o token CSRF da navegação |
| `GET /actuator/health`, `/actuator/info` | Público | `show-details: never`, probes habilitados |
| `GET /v3/api-docs/**`, `/swagger-ui*` | **ADMIN em produção**; público em dev | `application-prod.yml` desabilita springdoc por padrão (`SPRINGDOC_ENABLED:false`) |
| `/actuator/**` (exceto health/info) | ADMIN | Inclui `/actuator/prometheus` |
| `POST/PUT/DELETE /api/appointments/**` | ADMIN | Mutações exigem header CSRF |
| `POST/PUT/DELETE /api/truck-types/**` | ADMIN | Idem |
| `POST/PUT/DELETE /api/time-slots/**` | ADMIN | Idem |
| `POST/PUT/DELETE /api/palletization-types/**` | ADMIN | Idem |
| `/api/users/**`, `/api/audit-logs/**` | ADMIN | Leitura de PII e trilha restrita |
| Demais `GET /api/**` | Autenticado | `anyRequest().authenticated()` |

Evidência: `SecurityConfig.java:72-111` ([arquivo](file:///g/Christopher%20Chanquet/rede-krill-scheduling/backend/src/main/java/com/redekrill/scheduling/infrastructure/security/SecurityConfig.java#L72)).

---

## 4. Controles implementados

### 4.1 Autenticação e ciclo de sessão

- **Dois tokens.** Access token JWT assinado com HMAC (`HS256` pela biblioteca jjwt), validade padrão **15 min** (`JWT_ACCESS_EXPIRATION:15m`); refresh token opaco de 32 bytes gerados por `SecureRandom`, validade padrão **7 dias** (`JWT_REFRESH_EXPIRATION:7d`).
- ** binds de sessão.** O JWT carrega a claim `sid` (identificador da sessão). A autenticação só é aceita se `sessionId != null` **e** `authSessionService.isActive(sessionId)` — ou seja, um access token ainda não expirado deixa de funcionar assim que a sessão é revogada. Isso elimina a principal fraqueza do modelo stateless puro. Evidência: `JwtAuthenticationFilter.java:56-77`, `JwtTokenService.java:57-59`.
- **Rotação de uso único.** Em `refresh`, a sessão antiga é revogada com `revokeIfActive` (operação condicional, atômica por linha); se a revogação falhar, a tentativa é tratada como reuso e rejeitada. Evidência: `AuthSessionService.java:52-75`.
- **Persistência segura do refresh.** Apenas o **hash SHA-256** do refresh token vai para o banco (`uk_auth_session_refresh_hash UNIQUE`); o valor bruto nunca é armazenado. Evidência: `SecureRefreshTokenGenerator.java:27-39`, `V5__security_sessions_and_integrity.sql:13-27`.
- **Encerramento.** `logout` revoga por hash e por `sid`, `change-password` revoga **todas** as sessões do usuário (`revokeAllForUser`), e `user inactive` derruba autenticação em qualquer requisição (`User::active`).
- **Coeficiente de IP/UA** registrado em `auth_session.ip_address` / `user_agent` para investigação forense.

### 4.2 Armazenamento de credenciais

- Senhas com **BCrypt** (`BCryptPasswordEncoder` com força padrão 10). Evidência: `SecurityBeansConfig.java:11-13`. *Work factor não parametrizável — ver `SEC-07`.*
- Comprimento máximo de senha limitado a 72 caracteres na troca, respeitando o corte silencioso do BCrypt (mitiga truncamento). Evidência: `ChangePasswordUseCase.java:84-86`.
- Prevenção de enumeração de usuários no login: mesma mensagem genérica para usuário inexistente, inativo e senha inválida (`GENERIC_FAILURE`), enquanto a distinção fica apenas na auditoria interna. Evidência: `LoginUseCase.java:59-68`.

### 4.3 CSRF

- `CookieCsrfTokenRepository.withHttpOnlyFalse()` com `path=/`, `SameSite` e `Secure` herdados de `app.auth` — cookie legível pelo SPA, header `X-XSRF-TOKEN` obrigatório.
- Mutações no frontend sempre anexam o header, buscando o token primeiro no cookie e, na ausência, em `GET /api/auth/csrf`. Evidência: `frontend/src/services/api.js` (`ensureCsrf`, interceptor de request).
- Exceção restrita a requisições que já portam `Authorization: Bearer` (`BearerAuthorizationRequestMatcher`) — mantém utilitários de linha de comando e clientes de API que autenticam por header funcionando sem token CSRF, sem abrir essa exceção no fluxo de cookie usado pelo navegador. Evidência: `SecurityConfig.java:56-65`.
- Sessão HTTP permanece `STATELESS`, portanto não há CSRF de sessão servlet.

### 4.4 Autorização e RBAC

- Perfis `ADMIN`/`USER` projetados como `ROLE_<nome>` a partir do usuário **carregado do banco** a cada requisição, não da claim do token — remoção de privilégio tem efeito imediato. Evidência: `JwtAuthenticationFilter.java:60-69`.
- Regras declaradas por método + prefixo de rota; escrita em qualquer recurso de negócio é ADMIN.
- `PasswordChangeRequiredFilter` corta o uso normal da conta com senha inicial pendente, liberando apenas `/api/auth/me|logout|change-password|csrf`, e responde `403` com código estável `PASSWORD_CHANGE_REQUIRED`. Evidência: `PasswordChangeRequiredFilter.java:28-62`.
- O seed administrativo aplica `requirePasswordChange` (`SEED_ADMIN_REQUIRE_PASSWORD_CHANGE`), fechando o vetor "senha default conhecida".

### 4.5 Rate limiting e lockout de login

- Duplo contador (por e-mail e por IP) com **janela deslizante** (`Deque<Instant>`), limiar padrão 5 tentativas por conta / 20 por IP em 15 min.
- Bloqueio **progressivo e temporário** (multiplicador até 4× do `login-lockout`), evitando lockout permanente e autonegação de serviço.
- Resposta `429` com `Retry-After` e código `RATE_LIMITED`; falhas e bloqueios são auditados (`LOGIN_FAILED`, `LOGIN_BLOCKED`).
- Poda de estado acima de 10.000 entradas para limitar crescimento de memória.
- Evidência: `LoginAttemptService.java:40-114`, `GlobalExceptionHandler.java:132-147`, `application.yml:50-55`.

### 4.6 Política de senha

| Contexto | Regra aplicada |
|---|---|
| Criação de usuário (ADMIN) | mínimo 8, máximo 72 caracteres |
| Troca de senha (próprio usuário) | mínimo **12**, máximo 72, exige letra **e** número, bloqueia 5 senhas conhecidas, proíbe reuso da senha atual, exige senha atual correta |

Evidência: `CreateUserRequest.java:19-21` + `CreateUserUseCase.java:76-78`; `ChangePasswordUseCase.java:20-23,79-95`.
**Assimetria registrada como achado** (`SEC-05`).

### 4.7 Hardening de configuração e segredos

- `JWT_SECRET` sem default no `application.yml` (`${JWT_SECRET:}`): em perfis não-`dev`/`test` a aplicação **recusa subir** sem segredo; em dev/test gera segredo efêmero com aviso explícito de que tokens morrem no restart. Evidência: `JwtTokenService.java:86-103`.
- `ProductionConfigurationValidator` (`@Profile("prod")`, `@PostConstruct`) faz fail-fast em: `DB_PASSWORD` ausente ou igual a valor padrão conhecido, `JWT_SECRET` ausente/inválido/< 256 bits, `AUTH_COOKIE_SECURE=false`, `AUTH_EXPOSE_TOKEN_IN_RESPONSE=true`, `SEED_ADMIN_PASSWORD` fraca quando o seed está ativo. Não loga os valores analisados.
- `docker-compose.yml` exige segredo: `JWT_SECRET: ${JWT_SECRET:?...}`; não há mais default de senha de banco no compose.
- Produção: cookies `Secure` + `SameSite=Strict`, springdoc desabilitado, `show-sql:false`, `flyway.clean-disabled:true`, log estruturado (`ecs`), `forward-headers-strategy:native` com confiança em `X-Forwarded-For` **opt-in** (`TRUST_FORWARDED_HEADERS`, default `false`) — evita spoofing de IP no rate limit atrás de proxy desconfigurado.
- Erro do servidor sem vazamento: `server.error.include-message: never`, `include-stacktrace: never`.

### 4.8 Tratamento de erros e integridade

- Erros padronizados em `ApiError` com `code` de máquina (`RATE_LIMITED`, `PASSWORD_CHANGE_REQUIRED`, …).
- `DataIntegrityViolationException` ⇒ `409` com mensagem neutra; a causa técnica vai apenas para o log (mitiga vazamento de SQL em `orderId` duplicado). `OptimisticLockingFailureException` ⇒ `409` orientando recarga — trata a concorrência de ID Pedido e de edição simultânea apontadas na auditoria anterior (`C8`).
- Colunas `version` em `users` e `appointment` (`V5`) habilitam lock otimista.
- Falha inesperada ⇒ `500` com mensagem genérica e log com stacktrace + `correlationId`.

### 4.9 Observabilidade e trilha de auditoria

- `CorrelationIdFilter`: reusa ou gera `X-Correlation-ID`, publica no `MDC` e devolve no header; o padrão de log inclui `[%X{correlationId}]` — correlação de incidente sem expor payload.
- `CORS` expõe `X-Correlation-ID` ao navegador.
- Auditoria com 10 ações: `CREATE, UPDATE, DELETE, CANCEL, LOGIN, LOGIN_FAILED, LOGIN_BLOCKED, LOGOUT, PASSWORD_CHANGE, ACCESS_DENIED`; sem gravação de senha ou token (verificado por inspeção de `AuditAction` e dos pontos de registro).
- Métricas do outbox via Micrometer (`email_outbox_processed_total{status=sent|failed|retry}`) + `/actuator/prometheus` restrito a ADMIN.

### 4.10 E-mail (outbox transacional)

- Mensagem criada na **mesma transação** do agendamento (`V6`), envio fora da transação, retry exponencial (`initialBackoffSeconds * 2^attempts`, deslocamento limitado a 10), estados `PENDING/SENT/FAILED` com `CHECK`, `attempts >= 0`, `last_error` limitado a 1000 caracteres.
- Nenhuma mensagem é removida → reconstrução histórica possível, com custo de retenção de e-mail de fornecedor (ver `SEC-08`).

---

## 5. Resultados de verificação (evidência mensurada)

| Métrica | Valor | Fonte da evidência |
|---|---|---|
| Testes de backend | **125 passando** — 0 falhas, 0 erros, 0 skipped (25 suítes) | `backend/target/surefire-reports/TEST-*.xml`, carimbo 2026-11-02 22:20 |
| Cobertura agregada dos pacotes críticos do gate JaCoCo | **87,77%** (1966/2240 linhas) contra exigência de 80% | `backend/target/site/jacoco/jacoco.csv` |
| Cobertura global de linhas | 77,6% (8318 cobertas / 2406 não cobertas) | idem |
| Casos de segurança automatizados | `LoginUseCaseTest` (8), `ChangePasswordUseCase` coberto a 68,5%, `ArchitectureTest` (4), `SchedulingApiIntegrationTest` (10) | surefire + jacoco |
| `npm audit` — dependências de produção | **0 vulnerabilidades** | comando executado em `frontend/` nesta análise |
| `npm audit` — total (incl. dev) | 12 moderate, todas em devDependencies (cadeia `minimatch` ⟶ `eslint`/`eslint-plugin-vue`/`vue-eslint-parser`/`editorconfig`) | idem |
| OWASP Dependency-Check (backend) | **sem resultado** — plugin v12.1.8 no perfil `security`, `failBuildOnCVSS=7`, sem `target/dependency-check` e sem `NVD_API_KEY` | `pom.xml:199-217` + ausência de diretório |
| Build do backend | artefatos presentes (`rede-krill-scheduling-api.jar`, `jacoco.exec`) | `backend/target/` |

> Os números acima foram **lidos de artefatos existentes**, não reproduzidos nesta análise. A re-execução (`./mvnw -B clean verify`) foi pulada por decisão do responsável pelo projeto.

---

## 6. Matriz de achados

Severidade: **Crítico → Alto → Médio → Baixo → Informativo**. Status: `TRATADO` / `PARCIAL` / `ABERTO` / `ACEITO (com justificativa)`.

| ID | Severidade | Título | Status | Base da avaliação |
|---|---|---|---|---|
| **SEC-01** | Alto | Estado de rate limiting é local à instância | **PARCIAL** | `LoginAttemptService` usa `ConcurrentHashMap` em memória; o próprio código documenta a necessidade de Redis para múltiplas instâncias. O contador zera a cada restart e não é compartilhado atrás de load balancer, permitindo bypass distribuído da política de lockout. |
| **SEC-03** | Alto | Análise de composição (SCA) do backend nunca foi executada | **ABERTO** | Sem `target/dependency-check` e sem `NVD_API_KEY`. Não é possível afirmar ausência de CVE em Spring Boot 4.1.2, jjwt 0.12.6, springdoc 2.6.0, PostgreSQL driver e transitivos. Risco elevado pelo histórico de upgrades recentes. |
| **SEC-02** | Médio | Gate de cobertura por agregado oculta classes críticas fracas | **ABERTO** | O gate 80% é avaliado em agregado de pacote. Dentro dele: `LoginAttemptService` **57,4%**, `ChangePasswordUseCase` **68,5%**, `AuthSessionService` 79,2%, `AuditService` 80,0%. Precisamente as classes de controle de acesso e lockout. |
| **SEC-04** | Médio | Sem detecção de reuso de refresh token com revogação em cascata | **ABERTO** | Em `rotate()`, o reuso gera `AuthenticationFailedException`, mas **não** revoga as demais sessões do usuário nem alerta a auditoria. Recomenda-se revogar todas as sessões da família e registrar evento, padrão recomendado para tokens de longa duração. |
| **SEC-05** | Médio | Política de senha assimétrica entre criação e troca | **ABERTO** | ADMIN pode criar usuário com senha de 8 caracteres sem composição e sem lista de senhas proibidas, enquanto o próprio usuário é obrigado a usar 12 + letra + número ao trocar. A senha mais fraca é a que entra primeiro no sistema. |
| **SEC-06** | Médio | Validação de runtime não realizada | **ABERTO** | Docker indisponível → compose não validado; CI nunca executada sobre este estado; e2e cobre apenas o fluxo de autenticação (`frontend/e2e/auth.spec.js`). Controles CSRF/429/rotação não foram exercitados ponta a ponta contra um servidor real. |
| **SEC-07** | Baixo | Work factor do BCrypt não parametrizável | **ABERTO** | `new BCryptPasswordEncoder()` usa força padrão 10. Não há exposição por configuração para elevar (recomendado 12+ em sistema interno). |
| **SEC-08** | Baixo | Retenção indefinida de PII no outbox | **ABERTO** | `email_outbox` guarda `recipient` (e-mail do fornecedor) e `last_error` sem política de expurgo — decisão consciente para auditoria operacional, mas sem prazo de retenção definido. |
| **SEC-09** | Baixo | Credencial com aparência de segredo real em testes | **ABERTO** | `Admin@12345` aparece em `LoginUseCaseTest.java:60,66,149`. Não é segredo de produção (a senha default foi removida de todo código/config), mas scanner de segredos vai sinalizar e o README histórico ainda a exibia. |
| **SEC-10** | Baixo | `SameSite` divergente entre perfis | **INFORMATIVO** | dev usa `Lax` e produção `Strict`. Correto, mas exige verificação de que o `XSRF-TOKEN` é entregue antes da primeira mutação em `Strict` — o `ensureCsrf()` cobre esse caminho; não validado em runtime (ver `SEC-06`). |
| **SEC-11** | Baixo | `exposeTokenInResponse` devolve o access token no corpo | **INFORMATIVO** | Ativo somente em `dev` (`application-dev.yml`), proibido pelo validador em produção. Mecanismo legítimo para cliente de linha de comando, documentar para não copiar a configuração. |
| **SEC-12** | Baixo | CORS depende de lista explícita de origens | **INFORMATIVO** | `allowCredentials(true)` com origens de `CORS_ALLOWED_ORIGINS`; o compose define `http://localhost:8081`. Não há wildcard, mas um deploy com origem nova exige atualização consciente. |
| **SEC-13** | Informativo | `DROP TABLE capacity_settings` e remoção da constraint única de `palletization_type.code` em `V5` | **TRATADO** | Migração destrutiva porém justificada em comentário no próprio SQL, com `clean-disabled: true` em produção; exige atenção em rollback (runbook). |
| **SEC-14** | Médio | `EmailOutboxProcessor` sem teste próprio — 0% de cobertura | **ABERTO** | Re-lido do artefato JaCoCo na validação final: `com.redekrill.scheduling.infrastructure.email.EmailOutboxProcessor`, 49 linhas, 0 cobertas. O caminho que dá a garantia de entrega assíncrona (sondagem, backoff exponencial `initial*2^min(attempts,10)`, esgotamento em 5 tentativas, transições `PENDING`/`SENT`/`FAILED`) só existe como código. É a lacuna de teste mais barata desta matriz e a que cobre decisão registrada no ADR-003. |

### 6.1 Mapa com a auditoria anterior (seção C — problemas críticos)

| Achado prévio | Situação atual |
|---|---|
| **C1** Segredos e credenciais padrão versionados | **TRATADO** — busca por `AD4rGbLMuVyPKUTpWwvnj0q3…` não retorna ocorrência em código/config; `JWT_SECRET` sem default e obrigatório no compose; senha de banco sem default; `ProductionConfigurationValidator` bloqueia defaults conhecidos. Resta `SEC-09` em testes. |
| **C2** PDF, vídeo e relatório de segurança ausentes | **TRATADO (parcial)** — este relatório, os ADRs, o runbook e o PDF de entrega (`docs/entrega/`) existem; do vídeo há apenas o roteiro de gravação (`docs/entrega/ROTEIRO_VIDEO.md`), pois a gravação depende de um runtime que esta máquina não tem. |
| **C3** Sem telas de cadastro de faixas horárias e tipos de paletização | **TRATADO** — CRUD em `ReferenceDataController` + `TimeSlotsView`/`PalletizationTypesView`. |
| **C4** Ocupação não integrada ao formulário | **TRATADO** — `loadCapacity()` com seletor de data e painel percentual em `AppointmentsView.vue`. |
| **C5** Spring Boot 3.3.5 fora do suporte e sem análise de dependências | **PARCIAL** — upgrade para 4.1.2 concluído; análise de dependências ainda não executada (`SEC-03`). |
| **C6** `UpdateUserRequest.active` como `boolean` primitivo | **TRATADO** — agora `Boolean` com `@NotNull`. |
| **C7** Lacunas na trilha de auditoria | **TRATADO** — `LOGIN_FAILED`, `LOGIN_BLOCKED`, `PASSWORD_CHANGE`, `DELETE` adicionados. |
| **C8** Concorrência: ID Pedido duplicado retornando 500 / atualizações perdidas | **TRATADO** — `409` + lock otimista por `version`; causa técnica apenas em log. |

---

## 7. Referência rápida de configuração de segurança

| Variável | Default | dev | prod | Efeito |
|---|---|---|---|---|
| `JWT_SECRET` | vazio (app não sobe) | efêmero em memória | obrigatório, ≥256 bits Base64 | assinatura do access token |
| `JWT_ACCESS_EXPIRATION` | `15m` | `30m` | `15m` | vida do access token |
| `JWT_REFRESH_EXPIRATION` | `7d` | `7d` | `7d` | vida e expiração da linha em `auth_session` |
| `AUTH_COOKIE_SECURE` | `false` | `false` | `true` | flag `Secure` |
| `AUTH_COOKIE_SAME_SITE` | `Lax` | `Lax` | `Strict` | escopo de envio do cookie |
| `AUTH_EXPOSE_TOKEN_IN_RESPONSE` | `false` | `true` | `false` | token no corpo (só dev) |
| `TRUST_FORWARDED_HEADERS` | `false` | `false` | `true` | origem do IP no rate limit |
| `LOGIN_MAX_ATTEMPTS_ACCOUNT` / `_IP` | `5` / `20` | idem | idem | limiar de lockout |
| `LOGIN_WINDOW` / `LOGIN_LOCKOUT` | `15m` / `5m` | idem | idem | janela e duração progressiva |
| `SEED_ADMIN_ENABLED` / `..._REQUIRE_PASSWORD_CHANGE` | `false` / `true` em prod | `true` / `false` | `false` / `true` | provisioning do primeiro ADMIN |
| `SPRINGDOC_ENABLED` | `false` | `true` | `false` | documentação da API |

Obrigatoriedade de `DB_PASSWORD`, `JWT_SECRET` e `SEED_ADMIN_PASSWORD` forte em produção está imposta em código, não em convenção (`ProductionConfigurationValidator`).

---

## 8. Recomendações priorizadas

**P0 — antes de qualquer implantação**
1. Executar o OWASP Dependency-Check com chave NVD (`SEC-03`) e triar o relatório; registrar saída em `docs/entrega/`.
2. Validar runtime com Docker/Postgres real e rodar a CI completa sobre o commit de checkpoint (`SEC-06`).
3. Confirmar HTTPS terminando em proxy com `X-Forwarded-For` confiável **e** `TRUST_FORWARDED_HEADERS=true` — caso contrário o rate limit por IP passa a contar o IP do proxy, agregando todos os usuários em um único contador.

**P1 — próxima iteração**
4. Detecção de reuso de refresh com revogação de todas as sessões do usuário + auditoria de evento (`SEC-04`).
5. Unificar política de senha: mínimo 12 + letra/número + lista de senhas proibidas também no cadastro por ADMIN (`SEC-05`).
6. Gate de cobertura por **classe** para as 4 classes de controle de acesso (meta 80% individual, não agregado) e testes de `LoginAttemptService` cobrindo lockout progressivo e poda (`SEC-02`).
7. Testes de integração para `EmailOutboxProcessor`: retentativa com backoff, esgotamento em 5 tentativas, transições `PENDING`/`SENT`/`FAILED` e idempotência do agendador (`SEC-14`).
8. Shared login-attempt store (Redis ou tabela) se houver mais de uma instância (`SEC-01`).

**P2 — higiene**
9. Parametrizar o work factor do BCrypt (`SEC-07`).
10. Definir retenção/expurgo de `email_outbox` e da auditoria (`SEC-08`).
11. Substituir `Admin@12345` em fixtures e rodar um secret-scan (gitleaks/trufflehog) no repositório (`SEC-09`).

---

## 9. Plano de re-teste sugerido

| # | Verificação | Comando / método | Resultado esperado |
|---|---|---|---|
| 1 | Suíte backend + gate de cobertura | `cd backend && ./mvnw -B clean verify` | 125+ testes verdes, agregado crítico ≥ 80% |
| 2 | Suíte frontend | `cd frontend && npm run lint && npm run format:check && npm run typecheck && npm test -- --coverage` | 0 erros de lint; cobertura ≥ thresholds (60/60/50/60) |
| 3 | Fluxos críticos ponta a ponta | `npm run test:e2e` | login, logout, refresh e troca de senha |
| 4 | Composição backend | `./mvnw -B -Psecurity org.owasp:dependency-check-maven:check` | relatório em `target/dependency-check`, sem CVE ≥ CVSS 7 |
| 5 | Composição frontend | `npm audit --omit=dev` | 0 vulnerabilidades (status atual: atendido) |
| 6 | CSRF | `POST /api/appointments` sem `X-XSRF-TOKEN` e com cookie de sessão | `403` |
| 7 | Rate limit | 6 logins inválidos seguidos para a mesma conta | `429` + `Retry-After` + auditoria `LOGIN_BLOCKED` |
| 8 | Reuso de refresh | repetir o mesmo `POST /api/auth/refresh` | `401`; sessão antiga inutilizável |
| 9 | Escalada de privilégio | `USER` acessando `POST /api/appointments` e `/api/audit-logs` | `403` + auditoria `ACCESS_DENIED` |
| 10 | Fail-fast de configuração em produção | subir com perfil `prod` sem `JWT_SECRET` / com `AUTH_COOKIE_SECURE=false` | falha de inicialização por `ProductionConfigurationValidator` |
| 11 | Segredo de sessão | revogar a sessão via logout e reutilizar o access token ainda válido | `401` (claim `sid` deixa de ser ativa) |
| 12 | Validação de stack | qualquer erro não tratado | corpo sem stacktrace, `X-Correlation-ID` presente |

Itens 6 a 12 não foram executados: exigem instância em runtime.

---

## 10. Conclusão

O núcleo de autenticação deixou o modelo anterior (JWT stateless puro, token em `localStorage`, sem CSRF, sem revogação) e passou a um modelo com **sessão revogável, refresh rotativo de uso único, CSRF obrigatório, lockout progressivo, troca forçada de senha inicial e fail-fast de configuração em produção** — todos com evidência em código e cobertos pela suíte de 125 testes em execução local. Os sete problemas críticos da auditoria anterior estão tratados ou em tratamento.

As três lacunas que ainda impedem uma afirmação de segurança completa são de **evidência**, não de arquitetura: análise de dependências do backend nunca executada (`SEC-03`), ausência de validação em runtime (`SEC-06`) e cobertura insuficiente exatamente nas classes de controle de acesso (`SEC-02`). Nenhuma delas exige mudança estrutural; todas são fecháveis antes da entrega.

---

*Documento gerado por análise automatizada de código com verificação por evidência. Todos os números de linha e artefatos citados existiam no repositório na data da análise. Itens sem evidência foram marcados como não verificados em vez de inferidos.*
