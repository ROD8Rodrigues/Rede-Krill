# ADR-005 — Flyway como dono exclusivo do schema; Hibernate em `validate`

**Status:** Aceita
**Data:** 2026-11-02
**Escopo:** backend (`application.yml`, perfis `dev`/`prod`/`test`), migrações `V1`–`V6`, suíte de integração

---

## Contexto

O sistema tem regras de integridade que não cabem em anotações JPA: ID Pedido único em todo o banco, cancelamento lógico com `canceledAt`/`canceledBy` coerentes, multiplicadores fixos para os dois tipos obrigatórios de paletização, faixa horária de exatamente duas horas, hash único de refresh token. Um `ddl-auto: update` geraria schema a partir das entidades e deixaria todas essas regras fora do ar — e destruiria dados em renomeio de coluna.

## Decisão

- **`spring.jpa.hibernate.ddl-auto: validate`** em todos os perfis (`application.yml`, `application-test.yml`). A aplicação **recusa subir** se a entidade e o banco divergirem; ela nunca cria nem altera tabela.
- **`open-in-view: false`** — sem sessão de persistência ligada à requisição HTTP; o carregamento é explícito nas camadas de aplicação.
- **Flyway habilitado** com `locations: classpath:db/migration`. Toda mudança de schema é um arquivo versionado, com nome `V<n>__<descricao>.sql`.
- **`spring.flyway.clean-disabled: true`** no perfil `prod`. No perfil `test` é `false`, apenas para permitir ciclo rápido em banco embarcado efêmero.
- Migrações seguem regras escritas nos próprios comentários:
  - **Nenhum registro é removido.** `V3` faz *backfill* de `order_id` legados em faixa reservada a partir de `900000000000000000`, e só depois aplica `NOT NULL` + índice único.
  - **`CHECK ... NOT VALID`** para limites de 80 caracteres em `supplier_name`/`supplier_email`, de modo a impedir novos valores inválidos sem invalidar linhas antigas.
  - Constraints de status e de coerência de cancelamento (`ck_appointment_status`, `ck_appointment_cancellation`).
  - `V5` adiciona `must_change_password` e `version` com **default seguro**, para aplicar em banco já populado.
  - Índices de trabalho junto de cada constraint nova (`idx_auth_session_active`, `idx_email_outbox_pending`).
- **Teste de integração sem Docker nem Postgres local**: `EmbeddedPostgres` (Zonky) sobe um PostgreSQL real em `@SpringBootTest` com perfil `test`; o Flyway executa `V1`–`V6` nesse banco, então as migrações são exercitadas de verdade a cada `mvn test`.
- **Seed administrativo** fica na aplicação (`infrastructure/seed/DataInitializer`), não em migração: depende de hash BCrypt e de configuração por ambiente. Ele rejeita senha ausente, menor que 12 caracteres ou listada como conhecida, e aplica `require-password-change`.

## Consequências

**Positivas**
- O schema é revisável em code review e reproduzível em qualquer ambiente.
- `validate` transforma divergência entidade × banco em falha de inicialização imediata, não em erro em runtime de um fluxo raro.
- As migrações são testadas automaticamente pela suíte de integração (10 casos em `SchedulingApiIntegrationTest`).
- Dados de produção nunca são destruídos por decisão de mapeamento.

**Negativas / custo**
- Mudar entidade exige sempre dois artefatos (arquivo SQL + classe) — mais atrito no dia a dia.
- Migrações destrutivas continuam possíveis: `V5` faz `DROP TABLE capacity_settings` e remove a constraint `palletization_type_code_key`. `clean-disabled` protege contra `clean`, não contra um `V<n>` mal escrito. Rollback é por migração nova à frente, não por edição da anterior.
- O perfil `test` contém um segredo JWT (`cmVkZWtyaWxsLXRlc3Qtc2VjcmV0LWtleS0yMDI2ISE=`) para torná-la determinística. É exclusivo do perfil `test`, mas scanners de segredo vão sinalizar o arquivo — comportamento esperado, não credencial de produção.
- `outbox.enabled: false` no perfil `test` significa que o `EmailOutboxProcessor` **não** é exercitado pela suíte de integração; a tabela e o `enqueue` são, o agendador não.

## Evidência no código

| Item | Arquivo |
|---|---|
| `validate` + `open-in-view` + Flyway | `backend/src/main/resources/application.yml:8-14` |
| `clean-disabled` em produção | `backend/src/main/resources/application-prod.yml:2-5` |
| Backfill, índices e `CHECK NOT VALID` | `backend/src/main/resources/db/migration/V3__appointment_conformity.sql:8-66` |
| Constraints de referência e singleton histórico | `.../db/migration/V4__administrative_configuration.sql` |
| Defaults seguros, `auth_session`, remoções | `.../db/migration/V5__security_sessions_and_integrity.sql` |
| Outbox e índice de trabalho | `.../db/migration/V6__email_outbox.sql` |
| Configuração do perfil de teste | `backend/src/test/resources/application-test.yml` |
| PostgreSQL embarcado no teste | `.../src/test/java/com/redekrill/scheduling/integration/SchedulingApiIntegrationTest.java:3,33-35` + `pom.xml` (`embedded-postgres`) |
| Seed com validação de senha | `.../infrastructure/seed/DataInitializer.java:56-80` |

## Alternativas consideradas

- **`ddl-auto: update` em dev e `validate` em prod** — criaria dois schemas diferentes; o risco é descobrir em produção que o banco não bate.
- **Liquibase** — mais recursos (rollback por changeSet, XML/YAML), porém com curva maior e sem vantagem real para um projeto de migrações lineares em SQL puro.
- **Gerar schema só pelas entidades e versionar por diff externo** — perdaria as constraints de negócio que só existem no banco.
- **Seed em migração `V*`** — exigiria hash BCrypt gerado à mão no SQL e colocaria credencial no histórico de versionamento.
