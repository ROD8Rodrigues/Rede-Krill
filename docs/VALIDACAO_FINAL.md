# Validação final — Rede Krill Scheduling

Relatório de fechamento da entrega. Cada linha declara **como** o item foi conhecido, porque
"está pronto" e "foi verificado" são afirmações diferentes.

- Data: **2026-11-02**
- Ramo: `main` · último commit publicado: `06cee93` (2026-11-02)
- Estado da árvore de trabalho neste momento: **156 entradas não commitadas** (83 modificadas, 72 não rastreadas, 1 renomeada)
- `.github/` está **fora do versionamento** → nenhum workflow validou este estado
- Decisão registrada: a "Fase 0" (re-executar as suítes de teste antes de documentar) foi **pulada por orientação do responsável**. Os números de teste e cobertura vêm dos artefatos já gerados, não de execução desta sessão.

---

## 1. Escala de classificação

| Código | Significado | Como provar |
|---|---|---|
| **V** | **Validado nesta sessão** — comando executado aqui, saída conferida | comando e resultado registrados na seção 3 |
| **A** | **Validado por artefato** — executado antes (02/11 22:19–22:22), lido do arquivo de saída | caminho do artefato + timestamp |
| **C** | **Conferido por leitura de código** — implementação existe e está correta no fonte, mas não exercitada em runtime | `arquivo:linha` |
| **N** | **Não validado** — depende de algo indisponível nesta máquina (Docker, chave NVD, ambiente de CI) | bloqueio declarado na seção 5 |
| **L** | **Lacuna conhecida** — validado que *falta*; tem ID em `SECURITY_REPORT.md` | ID do achado |

Um item pode ser **C** e **L** ao mesmo tempo: o controle existe no código e, ainda assim, tem um defeito conhecido.

---

## 2. Resultado consolidado

| # | Item da entrega original | Classe | Valor / evidência |
|---|---|---|---|
| 1 | README reescrito | **V** | `README.md`, 721 linhas, com tabela de variáveis, API, migrações e limitações |
| 2 | `SECURITY_REPORT.md` | **V** | achados `SEC-01`…`SEC-31` (`SEC-16`…`SEC-31` em §6.1), plano de re-teste de 12 itens com situação por item, seção 11 com a correção de dependência |
| 3 | ADRs em `docs/arquitetura/` | **V** | índice + 6 ADRs (001 sessão, 002 CSRF, 003 outbox, 004 capacidade, 005 Flyway, 006 concorrência) |
| 4 | Runbook | **V** | `docs/operacao/RUNBOOK.md`, 306 linhas, com SQL de operação real |
| 5 | `docs/entrega/` com PDF + roteiro | **V** | `ENTREGA.html` → `Rede-Krill-Scheduling-Entrega.pdf` (6 páginas, Chrome headless) + `ROTEIRO_VIDEO.md` (11 cenas) |
| 6 | OWASP Dependency-Check no backend | **V** | plugin 12.2.2 com `failBuildOnCVSS=7`: 86 componentes, 0 vulneráveis; o achado (DOMPurify 3.4.16 no webjar do Swagger UI) foi corrigido e re-varrido, inclusive em **controle negativo** — ver §3.2 e §3.3 e `SECURITY_REPORT.md` §11. Ressalva: rodou com o cache local do NVD (sem `NVD_API_KEY` nesta máquina) e a goal **exige rede** (recusa `-o`) |
| 7 | `npm audit` | **V** | produção: **0 vulnerabilidades**; árvore completa: 12 moderadas (só dev) |
| 8 | Validação final classificada | **V** | este documento |

| Área do produto | Classe |
|---|---|
| Build e testes backend | **V** (re-executados em 2026-11-03 à tarde: **202** testes em 34 classes, 0 falhas/erros/ignorados — §3.3) |
| Cobertura e gate de qualidade | **V** (gate 516/522 = 98,85% linhas; projeto 87,02% — §4) |
| Portas de qualidade do frontend (lint, tipos, formatação, build, `vitest`) | **V** |
| Controles de segurança **em runtime** (CSRF, `429`, rotação, reuso com cascata, RBAC, outbox, política de senha) | **V** (§3.3 — 68/68 verificações contra instância real) |
| E2E de autenticação (Playwright) | **A** (artefato de execução anterior) + **N** (não re-executado nesta sessão) |
| Migrações V1–V7 | **V** em runtime (Flyway aplicou V1–V7 num schema limpo — §3.3) + **A** em Postgres embarcado nos testes |
| `docker compose up` / build de imagem / `nginx -t` | **N** — Docker e nginx ausentes desta máquina |
| Pipeline de CI | **N** — workflow versionado, nunca executado nesta máquina |
| Escala horizontal | **N** → **L** (`SEC-01`, restrição estrutural ainda em aberto) |

---

## 3. O que foi executado nesta sessão

Saídas reais, reproduzíveis com os comandos da seção 7.

| Comando | Resultado | Classe |
|---|---|---|
| `npm audit --omit=dev` (em `frontend/`) | `found 0 vulnerabilities` | **V** |
| `npm audit` (em `frontend/`) | `12 moderate severity vulnerabilities` — árvore de desenvolvimento (`eslint-config-prettier`, `vue-eslint-parser`, `editorconfig`/`minimatch`); sem efeito no bundle entregue | **V** |
| `npm run lint` → `eslint .` | código de saída **0**, nenhuma saída de erro | **V** |
| `npm run typecheck` → `vue-tsc --noEmit` | código de saída **0** | **V** |
| `npm run format:check` → `prettier --check .` | `All matched files use Prettier code style!` (saída 0) | **V** |
| `npm run build` → `vite build` | `✓ built in 4.08s` (saída 0); maior chunk `dist/assets/column-*.js` 374,48 kB · gzip 79,56 kB | **V** |
| leitura de `backend/target/surefire-reports/*.xml` | **125** testes, **25** classes, 0 falhas, 0 erros, 0 ignorados | **A** |
| leitura de `backend/target/site/jacoco/jacoco.csv` | ver seção 4 | **A** |
| leitura de `frontend/test-results/.last-run.json` | `{"status":"passed","failedTests":[]}` | **A** |
| `git status --porcelain` | 156 entradas; `ApiError.java` renomeado para `application/dto/error/` | **V** |

**Não executado na Fase 0 desta sessão** (decisão de pular a re-execução inicial): `mvn verify`, `mvn test`, `vitest run`, `playwright test`, `docker compose up`. `mvn clean verify`, `vitest run` e o Dependency-Check foram executados **depois**, nas etapas §3.1 e §3.2, que substituem esta ressalva para esses comandos. `playwright test` e `docker compose up` continuam não executados. Nada destas linhas é apresentado como resultado obtido agora.

### 3.1 Executado depois, ao habilitar o acesso ADMIN local

| Ação | Resultado | Classe |
|---|---|---|
| `.\start-dev.ps1` (carrega `.env`, mapeia `POSTGRES_*` → `DB_*`, roda `mvnw spring-boot:run`) | backend sobe: `Tomcat started on port 8080`, log do seed `Usuário administrador inicial já existe: admin@redekrill.com` | **V** |
| `npx vitest run` (em `frontend/`) | **20** testes em **3** arquivos, 0 falhas | **V** |
| `npm run lint` / `format:check` / `typecheck` / `build` após a correção | todos com saída 0; `✓ built in 1.52s` | **V** |
| Login pela UI em `http://localhost:5173/login` | `POST /api/auth/login` **200** → `Painel` renderizado como `Administrador` / perfil ADMIN; `GET /api/appointments/capacity?date=…` **200** | **V** |
| `POST /api/auth/refresh` via `api` do app, já autenticado | **200** — confirma que o token CSRF do pós-login é aceito | **V** |

Defeito encontrado e corrigido durante esta etapa (estava mascarado porque o Playwright mocka `/api/auth/*`):

- `frontend/src/services/api.js` — `ensureCsrf()` preferia o cookie `XSRF-TOKEN`, que traz o token **bruto**; o Spring Security (proteção BREACH) só aceita o valor **mascarado** devolvido no corpo de `GET /api/auth/csrf`. Além disso o axios, com `withXSRFToken` no padrão, sobrescrevia o cabeçalho pelo valor do cookie **depois** dos interceptors. Resultado: todo `POST /api/auth/login` pela UI devolvia `403 ACCESS_DENIED`. Correção: `withXSRFToken: false` nos dois clientes e token novo a cada requisição de escrita (login/logout geram token bruto novo, cache fica inválido).
- Configuração local — `CORS_ALLOWED_ORIGINS` só listava 5173/8081; caindo o Vite para 5174/5175 o backend respondia `403 Invalid CORS request`. Acrescentadas essas portas ao `.env`/`.env.example`.
- Armadilha registrada — `LoginAttemptService` bloqueou o IP durante o diagnóstico (`429` com `Retry-After`); o estado é em memória, e reiniciar o backend o zera (`RUNBOOK.md` §4.2).

Nada do §3.1 altera os números de cobertura ou de testes do backend: a suíte Java não foi reexecutada nesta sessão.

### 3.2 Executado depois, na correção das vulnerabilidades do Swagger UI (2026-11-03)

Escopo: `org.webjars:swagger-ui` fixado em 5.34.1 (DOMPurify 3.4.16 → 3.4.17) mais o alinhamento de `springdoc.swagger-ui.version`. Arquivos tocados: `backend/pom.xml` e `backend/src/main/resources/application.yml`. Narrativa completa em `SECURITY_REPORT.md`, seção 11.

| Ação | Resultado | Classe |
|---|---|---|
| `./mvnw -B dependency:tree -Dincludes=org.webjars:swagger-ui` | `swagger-ui:jar:5.34.1:compile` sob `springdoc-openapi-starter-webmvc-ui:3.2.0` | **V** |
| `./mvnw -B clean verify` | **BUILD SUCCESS** — 125 testes (0 falhas/erros/ignorados), `All coverage checks have been met`, artefato reempacotado | **V** |
| inspeção de `target/classes/application.yml` | `version: 5.34.1` (token `@swagger-ui.version@` substituído pela filtragem do parent) | **V** |
| inspeção dos 7 `.js` dentro de `BOOT-INF/lib/swagger-ui-5.34.1.jar` | DOMPurify presente só em `swagger-ui-bundle.js` e `swagger-ui-es-bundle.js`, ambos marcando **3.4.17**; nenhuma ocorrência de 3.4.16 | **V** |
| `./mvnw -B -Psecurity org.owasp:dependency-check-maven:12.2.2:check` (estado corrigido) | **BUILD SUCCESS** — 86 componentes, **0 vulneráveis**, Relatórios HTML e JSON gerados | **V** |
| mesma execução em cópia de **controle** sem o pin (webjar 5.34.0) | **BUILD FAILURE** — 2 componentes vulneráveis (`pkg:javascript/DOMPurify@3.4.16`), severidades critical/high/low, limiar `>= 7,0` | **V** |
| jar empacotado em `:8085` — `/v3/api-docs`, `/swagger-ui/index.html`, bundles, `swagger-ui.css`, `swagger-initializer.js` | todos **200**; conteúdo servido dos bundles marca 3.4.17 | **V** |
| navegador em `http://127.0.0.1:8085/swagger-ui/index.html` | título "Swagger UI", 7 grupos, 29 operações, bundle carregado, **zero mensagens de console** | **V** |
| `./mvnw -B -o spring-boot:run` em `:8086` (modo dev local) | `/swagger-ui/index.html` **200**, bundle **200** com 3.4.17 | **V** |
| `.\start-dev.ps1` no `:8080` do projetista + `POST /api/auth/login` do ADMIN | UI **200** com 3.4.17 e login **200** (perfil `ADMIN`) | **V** |
| suíte de frontend | **não reexecutada** — nenhum arquivo de `frontend/` mudou neste passo | **N** |
| `docker compose up` / build das imagens | **não executado** — Docker indisponível nesta máquina | **N** |

O teste de controle é a parte que sustenta a afirmação "não escondemos o alerta": o mesmo plugin, a mesma base de dados e o mesmo limiar de CVSS 7 falharam o build com o webjar antigo e aprovaram com o novo.

Executado em 2026-11-03: `clean verify` às 10:22:45 (carimbo de `backend/target/surefire-reports/`) e Dependency-Check às 10:28:18 (carimbo de `backend/target/dependency-check/`).

---

### 3.3 Executado depois, na auditoria e remediação completas (2026-11-03, tarde)

Escopo: correção dos achados da auditoria (`SEC-02`/`04`/`05`/`07`/`14` + os novos `SEC-16`…`SEC-29` em `SECURITY_REPORT.md` §6.1) e **execução do checklist de runtime da seção 6** contra uma instância real.

| Comando / método | Resultado obtido | Classe |
|---|---|---|
| `./mvnw -B -o clean verify` | **BUILD SUCCESS** — `Tests run: 202, Failures: 0, Errors: 0, Skipped: 0`; `All coverage checks have been met`; `SchedulingApiIntegrationTest` 10 testes em 25,4 s (Postgres embutido zonky); `ArchitectureTest` 4/4 | **V** |
| `./mvnw -B -o clean verify` (reexecução final, 14:06, após `JwtTokenServiceTest`) | **BUILD SUCCESS** em 57,0 s — `Tests run: 214, Failures: 0, Errors: 0, Skipped: 0` em **35** classes; gate atendido; artefato reempacotado | **V** |
| leitura de `backend/target/site/jacoco/jacoco.csv` desta execução | gate **516/522 = 98,85%** linhas; projeto **2032/2320 = 87,59%**; ver seção 4 | **V** |
| Instância de smoke: jar de produção em `:8090`, perfil `dev`, `DB_URL` com `currentSchema=smoke`, Flyway sobre schema limpo (V1–V7 aplicados), SMTP sink em `:1025` | `Started SchedulingApplication in 11,9 s`, `health=200`, seed `smoke.admin@redekrill.test` criado | **V** |
| **Checklist de runtime (seção 6) — 68 verificações** | **68/68 OK.** CSRF: mutação sem header → `403 ACCESS_DENIED`; token mascarado ≠ cookie bruto. Cookies: access `HttpOnly Path=/ SameSite=Lax`, refresh `HttpOnly Path=/api/auth`. Sessão: refresh `200` + token rotacionado; replay do token antigo → `401 Sessão expirada ou revogada`; logout `204` → `/api/auth/me` `401`. **Reuso com cascata (`SEC-04`):** replay do token consumido → `401`, `auth_session` da vítima `3 revogadas / 3`, **0 ativas**, e a sessão legítima (token novo) também cai; `REFRESH_TOKEN_REUSE` presente em `/api/audit-logs` (3 ocorrências). Lockout: `[401×5, 429]` com `Retry-After: 299`, senha correta durante o bloqueio também `429`, `LOGIN_BLOCKED` auditado. `PASSWORD_CHANGE_REQUIRED`: `403` em rota de negócio, `200` em `/api/auth/me`. RBAC: `USER` → `403` em `/api/users`, `/api/audit-logs`, `/actuator/prometheus`; ADMIN → `200` e `406` (não `500`) com `Accept: application/json`. Troca de senha: derruba a sessão que trocou **e** a paralela; senha antiga `401`, nova `200`. Política de senha (`SEC-05`): criar com 8 caracteres → `400 {'password': 'Senha deve ter entre 12 e 72 caracteres.'}`; sem números → `400 A senha deve conter letras e números.` Outbox: `SENT/1` para ativo, `CANCELED/0` para cancelado, 0 pendentes do cancelado. Regressão DOMPurify: `/v3/api-docs` `200`, `/swagger-ui/index.html` `200`, bundle servido marcando **3.4.17**. Auditoria sem senha em payload: 0 ocorrências das 5 senhas usadas | **V** |
| `./mvnw -B -Psecurity org.owasp:dependency-check-maven:12.2.2:check` (novo, pós-remediação) | **BUILD SUCCESS** — 86 componentes, **0 vulneráveis**, `Analysis Complete (14 seconds)`, relatórios regenerados às 13:49. Com `-o` a goal **recusa executar** (`Goal requires online mode…`): precisa de rede. Repetido às 14:21, porque o `clean verify` dos 214 testes apagou `target/` e os artefatos voltaram a existir (HTML 921.667 B, JSON 1.031.685 B — mesmo tamanho) | **V** |
| `npm run lint` · `prettier --check .` · `vue-tsc --noEmit` · `npx vitest run` | todos com saída 0; **20** testes `vitest` verdes (frontend ajustado para `MIN_PASSWORD_LENGTH = 12`, `minlength`, hint de composição) | **V** |
| `npm run test:e2e` (Playwright) | **não executado nesta sessão** — o arquivo de resultado lido (§3) é de execução anterior | **N** |
| `docker compose up` · build de imagem · `nginx -t` | **não executados** — Docker e nginx ausentes desta máquina; `frontend/nginx.conf` alterado (`SEC-23`) é revisão de configuração sem validação de runtime | **N** |

**Revalidação depois da última mudança de código (14:07–14:12).** `JwtTokenServiceTest` (12 casos) expôs dois defeitos reais de produto (`SEC-30`, `SEC-31` em `SECURITY_REPORT.md` §6.1) e as correções mexem no caminho de autenticação de **todo** pedido, então o checklist de runtime foi repetido contra o jar reconstruído:

| Ação | Resultado | Classe |
|---|---|---|
| `ResetSmoke` (recria o schema isolado `smoke` do zero) + `java -jar target/rede-krill-scheduling-api.jar` em `:8090` | `Started SchedulingApplication in 12,1 s`, Flyway V1–V7 aplicados, seed `Usuário administrador inicial criado: smoke.admin@redekrill.test` | **V** |
| `python smoke_runtime.py` novamente | **68/68** — inclusive `reuso do refresh ANTIGO -> 401 por revogacao` com a mensagem `Sessão expirada ou revogada`, cascata `3 revogadas / 3` e `0 ativas`, `REFRESH_TOKEN_REUSE` 3× em `/api/audit-logs`, lockout `[401×5, 429]` com `Retry-After: 300` e `PASSWORD_CHANGE_REQUIRED` | **V** |
| transcrição completa | `C:/Users/Chris/AppData/Local/Temp/kq/evidence-post/smoke-runtime-jar-214.txt` (UTF-8, 12 seções) | **V** |

Armadilha encontrado no caminho, e registrada porque custa caro em evidência: matar o *wrapper* bash da tarefa em segundo plano **não matou a JVM** (`exec java -jar` foi filho direto do shell encerrado). A instância antiga ficou segurando a `:8090`; a nova falhou no boot (`Port 8090 was already in use`) e a execução seguinte do harness pediu login contra um processo cujo schema tinha sido recriado debaixo dele — `401` em tudo, o que **pareceria** regressão de autenticação. Diagnóstico por `netstat -ano` + `Get-CimInstance Win32_Process` sobre o PID, depois reinício limpo. Lição: antes de atribuir um `401` ao código, confirme que quem respondeu é o processo que você acha que é.

**Correção de evidência (`SEC-29`) — leia antes de confiar em qualquer "reuso de refresh validado".** O item de replay do *harness* de smoke era **vacuoso** na versão anterior: o cookie de refresh era injetado com `http.cookiejar.Cookie(version=0, domain="localhost", …)`, e o `DefaultCookiePolicy` do Python não devolve cookie Netscape cujo domínio não contém ponto. A requisição de "ataque" saía **sem** o cookie, e o `401 vinha de `Sessão não informada`, não da detecção de reuso. O helper foi substituído por `replay_refresh`, que monta o header `Cookie` à mão, e a asserção passou a exigir a mensagem de **revogação**. Também por isso a cascata de `SEC-04` só pôde ser medida agora. Lição operacional: uma asserção por status HTTP não é evidência — é preciso assentar a *causa* do status.

Três correções de produto nasceram dessas execuções e não de leitura de código: (a) `AuthSessionService` distingue reuso de expiração natural e revoga a família com auditoria, lançando a exceção **fora** da transação para o rollback não apagar a revogação; (b) `application-dev.yml`/`application-prod.yml` gravavam `cookie-secure` e `expose-token-in-response` como literais, o que **anulava** `AUTH_COOKIE_SECURE` e `AUTH_EXPOSE_TOKEN_IN_RESPONSE` documentados no `.env.example` — viraram placeholders com default por perfil, provado no runtime (`token=False` no corpo do login com a env em `false`); (c) `JwtTokenService.parse` verificava `exp` com o relógio interno do JJWT em vez do `Clock` injetado, deixando o ramo de rejeição de token vencido sem nenhum teste (`SEC-30`), e um `JWT_SECRET` em Base64 malformado derrubava o boot com `DecodingException` em vez da mensagem acionável (`SEC-31`).

---

## 4. Cobertura medida (artefato JaCoCo)

Números lidos de `backend/target/site/jacoco/jacoco.csv` **regenerado pela execução de 2026-11-03 14:06** (§3.3 — 214 testes). Os valores da tabela anterior (125 testes, 91,25% / 80,05%) estavam superados; mantê-los seria reportar um artefato que não existe mais.

Contra o gate real do build — quatro pacotes, contador **LINHA**, mínimo **0,80** (`backend/pom.xml`):

| Escopo | Linhas | Instruções | Contra o gate |
|---|---|---|---|
| 4 pacotes críticos (gate) | **516/522 = 98,85%** | 2445/2471 = **98,95%** | **passa** |
| `domain.policy` | 42/42 = 100% | 322/324 = 99,38% | — |
| `application.usecase.appointment` | 221/221 = 100% | 994/994 = 100% | — |
| `application.usecase.auth` | 79/79 = 100% | 343/345 = 99,42% | — |
| `application.service` | 174/180 = 96,67% | 786/808 = 97,28% | acima de 80 isoladamente |
| projeto inteiro | 2032/2320 = **87,59%** linhas · 9757/11238 = 86,82% instruções · 420/588 = 71,43% ramos | | |

Comparação com a matriz de achados (`SECURITY_REPORT.md` §6): as classes que estavam abaixo do aceitável em `SEC-02` subiram, e o 0% de `SEC-14` deixou de existir.

| Classe | Linhas (antes → agora) | Instruções (agora) |
|---|---|---|
| `LoginAttemptService` | 69,4% → **93,0%** (66/71) | 93,6% |
| `ChangePasswordUseCase` | 75,6% → **100%** (31/31) | 100% |
| `AuthSessionService` | — → **98,8%** (80/81) | 99,7% |
| `EmailOutboxProcessor` (`infrastructure.email`) | **0%** (49 linhas) → **98,5%** (66/67) | 99,7% |
| `GlobalExceptionHandler` | — → **96,5%** (55/57) | 97,1% |
| `JwtTokenService` (`infrastructure.security`) | 72,7% → **100%** (45/45) | 100% |

`JwtTokenService` estava em 72,7% porque **nenhum teste exercitava a rejeição de token**: o ramo que descarta `exp` vencido ou assinatura inválida (`JwtTokenService.java:84-85`) tinha 0 casos. Isso não era só um número de cobertura — era o controle de "access token de 15 minutos" sem verificação automatizada, porque o parser comparava `exp` com o relógio do sistema e não com o `Clock` injetado (achado `SEC-30`). Com `JwtTokenServiceTest` (12 casos) a classe fechou em 100% e a expiração passou a ser testável em tempo determinístico.

O gate do build é medido por **pacote** (BUNDLE sobre os quatro listados), não por classe — por isso uma classe fora dos pacotes críticos só aparece aqui, não no `jacoco:check`. Registrado como limitação do check configurado (`SEC-02`), não como cobertura concluída por decreto.

**Precisão de número:** o gate é medido em **linhas** (98,85%); instrução é 98,95%. Nunca citar um como se fosse o outro, e sempre nomear o contador.

---

## 5. Bloqueios externos — nada aqui é falha de implementação

| Item | Por que não foi validado | O que destrava | Data/hora em que passa a **V** |
|---|---|---|---|
| Docker Compose (stack completo) | Docker indisponível nesta máquina | rodar `docker compose up` em máquina com Docker; `docker compose ps` com os 4 serviços `healthy` | +30 min |
| Login real pelo compose em `http://localhost` | o compose default é perfil `prod`, com cookies `Secure` → o navegador descarta o cookie. Em 2026-11-03 isso deixou de estar travado no YAML: `AUTH_COOKIE_SECURE` agora é placeholder com default por perfil (§3.3) | usar HTTPS **ou** `AUTH_COOKIE_SECURE=false` em ambiente local | junto com o item acima |
| GitHub Actions | `.github/workflows/ci.yml` está versionado, mas as alterações desta missão (78 entradas em `git status --porcelain`) ainda não têm commit; `gh` não está disponível nesta máquina para consultar os runs | commit de checkpoint + push pelo responsável; o workflow `ci.yml` executa no push/PR | após o push |
| OWASP Dependency-Check | **rodou** em 2026-11-03 duas vezes (§3.2 e §3.3), mas sem `NVD_API_KEY` nesta máquina: a análise de CVE usou o cache local da base do NVD, cuja idade depende do último update da máquina. O achado real (RetireJS/DOMPurify) não depende do NVD. Novo detalhe operacional: a goal **recusa o modo `-o`** | job agendado do CI com `NVD_API_KEY` para base fresca a cada semana | já **V** para o estado atual; pleno após o job agendado |
| `docker build` das imagens (`backend/Dockerfile`, `frontend/Dockerfile`) | mesmo bloqueio da linha acima: binário `docker` ausente. O caminho validado foi `java -jar` sobre o artefato empacotado, idêntico ao copiado na imagem | `docker compose up` + `docker compose ps` com os 4 serviços `healthy` | +30 min |
| `frontend/nginx.conf` (`SEC-23`, `X-Forwarded-For`) | binário `nginx` ausente; a mudança é revisão de configuração, **não** validação de runtime | `nginx -t` no container da imagem frontend e uma requisição atravessando o proxy | junto com o item acima |
| Playwright E2E | não re-executado nesta sessão (o `.last-run.json` de §3 é de execução anterior; além disso a suíte mocka `/api/auth/*`, o que mascarou o defeito de CSRF do §3.1) | `npm run test:e2e` com o backend real de pé, sem mocks | +30 min |
| Escala horizontal (≥2 réplicas) | restrição de projeto (rate limit em memória, outbox sem *claim*) | decisão de arquitetura nova + store compartilhado | nova iteração |

A linha "CSRF / `429` / rotação / outbox em runtime" que existia aqui foi **removida por execução**: o checklist da seção 6 rodou inteiro em 2026-11-03 à tarde contra uma instância real (§3.3). O que ainda não é **V** nesta tabela é infra (Docker, nginx, CI) e E2E — nada disso é controle de segurança do domínio.

---

## 6. Checklist de fumaça — **executado** em 2026-11-03 à tarde (§3.3)

Estado por item, com o resultado real. As 12 linhas abaixo são o checklist original; a execução o expandiu para **68 verificações** (os detalhes estão na tabela de §3.3).

```text
[x] 1  GET  /actuator/health                            → 200 {"status":"UP"} (health mail=UP)
[x] 2  GET  /api/auth/csrf                              → headerName=X-XSRF-TOKEN + token mascarado (≠ cookie bruto)
[x] 3  POST /api/auth/login (senha do seed)             → 200 + 2 cookies HttpOnly; com a env em false, sem token no corpo
[x] 4  POST /api/appointments sem X-XSRF-TOKEN          → 403 code=ACCESS_DENIED
[x] 5  POST /api/appointments com X-XSRF-TOKEN          → 201, emailQueued=true, emailSent=false
[x] 6  5× POST /api/auth/login com senha errada         → [401,401,401,401,401] e 6ª → 429 com Retry-After: 299
[~] 7  expirar o access (15′) e chamar rota protegida    → a rotação em si passou (refresh 200 + token novo, cookie
                                                            antigo reusado → 401 por revogacao); a expiração natural
                                                            de 15 minutos **não foi esperada em relógio real** aqui.
                                                            Ela agora é provada por teste determinístico
                                                            (`JwtTokenServiceTest.rejectsTheTokenOnceTheExpirationHasPassed`
                                                            — aceita em exp−1 s, rejeita em exp+1 s), que só existe
                                                            porque este item revelou a `SEC-30`
[x] 8  reutilizar o refresh antigo                      → 401 "Sessão expirada ou revogada" + cascata: auth_session da
                                                            vítima 3 revogadas / 0 ativas, sessão legítima também cai,
                                                            REFRESH_TOKEN_REUSE auditado (3 ocorrências)
[x] 9  SELECT status, attempts FROM email_outbox        → SENT/1 para o agendamento ativo, com mensagem recebida pelo
                                                            SMTP sink local (MailHog do compose não foi usado — Docker ausente)
[x] 10 criar dois agendamentos para o mesmo orderId     → 201 + 409 "Já existe um agendamento com o ID…"
[x] 11 trocar a senha obrigatória                       → 204; sessão que trocou **e** a paralela revogadas; senha antiga 401, nova 200
[x] 12 GET /actuator/prometheus como USER               → 403; como ADMIN → 200 (e 406, não 500, com Accept: application/json)
```

O item 7 é o único com ressalva, e ela é explícita: nada aqui afirma que a janela de 15 minutos foi observada em relógio real. O item 8 já foi **vacuoso** numa execução anterior — o cookie injetado pelo *harness* não saía no pedido e o `401` vinha de "sessão não informada"; a causa estava em `http.cookiejar` não devolver cookies Netscape de domínio sem ponto (`localhost`). Só depois de montar o header `Cookie` à mão e exigir a mensagem de **revogação** é que a detecção de reuso e a cascata de `SEC-04` ficaram medidas de verdade. Isso está registrado como achado `SEC-29` porque afeta a confiança em qualquer evidência anterior deste item.

Com o checklist executado, a superfície de segurança do domínio sai de **C** (lido em código) para **V** (medido em runtime) — exceto o item 7, que fica **P** (parcial) pela razão acima.

---

## 7. Como reproduzir qualquer número deste relatório

```bash
# testes e cobertura (re-executa; ~1 min). Offline por padrão; use o wrapper.
cd backend && ./mvnw -B -o clean verify
python - <<'PY'   # total de testes a partir dos XML
import xml.etree.ElementTree as ET, glob
t=f=e=s=0
for p in glob.glob('target/surefire-reports/*.xml'):
    r=ET.parse(p).getroot()
    t+=int(r.get('tests',0)); f+=int(r.get('failures',0)); e+=int(r.get('errors',0)); s+=int(r.get('skipped',0))
print('tests',t,'failures',f,'errors',e,'skipped',s)
PY

# cobertura dos pacotes do gate (linhas) a partir do CSV do JaCoCo
python - <<'PY'
import csv
rows=list(csv.DictReader(open('target/site/jacoco/jacoco.csv',encoding='utf-8')))
pre=('com.redekrill.scheduling.domain.policy','com.redekrill.scheduling.application.usecase.appointment',
     'com.redekrill.scheduling.application.usecase.auth','com.redekrill.scheduling.application.service')
sel=[r for r in rows if any(r['PACKAGE']==p or r['PACKAGE'].startswith(p+'.') for p in pre)]
mi=sum(int(r['LINE_MISSED']) for r in sel); c=sum(int(r['LINE_COVERED']) for r in sel)
print('gate LINE: %d/%d = %.2f%%'%(c,c+mi,100*c/(c+mi)))
for n in ('LoginAttemptService','ChangePasswordUseCase','JwtTokenService','EmailOutboxProcessor'):
    for r in sel+rows:
        if r['CLASS']==n:
            lm,lc=int(r['LINE_MISSED']),int(r['LINE_COVERED'])
            im,ic=int(r['INSTRUCTION_MISSED']),int(r['INSTRUCTION_COVERED'])
            print('%-22s line %.1f%% instr %.1f%%'%(n,100*lc/(lc+lm),100*ic/(ic+im)))
PY

# frontend
cd ../frontend && npm audit --omit=dev && npm run lint && npm run typecheck && npm run format:check && npm run build

# análise de composição do backend (§3.2) — exige rede ou cache do NVD já populado
cd ../backend
./mvnw -B -Psecurity org.owasp:dependency-check-maven:12.2.2:check
python - <<'PY'   # contagem de componentes vulneráveis e identidade do achado
import json
d=json.load(open('target/dependency-check/dependency-check-report.json',encoding='utf-8',errors='replace'))
v=[c for c in d['dependencies'] if c.get('vulnerabilities')]
print('componentes',len(d['dependencies']),'| vulneráveis',len(v))
for c in v:
    print(c['packages'][0]['id'],[(x['source'],x['severity'],x['name']) for x in c['vulnerabilities']])
PY

# controle negativo: a mesma varredura numa cópia do backend SEM o pin do webjar
# (copiar backend/ para fora do repositório, remover o bloco <dependencyManagement> do pom)
# esperado: BUILD FAILURE com pkg:javascript/DOMPurify@3.4.16 em critical/high/low

# verificação do que realmente sai empacotado
./mvnw -B dependency:tree -Dincludes=org.webjars:swagger-ui
unzip -l target/rede-krill-scheduling-api.jar | grep swagger-ui
grep -n "version:" target/classes/application.yml | tail -1

# PDF de entrega (HTML é a fonte editável)
cd .. && "/c/Program Files/Google/Chrome/Application/chrome.exe" --headless=new --disable-gpu \
  --no-pdf-header-footer \
  --print-to-pdf="docs/entrega/Rede-Krill-Scheduling-Entrega.pdf" \
  "file:///$(pwd)/docs/entrega/ENTREGA.html"
```

**Sobre reproduzir o checklist de runtime (§6).** Os 68 casos rodaram por um *harness* de sessão (`smoke_runtime.py`, `smoke-backend.sh`, `ResetSmoke.java`), mantido **fora do repositório** e sem transcrição dele para cá: o script carrega as credenciais da instância de fumaça (senha do seed, credenciais do Postgres lidas do `.env`), e commitar isso seria exatamente o que `SEC-09` (credencial com aparência de segredo em arquivo versionado) existe para evitar. Para reproduzir, o caminho é o da tabela acima — instância isolada (`SERVER_PORT=8090`, `DB_URL` com `currentSchema=smoke`, `SPRING_FLYWAY_SCHEMAS=smoke`, SMTP de escuta em `:1025`) e as 12 linhas do checklist exercidas manualmente ou por script seu. A transcrição desta última execução está em `C:/Users/Chris/AppData/Local/Temp/kq/evidence-post/smoke-runtime-jar-214.txt` (caminho local do validador, não um artefato versionado).

---

## 8. Veredito

**Pronto, com afirmativas de evidência que ainda não podem ser feitas — e elas são de infraestrutura, não de domínio.** O sistema tem arquitetura limpa e verificada por
ArquUnit, sessão revogável com rotação de refresh e **cascata no reuso**, CSRF, lockout de login, troca obrigatória de senha inicial,
política de senha única nos cinco pontos de entrada, cadastros operacionais, capacidade com concorrência tratada, auditoria, relatórios paginados e notificação por
outbox transacional — com **214 testes** passando (executados 2026-11-03 14:06) e gate de cobertura crítico superado em
linhas (**98,85%**; projeto 87,59%), portas de qualidade do frontend verdes nesta sessão, zero vulnerabilidades de produção nas
dependências JavaScript e **0 componentes vulneráveis** no Dependency-Check do backend após a correção do DOMPurify
do Swagger UI (§3.2). Ao contrário da versão anterior deste documento, os controles de segurança **foram exercitados em
runtime**: 68/68 verificações contra o jar empacotado, repetidas depois da última mudança de código (§3.3).

As afirmativas que ainda não podem ser feitas:

1. ~~**Não se pode dizer que as dependências do backend estão livres de CVE** (`SEC-03`) — a análise nunca rodou.~~ **Fechada em 2026-11-03:** o Dependency-Check 12.2.2 rodou com `failBuildOnCVSS=7` (86 componentes, 0 vulneráveis), encontrou o DOMPurify 3.4.16 embutido no webjar do Swagger UI e o achado foi corrigido com controle negativo (§3.2 e `SECURITY_REPORT.md` §11). Ressalva honesta: a varredura usou o cache local do NVD — sem `NVD_API_KEY` nesta máquina a frescura da base depende do cache; no CI agendado o segredo existe. A goal também **recusa `-o`**: precisa de rede.
2. ~~**Nenhum controle de segurança foi exercitado em runtime** (`SEC-06`).~~ **Fechada em 2026-11-03:** CSRF mascarado, cookies, rotação, **reuso com cascata e auditoria**, lockout `429`, `PASSWORD_CHANGE_REQUIRED`, RBAC, `406`/`400`, outbox `PENDING → SENT` com SMTP real, supressão de e-mail de cancelado, política de senha na criação e bundle DOMPurify 3.4.17 — **68/68** (§3.3 e §6).
3. **A CI não viu este estado específico** — `main` está sincronizada com `origin/main`, mas as alterações da missão permanecem **não commitadas** (78 entradas em `git status --porcelain`), por decisão do responsável; `gh` não está disponível nesta máquina.
4. **A stack de contêineres não foi levantada** — sem Docker nesta máquina: `docker compose up`, o build das duas imagens e `nginx -t` continuam não validados. O `X-Forwarded-For` adicionado em `frontend/nginx.conf` (`SEC-23`) é revisão de configuração, não medição.
5. **O E2E de autenticação não foi re-executado** — e a suíte atual mocka `/api/auth/*`, razão pela qual ela não pegou o defeito real de CSRF do §3.1. Reescrevê-la contra o backend real é trabalho aberto.
6. **Escala horizontal continua fora de escopo** (`SEC-01`): lockout e outbox em memória/sem *claim* não suportam ≥2 réplicas.

Para uso interno controlado, em instância única, com HTTPS e com o checklist da seção 6 já executado, a
entrega é aceitável. Para escala horizontal, primeiro migre o estado de tentativas de login (`SEC-01`).

**Condição para o registro ficar verdadeiro:** os itens 3 a 6 só mudam de **N** para **V** depois de executados;
nada neste documento antecipa esse resultado.

---

## 9. Apêndice — turno da noite de 03/11 (re-execuções e reconciliação de documentos)

Registro de um segundo turno de validação na mesma data, mantido separado do corpo (que é o fechamento do dia). Nada re-inventado; toda linha tem fonte citável. Sem Docker na máquina, como no resto do documento.

### 9.1 Re-execuções

| Quando | O quê | Resultado |
|---|---|---|
| 23:17 | OWASP Dependency-Check, plugin 12.2.2, perfil `security`, sem `NVD_API_KEY` | BUILD SUCCESS; 86 componentes; **0 vulneráveis**; NVD local com 911 registros; relatório em `backend/target/dependency-check/` |
| 23:11 | `frontend/package.json`: `overrides` `glob ^14.0.2` / `minimatch ^11.0.1` + `npm install` | `npm audit` = **0 vulnerabilidades** (total — produ e dev) |
| 23:17–23:18 | frontend: lint, format:check, `vue-tsc`, **20 testes** Vitest, build Vite 8.3.5, **E2E Playwright 3/3** (duas execuções: antes e depois dos overrides) | tudo verde |
| 23:19 | `mvnw test` | **214 testes, 0 falhas, 0 erros** |
| 23:20 | `mvnw clean verify` | BUILD SUCCESS, 214/0; JaCoCo regenerado |
| 23:21 | leitura do `jacoco.csv` regenerado | gate 516/522 = **98,85%** (igual à medição de 14:06); projeto **87,61% linhas · 71,43% branches · 86,84% instruções**; `LoginAttemptService` 66/71 linhas (92,9%), `ChangePasswordUseCase` 31/31 (100%), `EmailOutboxProcessor` 66/67 (98,5%), `JwtTokenService` 45/45 (100%), `AuthSessionService` 80/81 (98,8%) |

Consistência com o corpo: a projeção de cobertura global aproximou de 87,59% (14:06) para 87,61% (23:20) pela suíte nova; o gate dos 4 pacotes reproduziu exatamente 98,85%. O §8.5 (E2E mockada) continua valendo — a re-execução noturna rodou a mesma suíte mockada, **não** substitui a ressalva.

### 9.2 Reconciliação de documentos (conflito entre turnos)

O turno noturno reescreveu `README.md`, `SECURITY_REPORT.md`, `docs/operacao/RUNBOOK.md` e `docs/entrega/ROTEIRO_VIDEO.md`, **sobrescrevendo edições diurnas não commitadas** desses quatro arquivos; e criou `docs/arquitetura/ADRs/`, duplicando a convenção de `docs/arquitetura/`. Resolução aplicada no mesmo turno:

1. Versões noturnas salvas fora do repositório (`C:\Users\Chris\AppData\Local\Temp\opencode\night-backup\`).
2. Os quatro arquivos foram **restaurados do HEAD (`a9bbc7b`, 00:49)** — que já continha a documentação executiva da missão — e os números validados na noite foram reaplicados como deltas datados: README ("Testes e qualidade", "Análise de dependências", V7 na tabela de migrações, limitações), `SECURITY_REPORT.md` (§11 nova), RUNBOOK (cascata de reuso no §6; Dependency-Check no §7) e ROTEIRO_VIDEO (cenas 10–11 e fala de encerramento).
3. **As edições diurnas não commitadas desses quatro arquivos não são recuperáveis** — a transcrição da sessão diurna não está no banco do OpenCode e o snapshot é de 30/10. Declarado aqui para que a fonte de verdade fique explícita: os deltas diurnos desses arquivos têm uma perda remanescente; o conteúdo técnico não replicado (números de teste/cobertura, fechamentos de achados) está preservado nesta seção, na §11 do `SECURITY_REPORT.md` e nos ADRs.
4. `docs/arquitetura/ADRs/` (noite, sem contraparte por tópico com os ADRs diurnos) foi dissolvida: registro hexagonal → `ADR-007`, seed admin/troca obrigatória → `ADR-008`, agregações do relatório → `ADR-009`, funil de validação/SCA → `ADR-010`, todos no formato desta diretório, com índice atualizado.
5. `docs/entrega/RELATORIO_ENTREGA.*` (noite) é **complemento** de `ENTREGA.html`/PDF, não substituição; referências de ADRs e de HEAD corrigidas e o PDF regenerado.

O turno noturno **não revalidou** o checklist de runtime (§6) — apenas suítes e varreduras; o §6 diurno (68/68) permanece como evidência de runtime.
