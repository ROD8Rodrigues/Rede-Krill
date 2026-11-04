# ADR-003 — Outbox transacional para notificação por e-mail

**Status:** Aceita (revisada em 2026-11-03)
**Data:** 2026-11-02
**Escopo:** backend (`application/service/EmailOutboxService`, `infrastructure/email/EmailOutboxProcessor`, `domain/model/EmailOutboxMessage`, migrações `V6` e `V7`) e contrato da API

---

## Contexto

Depois de salvar um agendamento, o sistema precisa notificar o fornecedor por e-mail. A implementação original fazia o envio **dentro da mesma transação** do `POST /api/appointments`, o que gera três problemas:

1. **Falha de SMTP derruba o agendamento** ou obriga a capturar exceção e fingir sucesso — nos dois casos o comportamento é ruim.
2. **Latência e acoplamento**: o `POST` fica preso ao tempo de resposta do servidor de mail (MailHog em dev; SMTP real em produção).
3. **Perda de rastro**: se o envio falha, não há como tentar de novo nem de saber o que deixou de ser entregue.

A semântica antiga do campo `emailSent` no corpo da resposta também era ambígua: `true` significava "SMTP aceitou agora", `false` significava "falhou", sem estado intermediário.

## Decisão

Adotar o padrão **transactional outbox**:

- A mensagem de e-mail é escrita na **mesma transação** do agendamento, logo após `appointmentRepository.save(...)` e do registro de auditoria (`emailOutboxService.enqueue(saved, warnings)`). Se o `COMMIT` falhar, nem o agendamento nem a mensagem existem.
- O envio acontece **fora de qualquer transação**: o `@Scheduled` (`fixedDelayString = ${EMAIL_OUTBOX_POLL_INTERVAL_MS:30000}`) abre uma transação curta apenas para listar os ids do lote (`EMAIL_OUTBOX_BATCH_SIZE`, 20) e outra para ler cada mensagem; a chamada SMTP roda sem transação aberta. Cada mensagem é processada por `processOne(id)` em try/catch próprio, então uma mensagem que quebra não interrompe o restante do lote.
- **Retry exponencial** com teto: `initialBackoffSeconds * 2^min(attempts,10)`, limitado a 3600 s; padrão 60 s → 120 → 240…; após `EMAIL_OUTBOX_MAX_ATTEMPTS` (5) a mensagem passa a `FAILED`.
- Estados restritos por `CHECK` no banco: `PENDING`, `SENT`, `FAILED`, `CANCELED`; `attempts >= 0`. Nenhuma mensagem é removida — o histórico de entrega fica consultável.
- **Cancelamento suprime a notificação**: `CancelAppointmentUseCase` chama `emailOutboxService.cancelPending(appointmentId)` dentro da mesma transação do cancelamento, e o processador ainda reavalia o status antes de enviar. A mensagem passa ao estado terminal `CANCELED` (migração `V7` ampliou o `CHECK`): nenhum e-mail é enviado para agendamento cancelado e nenhuma linha é apagada.
- O `warnings` do agendamento (capacidade excedida) é gravado junto da mensagem, para que o corpo do e-mail reflita o mesmo aviso visto pelo operador.
- **Métricas**: contador `email_outbox_processed_total{status=sent|failed|retry|canceled}` exposto em `/actuator/prometheus` (ADMIN).
- **Contrato da API passa a ter dois campos** (`AppointmentCommandResult` / `AppointmentCommandResponse`):
  - `emailQueued` — `true` quando a mensagem foi aceita pelo outbox na mesma transação;
  - `emailSent` — `true` **somente** quando o SMTP confirmou o envio; no `POST` de criação ele é `false` por construção, porque o envio é assíncrono.
- O `PUT` de atualização continua sem reenviar notificação.

## Consequências

**Positivas**
- Agendar nunca falha por causa de servidor de e-mail indisponível; a notificação se recupera sozinha.
- Uma mensagem defeituosa não derruba o lote: o `try/catch` por mensagem mantém as demais no processamento.
- Cancelar um agendamento não gera mais confirmação de e-mail ao fornecedor — comportamento que antes só existia na intenção, não no código.
- Ganho de latência no `POST` (não espera SMTP) e trilha operacional de entrega por agendamento.
- A duplicidade de envio no restart é limitada ao intervalo entre "enviado" e "marcado como `SENT`" — janela estreita e aceita (e-mail duplicado é menos pior que e-mail perdido).

**Negativas / custo**
- O cliente **não pode** tratar `emailSent=false` como falha. A leitura correta de "aceito para entrega" é `emailQueued`. Isso é uma mudança de contrato e precisa estar na documentação de API e no README — o README antigo descrevia exatamente a semântica oposta.
- Entrega passa a ser **eventually consistent**: o fornecedor pode receber a confirmação até 30 s depois.
- O `@Scheduled` roda em todas as réplicas. Em implantação multi-instância são necessárias duas mudanças: *claim* da mensagem (`UPDATE ... WHERE status='PENDING' FOR UPDATE SKIP LOCKED` ou coluna de lock) e um líder. Nesta versão a topologia alvo é de instância única; o processador usa transações curtas por etapa (listar ids, ler mensagem, persistir estado) sem *claim* distribuído — risco registrado. A supressão por cancelamento é idempotente, mas duas réplicas ainda podem enviar a mesma mensagem antes de marcar `SENT`.
- Retenção de PII: `recipient` (e-mail do fornecedor) e `last_error` ficam indefinidamente na tabela (achado `SEC-08`).
- `EMAIL_OUTBOX_ENABLED=false` desliga o processador, mas as linhas continuam sendo criadas pelo `enqueue` — é uma configuração de operação, não um desligamento do outbox.

## Evidência no código

| Comportamento | Arquivo |
|---|---|
| `enqueue` dentro da transação do `POST` | `backend/src/main/java/com/redekrill/scheduling/application/usecase/appointment/CreateAppointmentUseCase.java:120` |
| Registro da mensagem com `PENDING` | `.../application/service/EmailOutboxService.java:25-27`, `.../domain/model/EmailOutboxMessage.java:32-35` |
| Supressão das mensagens pendentes no cancelamento | `.../application/usecase/appointment/CancelAppointmentUseCase.java:64`, `.../application/service/EmailOutboxService.java:39` |
| Estado terminal `CANCELED` (`markCanceled`) | `.../domain/model/EmailOutboxMessage.java:52`, `.../domain/model/EmailOutboxStatus.java:7` |
| Poll agendado, transação curta e `processOne` por mensagem | `.../infrastructure/email/EmailOutboxProcessor.java:70-87` e `:89-119` |
| Reavaliação do status do agendamento antes do SMTP | `.../infrastructure/email/EmailOutboxProcessor.java:101-105` |
| Backoff exponencial com teto e transição para `FAILED` | `.../infrastructure/email/EmailOutboxProcessor.java:121-136` |
| Métrica com `status=canceled` | `.../infrastructure/email/EmailOutboxProcessor.java:66` |
| Tabela, `CHECK` e índice de trabalho pendente | `backend/src/main/resources/db/migration/V6__email_outbox.sql` |
| Ampliação do `CHECK` para `CANCELED` | `backend/src/main/resources/db/migration/V7__email_outbox_canceled_status.sql` |
| Parâmetros configuráveis com validação | `.../infrastructure/config/properties/EmailOutboxProperties.java`, `application.yml:60-65` |
| Campos `emailQueued` / `emailSent` | `.../application/dto/appointment/AppointmentCommandResult.java:9-10`, `.../presentation/response/AppointmentCommandResponse.java:9-10` |
| Testes do processador (13 casos, envio fora de transação comprovado) | `backend/src/test/java/com/redekrill/scheduling/infrastructure/email/EmailOutboxProcessorTest.java` |

## Alternativas consideradas

- **Envio síncrono com `try/catch` e aviso no `warnings`** (estado anterior) — simples, mas sem retry, com latência acoplada e sem trilha de entrega.
- **Mensagem para broker externo (Rabbit/Kafka)** — resolveria escala e *claim*, mas introduz infra que o desafio não pede e que não existe neste ambiente (sem Docker disponível para subir broker).
- **Agendar por `@Async` sem persistência** — perderia a mensagem se o processo morresse entre a fila de threads e o SMTP; o requisito de "não perder notificação" pede persistência.
- **CDC / listen-notify do PostgreSQL** — elegante, porém acopla a aplicação a um recurso específico do banco e dificulta o teste com Postgres embarcado.
