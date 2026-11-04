# ADR-008 — Seed de administrador seguro e troca de senha obrigatória

**Status:** Aceita
**Data:** 2026-11-03
**Escopo:** backend (provisionamento do primeiro ADMIN) + frontend (fluxo de troca obrigatória)

## Contexto

A auditoria técnica anterior (`AUDITORIA_TECNICA_REDE_KRILL.md`, achado C1) registrou credencial padrão de administrador publicamente conhecida e hashing de senha em documentação pública. A primeira conta do sistema não podia nascer com senha conhecida — nem permanecer com senha temporária depois do primeiro acesso.

## Decisão

1. **Seed idempotente por variáveis**, sem credencial embutida: `SEED_ADMIN_ENABLED` (desligado por padrão), `SEED_ADMIN_EMAIL`, `SEED_ADMIN_PASSWORD` obrigatória e `SEED_ADMIN_REQUIRE_PASSWORD_CHANGE` (ligado por padrão em prod).
2. A senha do seed é conferida pela mesma **PasswordPolicy** do resto do sistema antes de qualquer hash: mínimo de 12 caracteres, letra e número, e a lista de valores proibidos (`admin@12345`, `redekrill`, `password`, …) inclui variantes de 12+ caracteres. Em produção, ausência de senha faz o boot **falhar**; em dev/test, o seed é ignorado com aviso explícito.
3. **Troca obrigatória no primeiro acesso:** a conta nasce com a trava de senha inicial; enquanto não trocar, o filtro bloqueia todas as rotas exceto `me`, `logout`, `change-password` e `csrf`, respondendo `403 PASSWORD_CHANGE_REQUIRED`. O frontend cai automaticamente na tela de troca.
4. Desativar usuário ou trocar senha **revoga imediatamente** todas as sessões (`revokeAllForUser`), inclusive as que ainda têm access token válido — o efeito é perceptível na próxima requisição.

## Consequências

- A senha inicial só existe no cofre do operador; nunca em log, código, configuração ou mensagem de erro.
- Nem o seed nem o reset por ADMIN permitem senha fraca: a mesma política vale para criação, seed e troca (fecha a assimetria registrada como `SEC-05` no relatório de segurança).
- Operador desatento travará sua primeira sessão até trocar a senha — intencional; o runbook documenta o passo.

## Evidência no código

- `backend/src/main/java/com/redekrill/scheduling/infrastructure/seed/DataInitializer.java:87-91` — piso de 12 caracteres + `passwordPolicy.isKnownOrWeak(password)` antes do hash; `:99-105` — aplicação de `seedProperties.requirePasswordChange()` na criação.
- `backend/src/main/java/com/redekrill/scheduling/infrastructure/config/properties/SeedProperties.java:13,36` — semântica da trava de troca obrigatória.
- `backend/src/main/java/com/redekrill/scheduling/domain/policy/PasswordPolicy.java:33` (`MIN_LENGTH = 12`), `:37-39` (lista de proibidas), `:80` (exigência de letra e número).
- `backend/src/main/java/com/redekrill/scheduling/infrastructure/security/PasswordChangeRequiredFilter.java` — bloqueio com `403 PASSWORD_CHANGE_REQUIRED` (reservado na superfície em `SECURITY_REPORT.md` §3.2).
- `backend/src/main/java/com/redekrill/scheduling/application/usecase/user/CreateUserUseCase.java:64` e `backend/src/main/java/com/redekrill/scheduling/presentation/request/CreateUserRequest.java:34` — política única também na criação por ADMIN.
- `backend/src/main/java/com/redekrill/scheduling/application/usecase/auth/ChangePasswordUseCase.java:90` — revogação de todas as sessões na troca.
- `backend/src/main/java/com/redekrill/scheduling/infrastructure/config/ProductionConfigurationValidator.java:83-88` — fail-fast de configuração de produção (senha do seed ausente/curta/conhecida recusa o boot).

## Alternativas consideradas

- **Senha gerada pelo sistema e impressa em log** — recusada: colocaria segredo real em destino versionado/exportado.
- **E-mail de convite com link de primeiro acesso** — recusada: o boot passaria a depender de SMTP externo configurado antecipadamente.
- **Troca de senha opcional** — recusada: manteria vivo o vetor C1 (senha temporária conhecida utilizável indefinidamente).
- **Seed sempre ligado com recriação silenciosa** — recusada: recriaria a conta (e sobrescreveria revogações) a cada start; idempotência "só cria se e-mail não existir" evita tanto a ausência quanto a recriação.
