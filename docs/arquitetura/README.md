# Decisões de arquitetura (ADRs) — Rede Krill Scheduling

Registram aqui apenas as decisões **difíceis de reverter** e **não óbvias por leitura do código**: alternativas recusadas, trade-offs aceitos e riscos deliberados. Convenção de formatação, nome de pacote e estilo de código não pertencem a este diretório — estão no `SECURITY_REPORT.md`, no `README.md` e nos linters do projeto.

## Índice

| ID | Decisão | Status | Data |
|---|---|---|---|
| [ADR-001](ADR-001-sessao-com-refresh-token-em-cookie-httponly.md) | Sessão com access token de 15 min + refresh rotativo de uso único, ambos em cookie `HttpOnly`; claim `sid` como binding de revogação | Aceita | 2026-11-02 |
| [ADR-002](ADR-002-csrf-com-cookie-token-repository.md) | CSRF habilitado com token em cookie legível ao SPA; exceção apenas para requisições com header `Authorization: Bearer` | Aceita | 2026-11-02 |
| [ADR-003](ADR-003-outbox-transacional-de-email.md) | Notificação por e-mail via outbox transacional com retry exponencial; contrato separado em `emailQueued` e `emailSent` | Aceita | 2026-11-02 |
| [ADR-004](ADR-004-limites-fixos-de-capacidade.md) | Limites de capacidade (480/dia, 120/faixa) como constantes de domínio; remoção da tabela de configuração que nunca foi lida | Aceita | 2026-11-02 |
| [ADR-005](ADR-005-flyway-como-dono-do-schema.md) | Flyway como dono exclusivo do schema; Hibernate em `validate`; migrações exercitadas por PostgreSQL embarcado nos testes | Aceita | 2026-11-02 |
| [ADR-006](ADR-006-concorrencia-e-lock-otimista.md) | Unicidade de ID Pedido em três camadas com `409` padronizado; lock otimista por `version` contra *lost update* | Aceita | 2026-11-02 |

## Como este conjunto se relaciona

```
ADR-001 (sessão em cookie) ──impõe──> ADR-002 (CSRF obrigatório)
        │                                      │
        └── auditoria por sessão ──> ADR-006 (integridade/concorrência)

ADR-005 (schema versionado) ──transporta──> ADR-004 (limites no domínio)
                                        └──> ADR-003 (tabela de outbox)
```

- **ADR-001 é a causa do ADR-002.** Trocar `localStorage` por cookie `HttpOnly` cria a necessidade de proteção CSRF; quem ler só um dos dois vai entender errado a configuração.
- **ADR-004 é referenciado por código.** O comentário de `V5__security_sessions_and_integrity.sql:29-32` cita explicitamente este ADR como justificativa de `DROP TABLE capacity_settings`.
- **ADR-003 alterou contrato público** (`emailQueued` × `emailSent`), portanto o README e a documentação de API devem espelhá-lo.

## Riscos deliberados hoje, por ADR

| ADR | Risco aceito | Onde acompanhar |
|---|---|---|
| 001 | Reuso de refresh token não revoga as demais sessões do usuário | `SECURITY_REPORT.md` → `SEC-04` |
| 001 | Lockout de login em memória, local à instância | `SEC-01` |
| 003 | Processador de outbox sem *claim* distribuído (topologia de instância única) | `SEC-06`, seção de implantação |
| 003 | Retenção indefinida de PII na tabela de outbox | `SEC-08` |
| 004 | Mudar limite exige release, não operação | — |
| 006 | Checagem de unicidade no caso de uso é susceptível a corrida (garantia vem do índice único) | — |

## Convenção para novos ADRs

1. Copiar a estrutura de qualquer arquivo deste diretório: **Status / Data / Escopo / Contexto / Decisão / Consequências / Evidência no código / Alternativas consideradas**.
2. Numerar sequencialmente (`ADR-007-...`) e adicionar linha no índice acima.
3. `Status` aceita: `Proposta`, `Aceita`, `Superada por ADR-NNN`, `Rejeitada`. Uma decisão aceita só muda de status por outro ADR que a declare superada — nunca por edição do texto original.
4. A seção **Evidência no código** é obrigatória: cada afirmação precisa apontar arquivo e linha existentes. Referência sem evidência vai para a seção de lacunas do `SECURITY_REPORT.md`.
5. Se um ADR for escrito para justificar uma migração destrutiva, o SQL deve citar o número do ADR no comentário, como `V5` faz com `ADR-004`.

**Base documental:** código na data de 2026-11-02, artefatos de teste/cobertura de `backend/target/` (execução das 22:20) e `SECURITY_REPORT.md`.
