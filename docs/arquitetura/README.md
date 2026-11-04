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
| [ADR-007](ADR-007-arquitetura-hexagonal-com-gate-de-archunit.md) | Arquitetura hexagonal com gate de ArchUnit executado a cada build | Aceita | 2026-11-03 |
| [ADR-008](ADR-008-seed-admin-seguro-e-troca-obrigatoria.md) | Seed de administrador por variáveis, com senha conferida pela política única e troca obrigatória no primeiro acesso | Aceita | 2026-11-03 |
| [ADR-009](ADR-009-relatorio-com-agregacoes-sql.md) | Relatório paginado no banco com agregações SQL do período completo (totais estáveis em qualquer página) | Aceita | 2026-11-03 |
| [ADR-010](ADR-010-funil-de-validacao-e-gate-de-dependencias.md) | Funil de validação em um único `mvnw verify` (unitários + integração com Postgres embarcado + ArchUnit) e SCA como gate (`failBuildOnCVSS=7`) | Aceita | 2026-11-03 |

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

### Decisões registradas em 03/11 (ADR-007 a ADR-010)

- **ADR-007 fecha o ciclo do artefato** que os ADRs 001–006 já assumiam: a direção de dependência que sustenta cookies/outbox/limites é verificável por build (ArchUnit), não por revisão manual.
- **ADR-008 materializa o fechamento de C1** (credencial padrão) e de `SEC-05` (política única): a conta inicial nasce com a mesma política da troca de senha e com a trava de troca obrigatória.
- **ADR-009 registra o contrato da página de relatórios:** os totais são três agregações SQL do período completo (`countByPeriod`, `sumPalletsByPeriod`, `sumEffectivePalletsByPeriod`) — a soma não depende da página.
- **ADR-010 registra o funil de validação** que produziu as evidências do `SECURITY_REPORT.md` §11 e de `docs/VALIDACAO_FINAL.md` — inclusive o pin do webjar do Swagger UI e o controle negativo documentado.

## Riscos deliberados hoje, por ADR

| ADR | Risco aceito | Onde acompanhar |
|---|---|---|
| 001 | ~~Reuso de refresh token não revoga as demais sessões do usuário~~ **fechado em 2026-11-03** (cascata + auditoria `REFRESH_TOKEN_REUSE`; ver adendo do ADR-001) | `SECURITY_REPORT.md` → `SEC-04` |
| 001 | Lockout de login em memória, local à instância | `SEC-01` |
| 003 | Processador de outbox sem *claim* distribuído (topologia de instância única) | `SEC-06`, seção de implantação |
| 003 | Retenção indefinida de PII na tabela de outbox | `SEC-08` |
| 004 | Mudar limite exige release, não operação | — |
| 006 | Checagem de unicidade no caso de uso é susceptível a corrida (garantia vem do índice único) | — |
| 009 | Relatório inclui agendamentos cancelados por escolha (transparência operacional); tratá-los à parte é opção do front | — |
| 010 | Frescura da base do NVD depende do cache local ou da `NVD_API_KEY` no CI | `SECURITY_REPORT.md` §11 |

## Convenção para novos ADRs

1. Copiar a estrutura de qualquer arquivo deste diretório: **Status / Data / Escopo / Contexto / Decisão / Consequências / Evidência no código / Alternativas consideradas**.
2. Numerar sequencialmente (`ADR-007-...`) e adicionar linha no índice acima.
3. `Status` aceita: `Proposta`, `Aceita`, `Superada por ADR-NNN`, `Rejeitada`. Uma decisão aceita só muda de status por outro ADR que a declare superada — nunca por edição do texto original.
4. A seção **Evidência no código** é obrigatória: cada afirmação precisa apontar arquivo e linha existentes. Referência sem evidência vai para a seção de lacunas do `SECURITY_REPORT.md`.
5. Se um ADR for escrito para justificar uma migração destrutiva, o SQL deve citar o número do ADR no comentário, como `V5` faz com `ADR-004`.

**Base documental:** código e artefatos de teste/cobertura de `backend/target/` na data de 2026-11-02, atualizados em 2026-11-03 (execução `./mvnw -B -o clean verify` das 14:06 — 214 testes — e checklist de runtime de `docs/VALIDACAO_FINAL.md` §6) e `SECURITY_REPORT.md`. Re-execuções da noite de 2026-11-03: `mvnw test`/`clean verify` às 23:19–23:20 (214 testes, gate 98,85%), Dependency-Check às 23:17 (0 vulneráveis) e portas verdes do frontend — ver `SECURITY_REPORT.md` §11.
