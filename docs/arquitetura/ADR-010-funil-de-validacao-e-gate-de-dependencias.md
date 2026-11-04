# ADR-010 — Funil de validação local e gate de análise de dependências

**Status:** Aceita
**Data:** 2026-11-03
**Escopo:** backend + frontend + CI (estratégia de testes e SCA)

## Contexto

O funil anterior tinha duas lacunas: nenhuma análise de composição de software (SCA) — impossível afirmar ausência de CVE — e testes que deixavam classes de controle de acesso fora do gate. Além disso, a máquina de validação desta entrega não tem Docker, o que inviabiliza Testcontainers e qualquer stack de contêineres como pré-requisito de suíte.

## Decisão

1. **Backend — um único `mvnw verify` roda todos os níveis:**
   - Unitários por use case/serviço (Mockito + AssertJ): regras de capacidade, conflito de `orderId`, auditoria, rate limit, JWT/sessão.
   - Integração com **PostgreSQL embarcado** (Zonky via `@DynamicPropertySource`): API completa com SQL/Flyway/cookies/CSRF/revogação/outbox reais — H2 esconde incompatibilidade exatamente do que o projeto depende.
   - Arquitetura: ArchUnit (ADR-007) na mesma suíte.
   - **Gate de cobertura JaCoCo** por agregado dos pacotes críticos (`domain.policy`, `usecase.appointment`, `usecase.auth`, `application.service`), exigência de 80% em linhas; medição por classe permanece visível no relatório para não repetir o achado `SEC-02`.
2. **SCA como gate explicitado:** perfil `security` do Maven com OWASP Dependency-Check fixado na versão 12.2.2 e `failBuildOnCVSS=7` — o build **falha** se aparecer vulnerabilidade com CVSS ≥ 7. Execuções manuais nesta máquina usam o cache local do NVD (sem `NVD_API_KEY`); no CI a varredura roda agendada com a chave. A versão do webjar do Swagger UI é fixada (`5.34.1`, que embarca DOMPurify 3.4.17) e o pom documenta o motivo e o acoplamento com `application.yml`.
3. **Frontend:** Vitest + Vue Test Utils (unitários), `vue-tsc` (typecheck), ESLint/Prettier, build Vite com code splitting e **E2E Playwright com API mockada** para o fluxo crítico de autenticação — roda nesta máquina e no CI sem backend nem Docker. A integração HTTP real de ponta a ponta fica explicitamente listada como lacuna (`docs/VALIDACAO_FINAL.md` §8, item 5), com o smoke de runtime a cobri-la na sessão de validação.
4. **`npm audit` com `overrides`, não com `audit fix --force`:** as 12 vulnerabilidades moderadas eram da cadeia de dev-deps (`glob`/`minimatch` ⟶ `@vue/test-utils`/`eslint`); o override fixa versões seguras sem subir majors de ferramentas (`glob ^14.0.2`, `minimatch ^11.0.1`).

## Consequências

- Feedback realista de SQL/cookies/revogação localmente, sem Docker.
- A E2E mockada não valida integração HTTP real — limitação declarada, não escondida.
- A frescura da base do NVD depende de cache local ou chave no CI; ressalva registrada em `SECURITY_REPORT.md` §11.
- Gate de cobertura por agregado (não por classe) — a medida por classe fica no relatório JaCoCo e os achados de classe são rastreados no relatório de segurança.

## Evidência no código

- `backend/pom.xml:188-205` — regra JaCoCo com os quatro `include` do gate e exigência mínima de linhas.
- `backend/pom.xml:214-234` — perfil `security` com `dependency-check-maven` 12.2.2 e `failBuildOnCVSS=7`.
- `backend/pom.xml:28-32` — pin do webjar `swagger-ui.version` 5.34.1 e comentário das CVEs do DOMPurify 3.4.16.
- `frontend/package.json:49-52` — `overrides` de `glob`/`minimatch`.
- `backend/src/test/java/com/redekrill/scheduling/integration/SchedulingApiIntegrationTest.java` — integração com Postgres embarcado.
- `frontend/e2e/auth.spec.js` — os três testes E2E Playwright.
- `.github/workflows/ci.yml` — jobs backend/frontend/infra + varredura de dependências agendada.

## Alternativas consideradas

- **H2 no lugar do Postgres embarcado** — recusado: esconde exatamente as incompatibilidades de SQL/Flyway que a suíte existe para pegar.
- **Testcontainers** — recusado nesta máquina: exige Docker daemon indisponível; Postgres embarcado cumpre o papel sem pré-requisito de container.
- **`npm audit fix --force`** — recusado: teria instalado majors quebrando toolchain (documentado no ADR original de testes e na trilha de comandos).
- **Gate por classe com 80% individual** — mantido como sugestão (relatório de segurança, item P1), não como regra grave: o custo de cobertura artificial cresce rápido; a métrica por classe segue publicada.
