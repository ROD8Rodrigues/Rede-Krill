# ADR-004 — Limites de capacidade como regra fixa de domínio (480/dia, 120/faixa)

**Status:** Aceita
**Data:** 2026-11-02
**Escopo:** backend (`domain/policy/CapacityPolicy`), banco (migrações `V4` e `V5`), API de ocupação
**Referência externa:** este ADR é o citado no comentário de `V5__security_sessions_and_integrity.sql:29-32`.

---

## Contexto

O desafio define dois limites de acompanhamento da capacidade do centro de distribuição: **480 pallets por dia** e **120 pallets por faixa horária**. Eles servem para *monitorar* ocupação, **não** para bloquear agendamentos: quando um limite é ultrapassado, o sistema devolve `warnings` e mantém o registro.

Em `V4__administrative_configuration.sql` foi criada a tabela `capacity_settings` (singleton `id = 1`, valores 480/120, `CHECK` entre 1 e 1.000.000) com a intenção de permitir ajuste pelo ADMIN. Ao implementar as telas administrativas, descobriu-se que **nenhuma linha de código nunca leu essa tabela** — os números usados no cálculo estavam hard-coded em `CapacityPolicy`. Ou seja: existia uma configuração aparente que não surtiria efeito, o pior tipo de configuração (ela sugere um comportamento que não entrega).

## Decisão

Tratar os limites como **constantes de domínio**, não como configuração externa:

```
public static final int DAILY_LIMIT = 480;
public static final int TIME_SLOT_LIMIT = 120;
```

- Remover `capacity_settings` em `V5` (`DROP TABLE IF EXISTS capacity_settings`), eliminando a superfície de configuração ilusória.
- Manter o cálculo em um único ponto: `CapacityPolicy.assess(dailyAntes, slotAntes, novoDaily, novoSlot)` retorna `CapacityAssessment` com `dailyTotal`, `slotTotal`, limites e a lista de `warnings` formatados com o excesso.
- Distinguir as duas grandezas que alimentam os dois limites:
  - **limite diário** usa `dailyPallets` (quantidade declarada no cadastro);
  - **limite da faixa** usa `effectivePallets`/`slotPallets` (quantidade com o multiplicador do tipo de paletização — não paletizado multiplica por 2).
- Agendamentos com `status = CANCELED` ficam fora de qualquer soma.
- Exposição em `GET /api/appointments/capacity?date=YYYY-MM-DD` com ocupação percentual arredondada em uma casa decimal, por dia e por faixa.
- Ocupação percentual também é recalculada em tempo real no formulário de agendamento (o operador vê o efeito do lançamento antes de confirmar).

## Consequências

**Positivas**
- Uma única fonte da verdade para os limites; o valor que está no código é o valor em efeito.
- Regra de negócio testável sem banco e sem Spring (`CapacityPolicyTest`, 9 casos, 100% de cobertura de linhas).
- Remove a ambição de "configuração" que exigiria UI, validação, auditoria de mudança e testes de integração próprios.

**Negativas / custo**
- Mudar um limite passa a exigir **release**, não operação. Aceito: o número vem do enunciado do desafio e não há pedido real de ajustá-lo em runtime.
- A remoção da tabela é destrutiva. `DROP TABLE` não é reversível por dados — se a decisão for revertida, é preciso uma migração nova recriando a tabela e o `INSERT` do singleton (o histórico de valores nunca existiu, então não há perda de informação).
- O `V4` permanece no histórico de migrações criando algo que o `V5` apaga; em banco já migrado isso é inofensivo, mas exige que ninguém tente "consertar" o `V4`.
- Se um dia os limites virarem configuração real, o caminho é o oposto do atual: `CapacityPolicy` precisa receber os valores por parâmetro (port/propriedade) e os testes precisam cobrir ambos os casos.

## Evidência no código

| Item | Arquivo |
|---|---|
| Constantes e cálculo | `backend/src/main/java/com/redekrill/scheduling/domain/policy/CapacityPolicy.java:17-46` |
| Mensagens de aviso com excesso | `.../domain/policy/CapacityPolicy.java:36-42` |
| Criação do singleton (histórico) | `backend/src/main/resources/db/migration/V4__administrative_configuration.sql:1-20` |
| Remoção com justificativa | `backend/src/main/resources/db/migration/V5__security_sessions_and_integrity.sql:29-32` |
| Consulta de ocupação | `.../presentation/controller/AppointmentController.java:83-86` |
| Testes de política | `backend/src/test/java/com/redekrill/scheduling/domain/policy/CapacityPolicyTest.java` (9 casos, cobertura 100%) |

## Alternativas consideradas

- **Manter `capacity_settings` e implementar a leitura** — exigiria UI de configuração, validação, auditoria de alteração e um plano para consistência de ocupação histórica quando o limite muda. Custo sem demanda.
- **Limites em `application.yml`** — mais barato que tabela, mas criaria o mesmo tipo de problema: um número fora do domínio que o `CapacityPolicy` ignoraria, e que mudaria por ambiente sem controle de versão.
- **Derivar o limite do tipo de caminhão** — conflituaria com o enunciado: a capacidade do caminhão diz respeito ao transporte; os 480/120 são do cais de recebimento.
