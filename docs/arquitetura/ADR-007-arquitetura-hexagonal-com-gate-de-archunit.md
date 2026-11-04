# ADR-007 — Arquitetura hexagonal com gate de ArchUnit no build

**Status:** Aceita
**Data:** 2026-11-03
**Escopo:** backend (organização de pacotes e regras de dependência)

## Contexto

O backend já estava separado em quatro camadas, mas nada impedia que a disciplina se desgastasse com o prazo: `domain` começando a importar Spring, um controller chamando repositório direto, ou um use case tocando em detalhe de infraestrutura. Regra verbal ou escrito em documento não resistem a pressão de prazo; a regra precisa falhar o build.

## Decisão

1. Quatro camadas com uma única direção de dependência: `presentation` → `application` → `domain`; `infrastructure` implementa as portas de `domain`/`application` e nunca é dependido por elas.
2. DTOs compartilhados entre camadas (`ApiError`, `AuthCookieSettings`, `AuthRequestAttributes`) vivem em `application.dto` — não criam dependência cruzada.
3. As regras de dependência são **executadas por ArchUnit a cada build**, fazendo parte da suíte: violação quebra `mvnw verify` tanto local quanto no CI, não é checklist de revisão.

Em particular:

- `domain` não depende de `application`, `infrastructure`, `presentation`, nem de `org.springframework.*`/`jakarta.*`.
- `application` não depende de `infrastructure`/`presentation`.
- `presentation` não depende de `infrastructure` (controllers conversam com use cases).
- `infrastructure` não depende de `presentation`.

## Consequências

- Testar regras de negócio não exige Spring (uso cases com mock rodam puros).
- Custo de boilerplate de mapeamento explícito entre camadas; aceito por evitar o acoplamento inverso.
- Atalho silencioso é impossível: o teste de arquitetura é o primeiro a falhar.

## Evidência no código

- `backend/src/test/java/com/redekrill/scheduling/architecture/ArchitectureTest.java:16-49` — as quatro regras (`noClasses().that().resideInAPackage(..)..should().dependOnClassesThat()..`): domínio em `:18-26`, aplicação em `:29-37`, apresentação em `:40-43`, infraestrutura em `:46-49`.
- Estrutura de pacotes e o mapa de camadas em `README.md`, seção "Como o sistema foi organizado".
- `docs/arquitetura/README.md` — índice das demais decisões estruturais.

## Alternativas consideradas

- **Camada única de serviços** — recusada: mistura regra de negócio com transporte HTTP e detalhe de persistência, sem limite claro para crescimento.
- **Verificar dependências apenas no CI** — recusada: falha chega tarde; com ArchUnit na suíte local, a violação aparece no primeiro `verify`.
- **maven-enforcer com regras de pacote** — recusada para este fim: expressões de import são menos legíveis e ficam fora da suíte de testes (não entram no relatório JaCoCo/surefire).
