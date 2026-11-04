# ADR-009 — Relatório com agregações SQL no banco

**Status:** Aceita
**Data:** 2026-11-03
**Escopo:** backend (consulta de relatórios) + frontend (apresentação dos totais)

## Contexto

O relatório original paginava em memória e somava os totais sobre a lista da página. Resultado duplamente errado com crescimento: degradação de memória/tempo com o volume e **totais que mudam de valor conforme a página** — a soma era da página, não do período.

## Decisão

1. **Paginação no banco:** `searchReport(start, end, page, size)` devolve `PageResult<Appointment>` com a página pedida, mantendo os filtros de período.
2. **Totais agregados em consultas SQL dedicadas** sobre o período completo, independentes da página:
   - `countByPeriod` — quantidade de agendamentos;
   - `sumPalletsByPeriod` — pallets declarados;
   - `sumEffectivePalletsByPeriod` — pallets efetivos (multiplicador aplicado).
3. O use case valida o período (datas obrigatórias, início ≤ fim) e compõe página + totais num único resultado; o frontend exibe os totais fixos e marca cancelados com status destacado (inclusos de propósito, para transparência operacional).

## Consequências

- Totais estáveis em qualquer página; consulta extra por página com índice por data (convergência com o volume, dispensando materialização).
- Contrato de paginação (`page`/`size`/total) único entre telas.

## Evidência no código

- `backend/src/main/java/com/redekrill/scheduling/domain/repository/AppointmentRepository.java:70-84` — `searchReport` (`:70`), `countByPeriod` (`:74`), `sumPalletsByPeriod` (`:79`), `sumEffectivePalletsByPeriod` (`:84`).
- `backend/src/main/java/com/redekrill/scheduling/application/usecase/appointment/GetAppointmentReportUseCase.java:33,52,58,60` — composição da página com os três agregados.

## Alternativas consideradas

- **Somar em memória a página atual** — comportamento original; recusado: totais errados por construção.
- **Buscar tudo e paginar em memória** — recusado: escala com o volume total, não com a página.
- **Tabela agregada/materializada** — recusada nesta fase: escrita extra em cada mutação de agendamento e risco de divergência; o volume alvo não justifica.
