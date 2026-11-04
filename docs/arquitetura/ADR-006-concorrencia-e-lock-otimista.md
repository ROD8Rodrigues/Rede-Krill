# ADR-006 — Concorrência: unicidade de ID Pedido em camadas + lock otimista por `version`

**Status:** Aceita (revisada em 2026-11-03 — ver "Divergência de contrato")
**Data:** 2026-11-02
**Escopo:** domínio (`Appointment`, `User`), persistência (entidades JPA, migração `V5`), API (`GlobalExceptionHandler`)

---

## Contexto

A auditoria técnica anterior registrou dois problemas de concorrência (`C8`):

1. **ID Pedido duplicado** criado por duas requisições simultâneas gerava **HTTP 500** com mensagem crua de violação de constraint — vazando detalhe de banco e tratando como erro interno algo que é colisão de negócio esperada.
2. **Atualizações se perdiam**: dois operadores editando o mesmo agendamento faziam o último salvar silenciosamente por cima do primeiro (*lost update*), sem nenhum aviso.

## Decisão

**Unicidade de `orderId` em três camadas**, cada uma com papel diferente:

1. **Verificação de negócio** no caso de uso: `existsByOrderId(orderId)` no `POST` e `existsByOrderIdAndIdNot(orderId, id)` no `PUT`, lançando `ConflictException` com mensagem legível ("Já existe um agendamento com o ID Pedido N"). Barato, rápido e cobre o caso comum.
2. **Constraint no banco**: `uk_appointment_order_id` (índice único criado em `V3` depois do backfill). É a única camada que sobrevive à corrida — duas transações que passam pela checagem (1) colidem aqui.
3. **Mapeamento de erro no handler**: `DataIntegrityViolationException` ⇒ **`409 Conflict`** com mensagem neutra; a causa técnica vai **só para o log** (`log.warn` com `getMostSpecificCause()`), nunca para o corpo da resposta.

**Lock otimista para atualização:**

- Coluna `version` adicionada em `V5` (`users` e `appointment`), `NOT NULL DEFAULT 0` para aplicar sem renumeração.
- `@Version` nas entidades JPA (`AppointmentJpaEntity:85`, `UserJpaEntity:49`) e o campo propagado no modelo de domínio.
- `OptimisticLockingFailureException` ⇒ **`409`** com instrução de recarregar, em vez de sobrescrever.

**Sem retry automático no servidor:** um conflito é devolvido ao operador, porque a resolução é semântica (o ID Pedido pode ser outro, não o mesmo mais tarde).

## Consequências

**Positivas**
- Colisão de ID Pedido deixa de ser erro 500 e vira contrato `409` documentado; nenhum detalhe de SQL ou de constraint vaza ao cliente.
- Edição simultânea passa a ser detectável: o segundo escritor é obrigado a recarregar e ver o estado atual.
- `DEFAULT 0` permitiu aplicar a migração em banco populado sem bloqueio longo nem recriação de tabela.
- O índice único é a garantia final: mesmo com um bug na checagem de negócio, o banco não aceita duplicidade.

**Negativas / custo**
- A checagem de negócio (camada 1) é naturalmente **susceptível a corrida** — não há `SELECT ... FOR UPDATE` nem *flush* explícito antes do commit. O desfecho correto vem da camada 2. Isso é intencional: a checagem existe para dar **mensagem boa**, não para garantir **unicidade**.
- `@Version` incrementa a cada save, inclusive quando a mudança é só de campos derivados (`effectivePallets` recalculado), o que pode gerar `409` em fluxos de recálculo automático — o frontend precisa recarregar em vez de insistir.
- Toda entidade com lock otimista precisa propagar `version` pelos mappers; esquecer um deles produz `version` sempre 0 em memória e conflitos silenciosos. Os mappers hoje tratam isso (`AppointmentPersistenceMapper`, `UserPersistenceMapper`).
- Não há *idempotency key* no `POST`: dois cliques em "salvar" com o mesmo payload resultam em `409` no segundo — comportamento correto, mas o UX depende do frontend apresentar como "já registrado" e não como falha genérica.

## Evidência no código

| Item | Arquivo |
|---|---|
| Checagem de unicidade no `POST` | `backend/src/main/java/com/redekrill/scheduling/application/usecase/appointment/CreateAppointmentUseCase.java:83-85` |
| Checagem de unicidade no `PUT` | `.../application/usecase/appointment/UpdateAppointmentUseCase.java:72-73` |
| Portas de consulta | `.../domain/repository/AppointmentRepository.java:48,50` |
| Índice único e backfill | `backend/src/main/resources/db/migration/V3__appointment_conformity.sql:25,41` |
| Coluna `version` | `backend/src/main/resources/db/migration/V5__security_sessions_and_integrity.sql:6-11` |
| `@Version` JPA | `.../infrastructure/persistence/entity/AppointmentJpaEntity.java:85`, `.../entity/UserJpaEntity.java:49` |
| `409` para violação de integridade (sem vazar causa) | `.../presentation/exception/GlobalExceptionHandler.java:149-157` |
| `409` para lock otimista | `.../presentation/exception/GlobalExceptionHandler.java:159-166` |

## Alternativas consideradas

- **`INSERT ... ON CONFLICT DO NOTHING` + leitura do resultado** — resolveria a corrida na camada 1 com uma instrução, mas exigiria escapar do JPA para SQL nativo e perderia a mensagem de negócio por campo. Ficou como otimização futura.
- **Tabela de reserva de ID Pedido com lock pessimista** — garante unicidade com mensagem boa, mas cria ponto de contenção e mais uma tabela para auditar.
- **Lock pessimista (`@Lock(PESSIMISTIC_WRITE)`) na atualização** — eliminaria o `409`, ao custo de segurar linha durante a edição; para um formulário que pode ficar aberto minutos, é o remédio errado.
- **`ETag`/`If-Match` em HTTP** — semanticamente ideal para `PUT` condicional, porém exigiria mudar o contrato da API e o cliente.

## Divergência de contrato (registrada na revisão de 2026-11-03)

O texto original fechava a alternativa do `ETag` dizendo que "`version` no corpo cumpre o mesmo papel com menos ruptura". **Isso não é verdade no código atual.** Busca em `presentation/request`, `presentation/response` e `application/dto` não encontra nenhum campo `version`: o contrato da API não transporta a versão.

O que existe, e funciona:

- `@Version` em `AppointmentJpaEntity:85` e `UserJpaEntity:49`, com `version` lida dentro da transação de cada requisição.
- Duas edições concorrentes do mesmo agendamento fazem o segundo `UPDATE` afetar zero linhas, e o Hibernate lança `OptimisticLockingFailureException`, mapeado para `409` pelo handler. A proteção contra *lost update* é **server-side por transação** e não depende do cliente enviar nada.

O que **não** existe:

- Precondição cliente-servidor. O cliente não pode dizer "grava só se o estado for o que eu li", porque nunca recebeu nem devolve `version`. O `409` aparece apenas quando duas requisições se sobrepoem no banco — um conflito lógico entre leitura antiga e escrita posterior passa despercebido.

Consequência prática: a alternativa `ETag`/`If-Match` continua aberta, e a redação anterior não deve ser lida como se estivesse implementada.
