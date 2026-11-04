# Runbook Operacional — Rede Krill Scheduling

**Público:** quem instala, opera e dá suporte à aplicação.
**Base:** código em 2026-11-02 (Spring Boot 4.1.2, migrações `V1`–`V6`) e [SECURITY_REPORT.md](../../SECURITY_REPORT.md).
**Complementos:** [README.md](../../README.md) (instalação e API) e [docs/arquitetura/](../arquitetura/README.md) (por que as coisas são assim).

> Este runbook descreve procedimentos verificados por leitura de código e configuração. As rotinas que exigem ambiente em execução estão marcadas com **⚠️ não executado nesta máquina** — a máquina de desenvolvimento não tem Docker, e nenhuma stack foi subida durante a documentação.

---

## 1. Visão do sistema

| Componente | Papel | Porta padrão | Perfil que o habilita |
|---|---|---|---|
| `frontend` (Nginx) | SPA estática + proxy de `/api`, `/swagger-ui*`, `/v3/api-docs` | `8081` → 80 | todos |
| `backend` (Spring Boot) | API REST, sessão, outbox, auditoria | `8080` | todos |
| `postgres` 16 | Banco com schema gerido pelo Flyway | `5432` | todos |
| `mailhog` | SMTP de desenvolvimento + UI de inspeção | `1025` / `8025` | **apenas profile `dev`** |

Ordem de subida garantida por `depends_on` com `condition: service_healthy`: Postgres (healthcheck `pg_isready`) → backend (healthcheck em `/actuator/health`) → frontend.

### Perfis

| Perfil | Cookies | Swagger | Seed | JWT sem segredo | Uso |
|---|---|---|---|---|---|
| `dev` | `Secure=false`, `SameSite=Lax` | habilitado | habilitado, sem troca forçada | aceita segredo efêmero | máquina local |
| `prod` | `Secure=true`, `SameSite=Strict`, obrigatórios | **desabilitado por padrão** | **desabilitado por padrão** | recusa iniciar | qualquer ambiente real |
| `test` | cookies sem `Secure`, token exposto | — | — | segredo fixo versionado | suíte automatizada |

O perfil `prod` é validado no boot por `ProductionConfigurationValidator`: se `JWT_SECRET` faltar ou for curto, `AUTH_COOKIE_SECURE=false`, `AUTH_EXPOSE_TOKEN_IN_RESPONSE=true`, `DB_PASSWORD` padrão ou `SEED_ADMIN_PASSWORD` fraca com seed ligado, **a aplicação não sobe** — isso é comportamento esperado, não bug.

---

## 2. Variáveis que quebram a operação se erradas

| Variável | Sintoma se errada | Correção |
|---|---|---|
| `JWT_SECRET` ausente (não-dev) | Boot falha: "JWT_SECRET é obrigatório" | `openssl rand -base64 48` |
| `JWT_SECRET` < 256 bits | Boot falha em produção | gerar novo Base64 de 48 bytes |
| `JWT_SECRET` trocado em dev | Todos os access tokens viram inválidos no restart | esperado; sessões no banco continuam válidas, o refresh repõe o access |
| `AUTH_COOKIE_SECURE=true` acessando por `http://` | Login retorna 200 mas a sessão não persiste; o SPA volta ao login | usar perfil `dev` em http, ou servir por HTTPS |
| `CORS_ALLOWED_ORIGINS` sem a origem real | `403 Invalid CORS request` nas requisições de escrita — o `Origin` do navegador chega ao backend **mesmo** passando pelo proxy do Vite | listar a origem exata (sem `/` final), incluindo portas de fallback como `5174`/`5175` |
| `TRUST_FORWARDED_HEADERS=false` atrás de proxy | Rate limit agrupa todos os usuários no IP do proxy; `auth_session.ip_address` inútil | ligar **e** configurar `FORWARD_HEADERS_STRATEGY=native` (padrão em prod) |
| `SEED_ADMIN_PASSWORD` vazia ou < 12 caracteres | Nenhum admin criado (warn em dev; falha em prod) | definir senha forte ≥12, não listada como conhecida |
| `EMAIL_OUTBOX_ENABLED=false` | E-mails nunca saem, mas a fila continua crescendo | religar e acompanhar `email_outbox` |
| `SPRINGDOC_ENABLED` | Swagger 403/inexistente em prod (esperado) ou exposto onde não deveria | decisão de ambiente, não correção |

---

## 3. Provisionamento inicial

**⚠️ não executado nesta máquina.**

1. Subir banco e aplicar migrações: o Flyway roda no boot do backend e cria `V1`–`V6`. Confira `flyway_schema_history` se precisar auditar.
2. Criar o primeiro ADMIN com seed temporário:
   ```
   SPRING_PROFILES_ACTIVE=dev
   SEED_ADMIN_ENABLED=true
   SEED_ADMIN_EMAIL=admin@redekrill.com
   SEED_ADMIN_REQUIRE_PASSWORD_CHANGE=true
   SEED_ADMIN_PASSWORD=<senha forte, >=12 caracteres>
   ```
   O seed é idempotente: se o e-mail já existir, nada é feito.
3. Entrar, trocar a senha (a troca **revoga todas as sessões** e derruba os cookies — faça login de novo).
4. **Desligar o seed** (`SEED_ADMIN_ENABLED=false`) e nunca manter senha de admin em arquivo versionado.
5. Criar os demais usuários pela tela **Usuários** (perfil USER para consulta). A senha inicial de usuário exige mínimo 8 caracteres; o próprio usuário será forçado a trocá-la se `mustChangePassword` for definido.

---

## 4. Rotinas de operação

### 4.1 Verificar se está saudável

```bash
curl -s http://<host>:8080/actuator/health          # {"status":"UP"} — sem detalhes, por config
curl -s http://<host>:8081/                          # SPA respondendo
```

Métricas (requer token de ADMIN): `GET /actuator/prometheus`. Métrica própria do sistema:

- `email_outbox_processed_total{status="sent"|"failed"|"retry"}` — contagem do processador de outbox. `retry` subindo de forma contínua = SMTP degradado; `failed` subindo = mensagens morreram após `EMAIL_OUTBOX_MAX_ATTEMPTS`.

Demais séries são as padrão do Micrometer/Actuator (JVM, HTTP, pools).

### 4.2 Desbloquear login (rate limit)

O bloqueio é **em memória, por instância**, com janela deslizante: limiar padrão de 5 tentativas por conta ou 20 por IP em 15 min, lockout de 5 min, progressivo até 4× (máx. 20 min).

- **Não há tabela para limpar.** A resposta `429` traz `Retry-After` em segundos — respeite e aguarde.
- Reiniciar o backend zera todos os bloqueios (também zera o estado legítimo de proteção).
- Com mais de uma réplica atrás de load balancer, o contador **não é compartilhado**: o ataque se distribui. Este é o risco `SEC-01` — mitigação operacional até existir store externo: sessões sticky ou uma réplica de escrita.
- Trilha: `audit_log` com `action = 'LOGIN_FAILED'` (credencial inválida) e `'LOGIN_BLOCKED'` (tentativa durante bloqueio).

```sql
SELECT created_at, actor_email, action, details
FROM audit_log
WHERE action IN ('LOGIN_FAILED','LOGIN_BLOCKED','ACCESS_DENIED')
ORDER BY created_at DESC LIMIT 50;
```

### 4.3 Revogar sessões

| Objetivo | Como |
|---|---|
| Uma sessão | Logout pela UI, ou `UPDATE auth_session SET revoked_at = now() WHERE id = ?;` |
| Todas de um usuário | Troca de senha pelo próprio usuário, ou `UPDATE auth_session SET revoked_at = now() WHERE user_id = ? AND revoked_at IS NULL;` |
| Desativar o usuário | `PUT /api/users/{id}` com `active=false` — o filtro de autenticação exige `User::active`, então o acesso para na requisição seguinte |

Um access token ainda dentro da validade **deixa de funcionar** na hora, porque a claim `sid` é verificada contra a tabela (`JwtAuthenticationFilter`). É o mecanismo de revogação real do sistema.

```sql
-- sessões ativas de um usuário
SELECT s.id, s.created_at, s.expires_at, s.ip_address, s.user_agent
FROM auth_session s JOIN users u ON u.id = s.user_id
WHERE u.email = 'fulano@redekrill.com' AND s.revoked_at IS NULL AND s.expires_at > now();
```

O hash de refresh fica em `refresh_token_hash` (SHA-256). **Nunca** há valor bruto no banco — se aparecer em log, é incidente.

### 4.4 Fila de e-mail (outbox)

```sql
SELECT status, count(*) FROM email_outbox GROUP BY status;

SELECT id, appointment_id, recipient, attempts, next_attempt_at, last_error
FROM email_outbox WHERE status = 'PENDING' ORDER BY next_attempt_at LIMIT 20;
```

| Situação | Ação |
|---|---|
| `PENDING` com `attempts` subindo e `next_attempt_at` no futuro |正常工作 do retry exponencial (60 s → 120 → 240…); só observe |
| `FAILED` após 5 tentativas | Corrigir SMTP e reabrir: `UPDATE email_outbox SET status='PENDING', attempts=0, next_attempt_at=now() WHERE id = ?;` |
| Fila crescendo sem consumo | Ver `EMAIL_OUTBOX_ENABLED`, logs do `EmailOutboxProcessor`, conectividade `MAIL_HOST:MAIL_PORT` |
| SMTP autenticado | `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH=true`, `MAIL_STARTTLS=true` |
| Dúvida se um agendamento foi notificado | `SELECT * FROM email_outbox WHERE appointment_id = ?;` — uma linha por tentativa de criação |

E-mail duplicado é possível se o processo morrer entre o envio e o `markSent`. Aceito pelo [ADR-003](../arquitetura/ADR-003-outbox-transacional-de-email.md).

Em dev, o MailHog em `http://localhost:8025` mostra o que foi entregue.

### 4.5 Conflitos comuns de agendamento

| Resposta | Causa | O que dizer ao usuário |
|---|---|---|
| `409` "Já existe um agendamento com o ID Pedido N" | `orderId` repetido (checagem de negócio) | Informe outro ID |
| `409` "O registro conflita com dados existentes" | Colisão simultânea pega pelo índice único `uk_appointment_order_id` | Tente novamente; o primeiro registro valeu |
| `409` "O registro foi alterado por outro usuário" | Lock otimista (`version`) | Recarregue a tela e reaplique a edição |
| `warnings` no retorno | Capacidade 480/120 excedida | **Não é erro** — o agendamento foi salvo; ocupação visível no painel |
| `403 PASSWORD_CHANGE_REQUIRED` | Senha inicial não trocada | Acessar *Troca de senha* |
| `403` sem código | CSRF ausente (mutação sem `X-XSRF-TOKEN`) | Recarregar a página; verificar cookies bloqueados pelo navegador |

Nenhuma dessas respostas contém stacktrace ou detalhe de SQL — a causa técnica vai apenas ao log.

### 4.6 Rastrear uma requisição

Todo request ganha `X-Correlation-ID` (gerado ou propagado do cliente), devolvido no header e inserido no MDC. O padrão de log local já imprime `[<correlationId>]`; em `prod` o formato é estruturado (`LOG_STRUCTURED_FORMAT=ecs`).

```
GET /api/appointments 403 ... [3f9c...] c.r.s.presentation.exception.GlobalExceptionHandler - ...
```

Para investigar um incidente: peça o `X-Correlation-ID` ao usuário (aparece no erro do navegador, aba Network) e filtre os logs por ele; cruze com `audit_log.created_at` na mesma janela.

### 4.7 Rotação do `JWT_SECRET`

1. Gere o novo segredo fora da aplicação: `openssl rand -base64 48`.
2. Atualize `JWT_SECRET` no `.env`/secret manager e faça deploy/restart.
3. Efeito: todos os access tokens deixam de assinar válido; **as sessões permanecem no banco**, então o refresh rotativo emite tokens novos de forma transparente na próxima chamada. Usuários não precisam logar de novo.
4. Se quiser forçar reautenticação geral, revogue sessões em massa (`UPDATE auth_session SET revoked_at = now() WHERE revoked_at IS NULL;`) — nesse caso a rotação é sentida por todos.

### 4.8 Backup

- `pg_dump` do banco. Inclui `auth_session` e `email_outbox`: restaurar um backup antigo reativa sessões e mensagens já entregues. Se for restaurar de ponto no tempo, **revogue todas as sessões** depois.
- Guarde o `.env`/secrets fora do repositório. Não há segredo real versionado desde a reforma de segurança.

### 4.9 Login falha no ambiente local (sem Docker)

Quatro causas já observadas nesta máquina, cada uma com sintoma próprio:

| Sintoma | Causa | Correção |
|---|---|---|
| Boot com "JWT_SECRET é obrigatório" | `.env` não chega ao processo. O Spring **não** lê `.env` (não há `spring-dotenv` no `pom.xml`); quem lê é o `docker compose`. Sem perfil `dev`, o segredo passa a ser obrigatório | no Windows use `.\start-dev.ps1` (exporta o `.env` e mapeia `POSTGRES_*` → `DB_*`), ou exporte as variáveis à mão |
| Boot com "no password was provided" / conexão recusada ao banco | `.env` traz `POSTGRES_PASSWORD`, o app lê `DB_PASSWORD` | mesmo mapa do `start-dev.ps1`; fora dele, defina `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` |
| `POST /api/auth/login` → `403` `Invalid CORS request` (corpo texto puro) | a origem do navegador não está em `CORS_ALLOWED_ORIGINS` — acontece quando o Vite cai para 5174/5175 porque 5173 está ocupada | incluir a porta real no `.env` e reiniciar o backend |
| `POST` autenticado → `403` `ACCESS_DENIED` (corpo JSON) | token CSRF errado. O cookie `XSRF-TOKEN` carrega o valor **bruto**; o servidor só aceita o valor **mascarado** que sai no corpo de `GET /api/auth/csrf` (proteção BREACH). O axios, quando `withXSRFToken` fica no padrão, sobrescreve o cabeçalho com o valor bruto do cookie **depois** dos interceptors | clientes com `withXSRFToken: false` e `ensureCsrf()` buscando um token novo a cada escrita (`frontend/src/services/api.js:39`). Login/logout geram token bruto novo, por isso nada pode ser cacheado |

- `429` "Muitas tentativas de login" durante o teste: ver `4.2` — reiniciar o backend zera o contador em memória.
- O `e2e/auth.spec.js` intercepta `/api/auth/*` com mocks, então **nenhuma** dessas quatro falhas aparece no Playwright. Validar login local é sempre contra o backend real.

---

## 5. Migrações, deploy e rollback

**Regra:** o schema pertence ao Flyway. Hibernate está em `validate` — a aplicação **recusa subir** se entidade e banco divergirem. erro de `Schema-validation` no boot quase sempre significa aplicação nova contra banco velho (ou vice-versa).

Sequência segura:

1. Backup + teste de restauração.
2. Migrações `V5`/`V6` podem rodar com a aplicação antiga no ar? **Não** — elas adicionam colunas com `DEFAULT` e tabelas novas (compatível), mas o boot da versão antiga não conhece `auth_session`; a ordem correta é **banco primeiro, aplicação depois**, em janela de manutenção.
3. Suba a nova versão. Confira `/actuator/health` e o log de migração do Flyway.
4. Smoke test: login (ADMIN e USER), criar agendamento, conferir `emailQueued`, trocar senha, ver auditoria.

**Rollback:** Flyway comunidade não faz downgrade. Nunca edite um `V<n>` já aplicado. Para reverter, aplique uma migração nova à frente desfazendo o efeito — e observe:

| Migração | Reversível? | Observação |
|---|---|---|
| `V5__security_sessions_and_integrity.sql` | Parcial | Adições (`must_change_password`, `version`, `auth_session`) são inofensivas para versão antiga. Mas `V5` **remove `capacity_settings`** e a constraint `palletization_type_code_key` — reverter exige recriar objetos e a tabela nunca teve valores úteis (ver [ADR-004](../arquitetura/ADR-004-limites-fixos-de-capacidade.md)) |
| `V6__email_outbox.sql` | Sim | `DROP TABLE email_outbox` descarta histórico de notificações |
| `V3` | **Não** | Backfill consumiu a faixa de `orderId` reservada em `900000000000000000+` e aplicou `NOT NULL`. Reverter deixaria registros sem ID |

Versão antiga da aplicação **não funciona** contra o banco novo? Na prática funciona para leitura, mas sem revogação de sessão — o que é, por si, motivo para preferir roll-forward.

---

## 6. Incidentes de segurança — o que fazer

| Sintoma | Procedimento |
|---|---|
| Credencial suspeita / token capturado | Desative o usuário (`active=false`) — derruba na próxima requisição. Registre em `audit_log` a investigação |
| `LOGIN_FAILED` em massa de um e-mail | Brute force direcionado: o lockout já está agindo. Se distribuído por IP, ver `ip_address` em `auth_session` e bloquear na borda |
| Reuso do mesmo refresh token (erros `401` em série na mesma conta) | **Revogação em cascata automática** (implementada em 03/11/2026 — adendo do `ADR-001`): reutilizar um token já consumido revoga **todas as sessões do usuário** e registra `REFRESH_TOKEN_REUSE` na auditoria (`AuthSessionService.java:121-150`). Fallback manual, se ambiente exigir: `UPDATE auth_session SET revoked_at = now() WHERE user_id = ? AND revoked_at IS NULL;` e força nova senha |
| Requisição com `Authorization` de outro sistema aparecendo nos logs | O header não é gravado em `audit_log` por design; confirme e investigue o logger customizado |
| Payload sensível em `email_outbox.last_error` | `VARCHAR(1000)` e contém a mensagem do SMTP; revise após correção do servidor de mail |

Nada disso foi testado contra ambiente em execução — são instruções derivadas do comportamento implementado.

---

## 7. Checklist de go-live

- [ ] `JWT_SECRET` próprio, ≥256 bits, fora do repositório
- [ ] `POSTGRES_PASSWORD` / `DB_PASSWORD` próprios, sem valor padrão
- [ ] Acesso servido por **HTTPS** (senão, cookies `Secure` do perfil `prod` não funcionam)
- [ ] `TRUST_FORWARDED_HEADERS=true` **e** proxy configurando `X-Forwarded-For` corretamente
- [ ] `CORS_ALLOWED_ORIGINS` com as origens reais (ou acesso exclusivamente via proxy Nginx)
- [ ] `SEED_ADMIN_ENABLED=false`, primeiro ADMIN criado e com senha trocada
- [ ] SMTP real testado (não MailHog), `MAIL_SMTP_AUTH`/`STARTTLS` conforme o provedor
- [ ] Backup automático do PostgreSQL + teste de restauração
- [ ] `docker compose config` validado (⚠️ ainda não feito — sem Docker nesta máquina)
- [ ] CI verde no commit da reforma (⚠️ ainda não executado sobre este código)
- [ ] OWASP Dependency-Check rodado com `NVD_API_KEY` e triado — já executado em 03/11/2026 **sem** chave nesta máquina (cache local do NVD, 911 registros): BUILD SUCCESS, 0 vulneráveis em 86 componentes; relatório em `backend/target/dependency-check/` (`SEC-03` tratada — `SECURITY_REPORT.md` §11). A varredura agendada com `NVD_API_KEY` fica no CI.
- [ ] Logs com `correlationId` chegando ao coletor; métrica do outbox monitorada
- [ ] Usuários finais sem credenciais compartilhadas e com troca de senha concluída

---

## 8. Escalabilidade e topologia

A aplicação é stateless no protocolo HTTP (sem sessão servlet), **mas não é stateless nos controles**:

| Recurso | Estado | Efeito com N réplicas |
|---|---|---|
| Rate limit de login | memória da instância | limiar efetivo multiplicado por N |
| Processador de outbox | `@Scheduled` em cada instância, sem *claim* | risco de envio duplicado |
| Sessão/revogação | PostgreSQL | ✔ funciona bem |
| Auditoria, capacidade, relatórios | PostgreSQL | ✔ funciona bem |

Para escalar horizontalmente é preciso antes: store compartilhado de tentativas de login (Redis) e *claim* de mensagem (`SKIP LOCKED` ou coluna de lock). Está registrado como consequência dos [ADR-001](../arquitetura/ADR-001-sessao-com-refresh-token-em-cookie-httponly.md) e [ADR-003](../arquitetura/ADR-003-outbox-transacional-de-email.md).

---

## 9. Comandos de diagnóstico rápido

```bash
# versão e saúde
curl -s http://localhost:8080/actuator/health
curl -s http://localhost:8080/actuator/info

# backend sem Docker (usa JAVA_HOME; confirme o JDK 21)
cd backend && ./mvnw spring-boot:run
cd frontend && npm run dev

# banco via psql
psql "$DB_URL" -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
psql "$DB_URL" -c "SELECT status, count(*) FROM email_outbox GROUP BY status;"
psql "$DB_URL" -c "SELECT count(*) FROM auth_session WHERE revoked_at IS NULL AND expires_at > now();"
```

```bash
# rebuild local do backend (JDK 21 obrigatório)
cd backend && JAVA_HOME=/caminho/para/jdk21 ./mvnw -B clean package
```

---

*Documento derivado exclusivamente de código, configuração e artefatos de teste existentes em 2026-11-02. Itens marcados com ⚠️ não foram executados.*
