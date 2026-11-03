# Auditoria Técnica — Rede Krill
## Sistema de Agendamento de Caminhões

| Item | Informação |
|---|---|
| Repositório auditado | `https://github.com/ROD8Rodrigues/Rede-Krill.git` (público) |
| Caminho local | `G:\Christopher Chanquet\rede-krill-scheduling` |
| Commit auditado | `06cee93` (`main`), 6 commits, árvore Git limpa antes deste relatório |
| Data da auditoria | 02/11/2026 |
| Documento de referência | PDF “Desafio Dev Rede Krill”, 5 páginas |
| Natureza | Auditoria técnica somente leitura |
| Escopo | Backend, frontend, banco e migrations, testes, segurança, Docker, README e entrega |
| Alterações realizadas | Nenhuma no projeto; este relatório é o único arquivo criado |

> **Método e limitações.** A auditoria combinou leitura integral do PDF, inspeção estática do código, conferência das migrations e dos artefatos de build existentes, consulta aos relatórios Surefire/JaCoCo já gerados e execução de `npm audit` em modo somente leitura. Não reexecutei Maven (para não escrever em `backend/target`), não subi o Docker Compose, não executei o frontend, não enviei e-mail real e não realizei teste de intrusão. Portanto, os números de testes e cobertura vêm dos relatórios existentes, e a validação de Docker, Swagger, envio de e-mail e frontend é estática ou baseada em testes já registrados. Esta auditoria não substitui um pentest nem um teste de carga.

**Legenda de status usada na matriz**

- **Atende**: há implementação e evidência suficientes para o requisito.
- **Parcial**: há implementação, mas falta parte do requisito, integração, validação ponta a ponta ou evidência.
- **Não atende**: requisito ausente, incompleto de forma objetiva ou contradito pela implementação.

---

## A. Resumo Executivo

O projeto implementa bem o núcleo de negócio do desafio. O fluxo de agendamento existe de ponta a ponta: cadastro, consulta, edição, cancelamento lógico, controle de capacidade diária e por faixa, cálculo de paletização, relatórios por período, perfis ADMIN/USER, auditoria e Swagger. As regras mais sensíveis estão no domínio e não apenas na interface: os limites de 480 pallets por dia e 120 por faixa são informativos, o cadastro não é bloqueado quando ultrapassa os limites, a dobra de “Não Paletizado” é aplicada somente na faixa horária e os cancelados saem da capacidade sem desaparecer do histórico.

O backend é a parte mais madura. A suíte existente tem **122 testes** (118 métodos `@Test` + 4 regras ArchUnit), com **0 falhas, 0 erros e 0 ignorados**. A cobertura de linhas nos pacotes críticos definidos no `pom.xml` é de **100% (294/294)**, com **87% de branches (87/100)** nesses pacotes. A cobertura geral é de **90,79% de linhas (1271/1400)** e **79,58% de branches (191/240)**. O gate de 80% de linhas nos domínios críticos está configurado e é superado.

A entrega, porém, **não está integralmente conforme**. Há três bloqueios principais:

1. **Segredos e credenciais padrão versionados em repositório público**: segredo JWT simétrico padrão, senha padrão do administrador e credenciais padrão do PostgreSQL aparecem em `application.yml`, `.env.example` e `docker-compose.yml`. Como o repositório é público, uma implantação que não sobrescreva esses valores permite forjar tokens administrativos ou entrar com o usuário inicial.
2. **Lacuna funcional obrigatória**: o PDF exige tela de cadastro para todos os itens de controle, mas não existem telas nem endpoints CRUD para faixas horárias e tipos de paletização. A tabela `capacity_settings` também existe, mas não é lida por nenhum código Java.
3. **Entrega incompleta**: não há PDF de entrega, vídeo demonstrativo nem relatório de segurança no repositório, itens explicitamente exigidos na página 5 do desafio. O README também não justifica a escolha do PostgreSQL nem as decisões arquiteturais.

Além disso, a ocupação percentual existe na tela de agendamentos, mas em um painel separado do modal de cadastro. Alterar a data ou a faixa dentro do formulário não atualiza o painel antes de salvar, o que atende apenas parcialmente ao requisito de exibir a ocupação por porcentagem junto dos campos de Data e Faixa Horária.

**Veredito sintético:** núcleo funcional sólido e bem testado, mas **não aprovado para entrega integral no estado atual**. É aprovável para demonstração local controlada, desde que os segredos sejam sobrescritos e o ambiente não fique exposto. A correção dos bloqueios é viável e não exige reescrita.

### Visão por dimensão

| Dimensão | Situação | Síntese |
|---|---|---|
| Regras de negócio | Forte | 480/120, quatro faixas de 2h, dobra na faixa, cancelamento lógico, relatórios e permissões implementados |
| Escopo e telas | Parcial | Sem CRUD de faixas horárias e tipos de paletização; ocupação não integrada ao modal |
| Testes de backend | Forte | 122 testes, 0 falhas; 100% de linhas nos pacotes críticos |
| Testes de frontend | Ausente | Sem Vitest, Playwright, ESLint ou typecheck |
| Segurança | Crítica | Segredos/credenciais padrão, JWT em `localStorage`, sem revogação, sem rate limiting |
| Infraestrutura | Adequada com ressalvas | Docker multi-stage e Compose completos, mas não validados em execução; Spring Boot 3.3.5 fora do suporte OSS |
| Documentação | Parcial | README detalhado, mas sem justificativa de banco, sem V4 e com árvore de frontend desatualizada |
| Entrega | Incompleta | Sem PDF, vídeo ou relatório de segurança |

### Principais achados

- **C1 (crítico)** — Segredos e credenciais padrão em repositório público.
- **C2 (crítico)** — PDF, vídeo e relatório de segurança ausentes.
- **C3 (crítico de conformidade)** — Sem telas de cadastro para faixas horárias e tipos de paletização.
- **C4 (alto)** — Ocupação percentual não integrada ao formulário de agendamento.
- **C5 (alto)** — Spring Boot 3.3.5 fora do suporte OSS e ausência de análise de dependências.
- **C6 (alto)** — `UpdateUserRequest.active` é `boolean` primitivo e pode desativar usuário silenciosamente.
- **C7 (alto)** — Lacunas na auditoria: login inválido, 401 e `ACCESS_DENIED` anônimo não são registrados corretamente.
- **C8 (média/alta)** — Corrida de `orderId` pode retornar 500 e edições concorrentes podem se perder.

**Recomendação:** bloquear a entrega até a conclusão do P0 do plano (Seção H). Depois disso, o projeto pode ser aprovado com as ressalvas do P1.

---

## B. Matriz de Conformidade

### B.1 Escopo funcional obrigatório

| ID | Requisito do PDF | Evidência na implementação | Status | Observação |
|---|---|---|---|---|
| B1 | Separar frontend e backend; comunicação exclusivamente por APIs REST | `frontend/` em Vue 3 consumindo `/api` via Axios (`frontend/src/services/api.js:6-11`); `backend/` Spring MVC com controllers REST; sem renderização server-side | **Atende** | A separação é real; o frontend é SPA e o backend expõe JSON. |
| B2 | Cadastrar, consultar, acompanhar e cancelar agendamentos | `AppointmentController` com GET, GET por ID, POST, PUT e DELETE; `AppointmentsView.vue`; `CancelAppointmentUseCase` com cancelamento lógico | **Atende** | O cancelamento preserva histórico e não apaga o registro. |
| B3 | Visibilidade da ocupação por dia e faixa horária | `GET /api/appointments/capacity`; `GetCapacityOverviewUseCase`; painel em `AppointmentsView.vue:56-139` | **Atende** | A visão geral existe; a integração com o formulário é parcial (B10/B11). |
| B4 | Controle por dia: 480 pallets, sem bloquear, apenas notificar o excesso | `CapacityPolicy.DAILY_LIMIT = 480` (`CapacityPolicy.java:17`); aviso quando `dailyTotal > 480`; `CreateAppointmentUseCase` salva mesmo com aviso | **Atende** | O limite é informativo; não há constraint de banco que bloqueie 480+. |
| B5 | Controle por faixa: 120 pallets, sem bloquear, apenas notificar o excesso | `CapacityPolicy.TIME_SLOT_LIMIT = 120` (`CapacityPolicy.java:18`); somas por faixa em `AppointmentJpaRepository.java:56-74`; aviso quando `slotTotal > 120` | **Atende** | A faixa usa `effectivePallets`; o dia usa `palletQuantity`. |
| B6 | Faixa de horário: 4 faixas de 2 horas | `V2__reference_data.sql:7-11` cria 08:00–10:00, 10:00–12:00, 13:00–15:00 e 15:00–17:00; `V4__administrative_configuration.sql:23-30` exige exatamente 2h | **Atende** | A constraint da V4 reforça a regra no banco. |
| B7 | Tipo de paletização: Paletizado e Não Paletizado; dobrar a quantidade na faixa horária no segundo caso | `V2__reference_data.sql:1-3` define multiplicadores 1 e 2; `Appointment.dailyPallets()` retorna `palletQuantity` e `slotPallets()` retorna `effectivePallets`; o cálculo aplica o multiplicador somente na faixa | **Atende** | A lógica está no domínio e é usada nas somas de capacidade. |
| B8 | Tipo de caminhão: truck, carreta e van | `V2__reference_data.sql:15-18` cria Carreta, Truck e VUC; `V3__appointment_conformity.sql:68-73` adiciona Van; CRUD em `TruckTypeController` e `TruckTypesView.vue` | **Atende** | VUC é um tipo extra; os três exigidos estão presentes. |
| B9 | Todos os itens devem conter tela para cadastro | Telas/rotas para agendamentos, tipos de caminhão, usuários, relatórios e auditoria; `ReferenceDataController` só tem GET para faixas e paletização; não há endpoints CRUD para `time_slot` ou `palletization_type`; `capacity_settings` não é usado | **Parcial** | Lacuna objetiva: faixas horárias e tipos de paletização são somente seed/leitura. O próprio README lista isso como pendência (`README.md:568`). |
| B10 | Cadastro com Data exibindo ocupação por porcentagem em tela | Campo de data em `AppointmentsView.vue:360-371`; painel de ocupação separado em `AppointmentsView.vue:56-139`; `openCreateModal` não sincroniza `capacityDate` (`:730-745`); não há `watch` de `form.date` | **Parcial** | O painel existe, mas não acompanha a data escolhida no modal; a sincronização automática ocorre apenas após salvar (`:798-799`). |
| B11 | Cadastro com Faixa Horária exibindo ocupação por porcentagem em tela | Select de faixa em `AppointmentsView.vue:435-448`; painel de faixas em `:108-134`; não há atualização ao trocar `form.timeSlotId` | **Parcial** | O percentual por faixa existe no painel, mas não ao lado do campo nem em tempo real no formulário. |
| B12 | ID Pedido numérico, obrigatório e único | Validação de domínio `Appointment.java:37-38`; `V3__appointment_conformity.sql:39-46` com `NOT NULL`, índice único e `CHECK (order_id > 0)`; verificação de duplicidade no use case | **Atende** | Há uma condição de corrida possível; ver D1/C8. |
| B13 | Fornecedor com texto de 80 caracteres, obrigatório | `Appointment.java:39-43`; `CreateAppointmentRequest` com `@Size(max = 80)`; `V3__appointment_conformity.sql:60-62` com `CHECK ... NOT VALID` | **Atende** | Legado acima de 80 é preservado; novos registros são limitados. |
| B14 | E-mail do fornecedor com 80 caracteres, obrigatório | `Appointment.java:44-48`; request com `@Email` e `@Size(max = 80)`; `V3__appointment_conformity.sql:64-66` | **Atende** | O e-mail é validado no domínio e na borda. |
| B15 | Tipo de caminhão e tipo de paletização obrigatórios | `CreateAppointmentRequest` com `@NotNull`; `CreateAppointmentUseCase.java:169-171` valida a presença dos IDs | **Atende** | Também há validação de existência e de registros ativos. |
| B16 | Observação opcional | Campo `notes` no request e no domínio; `V3__appointment_conformity.sql:14` cria `notes VARCHAR(500)`; limite de 500 no domínio/request | **Atende** | O campo é opcional e o limite é consistente. |
| B17 | Após o cadastro, enviar e-mail ao fornecedor com as informações do agendamento | `CreateAppointmentUseCase.java:130-156` envia após a transação; `SmtpEmailNotifier` monta e-mail com Thymeleaf (`appointment-created.html`); falha retorna `emailSent: false` e aviso, sem desfazer o cadastro | **Parcial** | A implementação existe, mas o teste de integração mocka `EmailNotifier`; `SmtpEmailNotifier` tem 0% de cobertura e o envio real não foi validado. |
| B18 | Relatório para busca de agendamentos com filtros por período | `ReportController` em `/api/reports/appointments`; `GetAppointmentReportUseCase`; `ReportsView.vue` com data inicial/final e atalhos de período | **Atende** | O relatório inclui cancelados no histórico e os exclui da capacidade. |
| B19 | Permissões: Administrador com acesso total; Usuário Comum apenas visualização | `SecurityConfig.java:48-55`: `/api/users/**` e `/api/audit-logs/**` exigem ADMIN; POST/PUT/DELETE de agendamentos e tipos de caminhão exigem ADMIN; `anyRequest().authenticated()`; frontend esconde ações administrativas | **Atende** | O backend é a barreira efetiva; o frontend apenas complementa a UX. |
| B20 | Tela exclusiva de auditoria/log de acesso para o Administrador | `AuditLogController` e `AuditView.vue` com rota `adminOnly`; ações CREATE, UPDATE, DELETE, CANCEL, LOGIN, LOGOUT e ACCESS_DENIED registradas | **Parcial** | A tela é exclusiva e as mutações são registradas, mas login inválido, 401 e leituras não entram na trilha; o caso anônimo de ACCESS_DENIED falha (C7). |

### B.2 Requisitos técnicos e de entrega

| ID | Requisito do PDF | Evidência na implementação | Status | Observação |
|---|---|---|---|---|
| T1 | Backend Java 17+ com Spring Boot | `pom.xml:10` com Spring Boot 3.3.5; `pom.xml:21` com Java 21 | **Atende** | A versão atende ao requisito, mas 3.3.5 está fora do suporte OSS (C5). |
| T2 | Frontend Vue.js 3 com Tailwind CSS e PrimeVue ou equivalente | `frontend/package.json` com Vue 3.5, Vite 5.4, Tailwind 3.4 e PrimeVue 4.2 | **Atende** | A stack é coerente com o pedido. |
| T3 | Banco de dados livre, com justificativa da escolha e das decisões arquiteturais | PostgreSQL 16 em `docker-compose.yml` e nas migrations; README descreve a estrutura, mas não justifica a escolha nem os trade-offs | **Não atende** | O PDF exige justificativa explícita; não há seção de decisão arquitetural. |
| T4 | API REST documentada via Swagger ou similar | `springdoc-openapi-starter-webmvc-ui` 2.6.0 (`pom.xml:84-88`); anotações `@Tag`, `@Operation` e `@SecurityRequirement`; endpoints públicos `/swagger-ui.html` e `/v3/api-docs` | **Atende** | A documentação existe no código; não foi aberta em runtime nesta auditoria. |
| T5 | Dockerfile otimizado para build e execução | `backend/Dockerfile` multi-stage (Maven + JRE Alpine, usuário não-root); `frontend/Dockerfile` multi-stage (Node + Nginx); `.dockerignore` em ambos | **Atende** | O backend é non-root; o Nginx do frontend roda como root e não há healthcheck. |
| T6 | `docker-compose.yml` para orquestrar o ambiente completo | `docker-compose.yml` com PostgreSQL 16, MailHog, backend e frontend; volume persistente; healthcheck do PostgreSQL | **Atende** | Estrutura completa, mas não executada nesta auditoria; backend/frontend não têm healthcheck. |
| T7 | Testes automatizados com cobertura mínima de 80% nos domínios críticos | 122 testes, 0 falhas; gate JaCoCo de 80% de linhas nos pacotes críticos (`pom.xml:164-184`); cobertura apurada de 100% de linhas e 87% de branches nesses pacotes | **Atende** | O gate cobre linhas, não branches; a cobertura de branches ainda é 87% nos críticos. |
| T8 | Repositório público no GitHub com histórico coerente de commits | Remoto público; `git ls-remote` retorna `refs/heads/main` em `06cee93`; 6 commits com mensagens descritivas; árvore limpa antes deste relatório | **Atende** | O histórico é curto, mas coerente e rastreável. |
| T9 | Clean Architecture (diferencial) | Separação `domain`, `application`, `infrastructure` e `presentation`; `ArchitectureTest` com 4 regras ArchUnit, incluindo domínio sem dependência de Spring/Jakarta | **Atende** | É um diferencial real, não apenas pastas com nomes arquiteturais. |
| T10 | README explicativo para execução local simples | `README.md` com requisitos, Docker, desenvolvimento, endpoints, regras, segurança, testes e melhorias pendentes | **Parcial** | Falta justificativa do banco, a V4 não aparece e a árvore do frontend está desatualizada (lista só 3 views). |
| T11 | Documento PDF de entrega com link do código, instruções e justificativas | Nenhum PDF de entrega no repositório; o README funciona como documentação, mas não substitui o documento pedido | **Não atende** | A página 5 do PDF é explícita quanto ao arquivo PDF de entrega. |
| T12 | Vídeo de demonstração de até 15 minutos | Nenhum arquivo de vídeo ou link de vídeo no repositório | **Não atende** | O README lista “vídeo e PDF final” como pendência (`README.md:569`). |
| T13 | Relatório resumido de segurança com análise de vulnerabilidades | Não há `SECURITY*.md`, relatório OWASP Dependency-Check, SonarQube ou análise estática equivalente | **Não atende** | Também não há plugin OWASP/Sonar/SpotBugs no `pom.xml`. |

### B.3 Resultado da matriz

| Status | Quantidade |
|---|---:|
| Atende | 23 |
| Parcial | 6 |
| Não atende | 4 |
| **Total** | **33** |

Os itens “Parcial” e “Não atende” estão detalhados nas Seções C, D e F. A matriz mostra que o problema não é o núcleo funcional: é a combinação de lacunas de escopo, segurança e entrega.

---

## C. Problemas Críticos

### C1. Segredos e credenciais padrão versionados em repositório público

**Severidade:** Crítica

**Evidências:**
- `backend/src/main/resources/application.yml:39` — segredo JWT simétrico padrão.
- `backend/src/main/resources/application.yml:48` — senha padrão do administrador inicial.
- `docker-compose.yml:42` e `:46` — os mesmos valores como fallback do Compose.
- `.env.example:7` e `:12` — segredo JWT e senha do admin expostos como exemplo “pronto para uso”.
- `backend/src/main/java/com/redekrill/scheduling/infrastructure/seed/DataInitializer.java:22,56-58` — senha padrão em constante e apenas `log.warn` quando ela está em uso.
- Repositório público e e-mail administrativo padrão documentado no README.

**O que foi encontrado:** o projeto funciona “sem configuração” porque fornece valores padrão para o segredo JWT, para a senha do administrador e para as credenciais do PostgreSQL. Em um repositório público, esses valores deixam de ser segredo. O `DataInitializer` cria o administrador com a senha padrão se `SEED_ADMIN_PASSWORD` não for informado e apenas registra um aviso no log; não força troca de senha. O README alerta para não usar essas credenciais em produção, mas o alerta não impede que o Compose seja iniciado com os fallbacks.

**Cadeia de exploração:** um atacante com acesso ao repositório conhece o segredo JWT. Como o `JwtAuthenticationFilter` valida a assinatura e depois carrega o usuário pelo e-mail contido no token (`JwtAuthenticationFilter.java:41-44`), é possível assinar um token com o e-mail do administrador padrão e obter autorização ADMIN, desde que o administrador exista e esteja ativo. Alternativamente, se a senha padrão não tiver sido trocada, o login direto com as credenciais documentadas já entrega o mesmo acesso. O impacto é comprometimento total: cadastro, edição, cancelamento, gestão de usuários e auditoria.

**Por que é crítico:** não depende de uma falha complexa. Basta uma implantação de demonstração ou produção que não sobrescreva as variáveis. O `docker compose up` sem `.env` usa os fallbacks. O README e o `.env.example` tornam o caminho ainda mais direto.

**Correção necessária:**
1. Remover todos os valores padrão de segredos e senhas do código, do Compose e do `.env.example`.
2. Falhar a inicialização com mensagem clara se `JWT_SECRET`, `SEED_ADMIN_PASSWORD` ou credenciais de banco não forem informados em perfis que não sejam explicitamente de desenvolvimento.
3. Gerar segredo JWT aleatório de pelo menos 256 bits e documentar o comando de geração; manter o segredo apenas em variável de ambiente/secret manager.
4. Exigir troca de senha no primeiro login do admin ou criar o admin apenas em perfil de desenvolvimento, com senha aleatória impressa uma única vez.
5. Rotacionar o segredo e a senha já expostos; remover os valores do histórico do Git com `git filter-repo`/BFG, pois apagar apenas do commit atual não resolve o vazamento.
6. Adicionar um teste/checagem de startup que rejeite segredo conhecido ou senha padrão.

**Critério de aceite:** o repositório não contém segredo nem senha utilizável; `docker compose up` sem variáveis falha com orientação clara; o admin inicial não nasce com senha conhecida; o teste de startup rejeita valores padrão.

### C2. PDF, vídeo e relatório de segurança ausentes

**Severidade:** Crítica (bloqueio de entrega)

**Evidências:**
- A página 5 do PDF exige um arquivo PDF com link do código, vídeo de demonstração de até 15 minutos, instruções de execução, justificativa do banco/decisões arquiteturais e relatório de segurança.
- A varredura da raiz e do repositório não encontrou `*.pdf`, `*.mp4`, `*.mov`, `SECURITY*.md` ou `RELATORIO*.md` fora de `node_modules`/`target`.
- O próprio README lista “Concluir a documentação de entrega, o vídeo e o PDF final” como pendência (`README.md:569`).

**O que foi encontrado:** o README é uma boa documentação técnica, mas não é o documento de entrega pedido. Não há vídeo, não há PDF e não há relatório de segurança. A justificativa do banco também não existe (T3).

**Impacto:** mesmo que o código esteja correto, a entrega pode ser considerada incompleta por critério objetivo do desafio. É um bloqueio independente da qualidade do software.

**Correção necessária:**
1. Criar o PDF de entrega com link do repositório, instruções de execução local, justificativa do PostgreSQL, decisões arquiteturais, cobertura de testes e limitações.
2. Gravar vídeo de até 15 minutos mostrando arquitetura, estrutura do código e o sistema funcionando: login, cadastro, aviso de capacidade, edição, cancelamento, relatório, auditoria e Swagger.
3. Executar OWASP Dependency-Check ou SonarQube e anexar um relatório resumido de segurança, com achados, severidade e mitigações.
4. Linkar PDF, vídeo e relatório no README e no próprio PDF.

**Critério de aceite:** PDF, vídeo e relatório de segurança acessíveis por links no README/PDF; vídeo com duração ≤ 15 minutos; relatório com ferramenta, data, escopo e achados.

### C3. Sem telas de cadastro para faixas horárias e tipos de paletização

**Severidade:** Crítica de conformidade

**Evidências:**
- O PDF exige “todos os itens abaixo devem conter uma tela para cadastro” e lista controle por dia, controle por faixa, faixa de horário, tipo de paletização e tipo de caminhão.
- `frontend/src/router/index.js:12-69` registra apenas login, dashboard, agendamentos, relatórios, tipos de caminhão, usuários e auditoria.
- `frontend/src/layouts/AppLayout.vue:117-124` não tem itens de navegação para faixas horárias ou tipos de paletização.
- `ReferenceDataController` expõe somente `GET /api/time-slots` e `GET /api/palletization-types`; não há POST/PUT/DELETE.
- `V4__administrative_configuration.sql:6-19` cria `capacity_settings`, mas nenhum código Java lê ou escreve nessa tabela.
- O README reconhece a pendência: “Definir se faixas horárias e tipos de paletização terão telas de cadastro próprias” (`README.md:568`).

**O que foi encontrado:** faixas horárias e tipos de paletização existem apenas como dados de seed e consulta. O administrador não consegue cadastrar, editar ou excluir uma faixa ou um tipo de paletização pela interface. Os limites de capacidade também são fixos no código (`CapacityPolicy`), não configuráveis pela tabela `capacity_settings`.

**Impacto:** o requisito de “tela para cadastro de todos os itens” não é atendido de forma integral. É uma lacuna funcional objetiva, não apenas uma melhoria de UX.

**Correção necessária:**
1. Criar endpoints administrativos CRUD para `time_slot` e `palletization_type`, com validações: faixa de exatamente 2h, início anterior ao fim, quatro faixas, unicidade de início, multiplicadores 1 e 2 protegidos para os tipos obrigatórios.
2. Criar telas Vue administrativas com listagem, formulário, edição, exclusão protegida por uso e feedback de erro.
3. Decidir o destino de `capacity_settings`: usar na configuração dos limites ou remover a tabela e documentar que os limites são fixos por regra de negócio. Se usar, criar tela e endpoint; se remover, eliminar a migration ou justificar a existência.
4. Adicionar itens de navegação e proteção `adminOnly`.
5. Cobrir os novos use cases e controllers com testes.

**Critério de aceite:** ADMIN consegue cadastrar/editar/excluir faixas e tipos de paletização pela interface; regras de 2h e multiplicadores são validadas no backend e no banco; usuário comum não acessa as telas; testes passam.

### C4. Ocupação percentual não integrada ao formulário de agendamento

**Severidade:** Alta

**Evidências:**
- O PDF exige que Data e Faixa Horária contenham “a informação de ocupação por % em tela” no cadastro de agendamento.
- `AppointmentsView.vue:56-139` tem um painel de ocupação com `capacityDate` próprio.
- O campo Data do modal está em `:360-371`; o select de Faixa está em `:435-448`.
- `openCreateModal` (`:730-745`) define `form.date`, mas não sincroniza `capacityDate` nem recarrega a ocupação.
- Não há `watch` de `form.date` ou `form.timeSlotId`.
- A sincronização automática com o formulário só acontece após salvar (`:798-799`); o painel pode ser atualizado manualmente pelo seletor de data ou pelo botão de refresh.

**O que foi encontrado:** o painel de ocupação é útil e correto, mas vive fora do modal. Ao abrir “Novo Agendamento”, o painel pode estar mostrando outra data. Ao trocar a data ou a faixa no formulário, os percentuais não mudam. O usuário precisa sair do modal ou ajustar o painel manualmente para ver a ocupação da data/faixa que está cadastrando; a sincronização automática só ocorre depois de salvar.

**Impacto:** o requisito de exibir a ocupação junto dos campos de Data e Faixa é atendido apenas parcialmente. O usuário pode cadastrar sem ver o impacto antes de salvar, e os avisos de capacidade só aparecem depois da persistência. Como o limite é informativo, não há bloqueio, mas a experiência e a conformidade ficam prejudicadas.

**Correção necessária:**
1. No modal, observar `form.date` e `form.timeSlotId` com debounce e chamar `GET /api/appointments/capacity?date=...`.
2. Exibir, ao lado dos campos, o percentual diário e o percentual da faixa selecionada, com barras e estados de carregamento/erro.
3. Reutilizar o mesmo endpoint e os mesmos formatadores do painel para evitar duplicação.
4. Sincronizar `capacityDate` ao abrir o modal e ao editar um agendamento.
5. Cobrir o comportamento com teste de componente/E2E.

**Critério de aceite:** ao mudar a data ou a faixa no modal, os percentuais correspondentes são atualizados antes de salvar; o usuário vê o impacto diário e por faixa; erros de consulta são tratados sem quebrar o formulário.

### C5. Spring Boot 3.3.5 fora do suporte OSS e ausência de análise de dependências

**Severidade:** Alta

**Evidências:**
- `backend/pom.xml:10` usa Spring Boot 3.3.5.
- O suporte OSS da linha 3.3.x terminou em 30/06/2025; o último patch OSS foi 3.3.13.
- Não há OWASP Dependency-Check, SonarQube, SpotBugs, Checkstyle ou PMD no `pom.xml`.
- `npm audit --omit=dev` retornou 0 vulnerabilidades em produção; o audit completo retornou 1 alta (`vite <= 6.4.3`) e 1 moderada (`esbuild <= 0.24.2`), ambas de desenvolvimento.
- Não há workflow de CI que execute análise de dependências.

**Impacto:** a aplicação pode carregar CVEs conhecidas sem que a equipe perceba. Como o relatório de segurança é um item de entrega, a ausência de análise também reprova T13. As vulnerabilidades de Vite/esbuild afetam principalmente o ambiente de desenvolvimento, mas ainda precisam ser tratadas.

**Correção necessária:**
1. Migrar para uma linha de Spring Boot com suporte OSS (por exemplo, 3.4+/3.5+, conforme compatibilidade) e aplicar o último patch.
2. Adicionar OWASP Dependency-Check ou SonarQube ao build, com limite de severidade e relatório versionado.
3. Atualizar Vite/esbuild para versões corrigidas e revalidar o build.
4. Executar `npm audit` e `mvn dependency:tree` em CI, com falha para vulnerabilidades altas/críticas não aceitas.
5. Documentar exceções e mitigações no relatório de segurança.

**Critério de aceite:** versão do Spring Boot suportada; análise de dependências no build/CI; nenhuma vulnerabilidade alta/crítica não mitigada; relatório de segurança anexado.

### C6. `UpdateUserRequest.active` é `boolean` primitivo

**Severidade:** Alta (integridade de dados)

**Evidência:**
- `backend/src/main/java/com/redekrill/scheduling/presentation/request/UpdateUserRequest.java:22` declara `boolean active`, sem `@NotNull`.
- `UpdateUserUseCase.java:66-73` repassa `command.active()` para `existing.update(...)`.
- A proteção de autoatualização (`UpdateUserUseCase.java:90-103`) só impede que o admin desative a própria conta; um admin editando outro usuário não tem essa proteção.

**O que foi encontrado:** em um PUT em que o campo `active` seja omitido, o Jackson desserializa o primitivo como `false`. O usuário é desativado silenciosamente, mesmo que a intenção fosse apenas alterar nome, e-mail, perfil ou senha. O frontend atual envia o campo, mas integrações futuras ou chamadas diretas à API podem não enviar.

**Impacto:** indisponibilidade de contas, perda de acesso e inconsistência de dados. É um problema clássico de contrato de API e pode ser explorado por engano ou por um cliente mal implementado.

**Correção necessária:**
1. Trocar para `Boolean active` e anotar com `@NotNull(message = "Situação da conta é obrigatória.")`.
2. Adicionar teste de controller/use case para PUT sem `active`, esperando 400.
3. Garantir que o frontend sempre envie o campo e que o formulário de edição preserve o valor atual.
4. Considerar um PATCH separado para ativação/desativação, com semântica explícita.

**Critério de aceite:** PUT sem `active` retorna 400; PUT com `active=false` desativa somente quando solicitado; testes cobrem ambos.

### C7. Lacunas na trilha de auditoria

**Severidade:** Alta

**Evidências:**
- `LoginUseCase.java:40-49`: falhas de credencial lançam `AuthenticationFailedException` antes de qualquer `auditService.record`.
- `RestAuthenticationEntryPoint` responde 401 sem registrar auditoria.
- `RestAccessDeniedHandler.java:58-60` usa o literal `"anonimo"` quando não há autenticação; `AuditLog.java:17` exige e-mail válido via `DomainValidation.requireEmail`, então o registro falha e é engolido pelo `catch` em `:68-71`.
- `AuditLogController`/`AuditView` não registram leituras (GETs).
- O PDF pede uma tela de auditoria/log de acesso registrando as ações executadas nas funcionalidades.

**O que foi encontrado:** as mutações e o login/logout bem-sucedidos são auditados com o ator correto. Porém, tentativas de login inválidas, requisições 401 e o caso de acesso negado sem autenticação não deixam trilha confiável. O uso de `"anonimo"` é incompatível com a validação de e-mail do próprio modelo de auditoria.

**Impacto:** a auditoria não cobre tentativas de acesso não autorizado, que são justamente as mais relevantes para segurança. Um atacante pode tentar senhas e ser bloqueado sem registro. A exigência de “log de acesso” fica parcialmente atendida.

**Correção necessária:**
1. Usar um identificador de ator válido para eventos anônimos (por exemplo, `anonymous@system.local`) ou permitir ator nulo com um campo separado.
2. Registrar falhas de login com e-mail tentado, IP, user agent e motivo, sem armazenar a senha.
3. Registrar 401 no `RestAuthenticationEntryPoint` e 403 no `RestAccessDeniedHandler`.
4. Definir se GETs sensíveis (por exemplo, listagem de usuários e auditoria) serão auditados; se sim, implementar.
5. Não engolir falhas de auditoria em eventos de segurança; no mínimo, logar em nível ERROR e métrica.
6. Adicionar testes para login inválido, 401 e ACCESS_DENIED anônimo.

**Critério de aceite:** tentativas de login inválidas e acessos negados/401 aparecem na auditoria com ator válido; o teste cobre o caso anônimo; nenhum evento de segurança é descartado silenciosamente.

### C8. Concorrência: ID Pedido duplicado pode retornar 500 e atualizações podem se perder

**Severidade:** Média/Alta

**Evidências:**
- `CreateAppointmentUseCase.java:89-92` verifica `existsByOrderId` antes de salvar; é uma checagem “check-then-act” sem lock.
- O banco tem índice único (`V3__appointment_conformity.sql:41`), então a corrida é detectada, mas `GlobalExceptionHandler` não trata `DataIntegrityViolationException`; o fallback é o handler genérico de 500.
- Não há `@Version` nas entidades; duas atualizações concorrentes podem sobrescrever uma à outra.
- O README reconhece a pendência de concorrência (`README.md:565`).

**Impacto:** em cenários de duplo clique, retry ou duas sessões, o usuário pode receber 500 em vez de 409; em edições concorrentes, a última gravação vence silenciosamente e pode perder dados.

**Correção necessária:**
1. Mapear `DataIntegrityViolationException` para 409 com mensagem de conflito e, quando possível, identificar a constraint violada.
2. Adicionar `@Version` nas entidades JPA e tratar `OptimisticLockException` com 409/retry.
3. Manter a checagem amigável no use case, mas tratar a constraint como fonte de verdade.
4. Adicionar testes de concorrência com duas transações simultâneas.

**Critério de aceite:** corrida de `orderId` retorna 409; edição concorrente não perde atualização silenciosamente; testes reproduzem os cenários.

---

## D. Melhorias e Recomendações

As melhorias abaixo não são todas bloqueadores de entrega, mas reduzem risco e elevam a qualidade. A prioridade indica a ordem recomendada; o esforço é uma ordem de grandeza.

| ID | Prioridade | Melhoria | Justificativa | Esforço |
|---|---|---|---|---|
| D1 | P1 | Tratar concorrência de `orderId` com 409 e adicionar `@Version` | Evita 500 e perda de atualização (C8) | Médio |
| D2 | P1 | Completar auditoria de segurança | Cobre login inválido, 401, 403 anônimo e leituras sensíveis (C7) | Médio |
| D3 | P1 | Endurecer sessão/JWT | `localStorage` é vulnerável a XSS; token de 24h sem revogação; logout só no cliente | Grande |
| D4 | P1 | Adicionar rate limiting e lockout no login | Sem isso, força bruta é viável, especialmente com senha padrão | Médio |
| D5 | P1 | Mapear violações de integridade e padronizar erros | Evita 500 genérico e melhora a experiência da API | Pequeno |
| D6 | P1 | Validar Docker Compose de ponta a ponta | README admite que o ambiente completo não foi validado | Médio |
| D7 | P1 | Adicionar healthchecks, headers de segurança e TLS | Operação e segurança em produção | Médio |
| D8 | P1 | Criar testes de frontend, lint e typecheck | Hoje não há nenhum teste de UI, ESLint ou verificação de tipos | Grande |
| D9 | P1 | Criar pipeline de CI | Build, testes, cobertura, `npm audit` e `docker build` automatizados | Médio |
| D10 | P1 | Atualizar Spring Boot e dependências | Linha 3.3.x fora do suporte OSS; Vite/esbuild com advisories de dev | Médio |
| D11 | P1 | Corrigir documentação e adicionar ADRs | README omite V4, árvore do frontend e justificativa do banco | Pequeno |
| D12 | P2 | Implementar outbox/retry de e-mail e teste com MailHog | `SmtpEmailNotifier` tem 0% de cobertura e o envio real não foi validado | Médio |
| D13 | P2 | Adicionar observabilidade | Sem Actuator, métricas, tracing ou logs estruturados | Médio |
| D14 | P2 | Revisar performance de relatórios | Relatórios carregam todo o período; pode degradar com volume | Médio |
| D15 | P2 | Resolver `capacity_settings` | Tabela morta induz a erro e não há tela/configuração | Pequeno |
| D16 | P2 | Proteger Swagger em produção e melhorar exemplos | Documentação pública expõe superfície; falta exemplos de erro | Pequeno |
| D17 | P3 | Adicionar runbook, backup/restore e política de retenção | Operação de produção | Médio |
| D18 | P3 | Revisar acessibilidade e UX | Formulários longos, foco, teclado e mensagens | Pequeno |

### Notas sobre as melhorias prioritárias

**D1 — Concorrência.** A checagem `existsByOrderId` é necessária para a mensagem amigável, mas não é suficiente. A constraint única do banco é a fonte de verdade; o handler precisa traduzi-la para 409. Para edições, `@Version` é o mínimo. Se o volume crescer, considerar retry curto no conflito.

**D3 — JWT.** O token fica em `localStorage` (`frontend/src/services/api.js:13-18`), o que facilita roubo em caso de XSS. O logout (`frontend/src/stores/auth.js:46-58`) apenas remove o token do cliente; o servidor continua aceitando o JWT até a expiração. Para um sistema interno, uma boa evolução é cookie `HttpOnly`/`Secure`/`SameSite` com CSRF, ou access token curto + refresh token revogável. Também vale reduzir a validade padrão de 24h e permitir revogação por usuário.

**D4 — Rate limiting.** O endpoint `/api/auth/login` é público e não tem limite de tentativas. BCrypt aumenta o custo por tentativa, mas não impede força bruta distribuída. Adicionar limite por IP/usuário, atraso progressivo e lockout temporário; considerar MFA para ADMIN.

**D6/D7 — Docker.** O Compose tem healthcheck apenas do PostgreSQL. O frontend depende do backend apenas por `depends_on` simples, sem condição de saúde. O backend não tem endpoint de health. O Nginx não envia CSP, `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy` ou HSTS. O frontend roda como root no Nginx. Para produção, usar secrets, TLS, perfis separados, portas internas não publicadas e imagens fixadas por digest.

**D8 — Frontend.** `frontend/package.json` tem apenas `dev`, `build` e `preview`; não há script de teste, lint ou typecheck. Não há Vitest, Vue Test Utils, Playwright, ESLint ou Prettier. O `UnderConstructionView.vue` existe, mas não é usado pelo router. O mínimo viável é Vitest + Vue Test Utils para componentes críticos e um E2E de login/cadastro/cancelamento.

**D10 — Dependências.** O `npm audit` de produção está limpo; o de desenvolvimento tem 1 alta (Vite) e 1 moderada (esbuild). A correção é atualizar Vite e a cadeia do esbuild. No backend, o Spring Boot 3.3.5 deve migrar para uma linha suportada; o último patch OSS da 3.3.x foi 3.3.13.

**D12 — E-mail.** O fluxo atual é “salva e tenta enviar”; se falhar, o cadastro permanece e o usuário recebe aviso. Isso é adequado para disponibilidade, mas não garante entrega. Um outbox transacional com retry e idempotência resolve perda de e-mail. O teste de integração mocka `EmailNotifier`; falta um teste com SMTP real (MailHog) e um teste do template.

**D13 — Observabilidade.** Não há Spring Boot Actuator, métricas, tracing ou logs estruturados. Para operação, expor `/actuator/health` (com detalhes protegidos) e métricas de fila de e-mail, latência de API e falhas de login. Logs devem incluir correlation ID e não devem conter tokens/senhas.

**D14 — Relatórios.** `GetAppointmentReportUseCase` carrega todos os agendamentos do período. Para volumes grandes, paginar/streamar e criar índices adequados. O relatório é importante para o negócio, então vale teste de carga com um período representativo.

**D15 — `capacity_settings`.** A tabela foi criada na V4 com 480/120, mas `CapacityPolicy` usa constantes. Ou a tabela vira fonte de configuração (com endpoint/tela e cache), ou deve ser removida para não sugerir uma capacidade configurável que não existe.

---

## E. Cobertura e Testes

### E.1 Método e evidências

Os números abaixo foram extraídos dos relatórios existentes:

- `backend/target/surefire-reports/TEST-*.xml`, gerados em 02/11/2026 13:02.
- `backend/target/site/jacoco/jacoco.csv`, gerado em 02/11/2026 13:02:54.
- Configuração do gate em `backend/pom.xml:140-184`.

Não reexecutei `mvn test`/`mvn verify` nesta auditoria para não alterar `backend/target`. Portanto, os números refletem o build presente no diretório de trabalho, não uma reprodução independente a partir do commit auditado. Para a entrega, recomenda-se rodar `mvn clean verify` e arquivar o relatório limpo.

### E.2 Contagem de testes

| Grupo | Classes | Testes |
|---|---:|---:|
| Agendamentos (create, update, cancel, get, list, report, capacity) | 7 | 46 |
| Autenticação (login, logout, current user) | 3 | 12 |
| Usuários (create, update, list) | 3 | 17 |
| Tipos de caminhão (create, update, delete, list) | 4 | 9 |
| Auditoria (service e listagem) | 2 | 3 |
| Dados de referência | 1 | 2 |
| Paginação | 1 | 1 |
| Domínio (Appointment, CapacityPolicy) | 2 | 20 |
| Integração da API | 1 | 8 |
| Arquitetura (ArchUnit) | 1 | 4 |
| **Total** | **25** | **122** |

Detalhamento dos testes mais relevantes:

- `CreateAppointmentUseCaseTest`: 14 testes, incluindo avisos de limite diário e de faixa, falha de e-mail sem rollback, duplicidade de `orderId`, referências inexistentes e ator inativo.
- `UpdateAppointmentUseCaseTest`: 13 testes.
- `AppointmentTest`: 11 testes de invariantes do domínio.
- `UpdateUserUseCaseTest`: 10 testes, incluindo autoproteção do admin.
- `CapacityPolicyTest`: 9 testes.
- `SchedulingApiIntegrationTest`: 8 testes com MockMvc e PostgreSQL embarcado Zonky.
- `ArchitectureTest`: 4 regras ArchUnit.

**Resultado:** 122 testes, **0 falhas, 0 erros, 0 ignorados**.

**Artefato stale:** o diretório Surefire contém 26 XMLs e 125 testes brutos. O XML `TEST-com.redekrill.scheduling.application.usecase.appointment.DeleteAppointmentUseCaseTest.xml` (3 testes, de 01/11/2026) não tem fonte Java nem classe compilada correspondente. Ele é resíduo de build e foi excluído da contagem. Isso não invalida os 122 testes atuais, mas mostra que o `target` não foi limpo; um `mvn clean verify` é necessário para uma evidência de entrega confiável.

### E.3 Cobertura apurada

| Escopo | Classes | Linhas cobertas | Cobertura de linhas | Branches | Cobertura de branches | Instruções |
|---|---:|---:|---:|---:|---:|---:|
| Pacotes críticos (policy + appointment + auth + service) | 15 | 294/294 | **100,00%** | 87/100 | **87,00%** | 99,85% |
| Todos os use cases | 23 | 435/435 | **100,00%** | 111/128 | 86,72% | 100,00% |
| Modelo de domínio | 12 | 130/138 | 94,20% | 40/58 | 68,97% | 95,39% |
| Infraestrutura | 31 | 342/417 | 82,01% | 18/28 | 64,29% | 82,21% |
| Apresentação | 32 | 233/277 | 84,12% | 3/6 | 50,00% | 80,22% |
| **Geral** | **128** | **1271/1400** | **90,79%** | **191/240** | **79,58%** | **90,68%** |

O gate configurado é de **80% de linhas** para os pacotes críticos (`domain.policy`, `application.usecase.appointment`, `application.usecase.auth`, `application.service`). Ele é superado com folga: 100% de linhas e 87% de branches nesses pacotes. A cobertura geral de branches é 79,58%, ligeiramente abaixo de 80%, mas o requisito do PDF é para os domínios críticos, não para o total.

### E.4 Pontos fortes dos testes

1. **Regras de negócio no domínio:** `CapacityPolicyTest` e `AppointmentTest` cobrem os limites, a dobra de paletização e as invariantes do agendamento.
2. **Casos de uso críticos:** criação, atualização, cancelamento e relatórios têm testes dedicados, incluindo cenários de erro.
3. **Integração real de API:** `SchedulingApiIntegrationTest` usa MockMvc e PostgreSQL embarcado, cobrindo autenticação, perfis, auditoria e validações.
4. **Arquitetura:** as 4 regras ArchUnit protegem a separação de camadas e o domínio sem Spring/Jakarta.
5. **Gate no build:** o JaCoCo está configurado para falhar o `verify` se a cobertura de linhas dos pacotes críticos cair abaixo de 80%.

### E.5 Lacunas e riscos de cobertura

| Classe/área | Cobertura de linhas | Risco |
|---|---:|---|
| `SmtpEmailNotifier` | **0,00%** (0/32) | O envio real de e-mail não é testado; o teste de integração mocka `EmailNotifier`. |
| `EmailDeliveryException` | **0,00%** (0/2) | Caminho de falha de e-mail não exercitado. |
| DTOs de request/response de tipo de caminhão | **0,00%** | Contratos de entrada/saída pouco cobertos. |
| `GlobalExceptionHandler` | **42,11%** (16/38) | Mapeamentos de erro e fallback genérico pouco testados. |
| `JpaTruckTypeRepositoryAdapter` | **30,77%** (4/13) | Persistência de tipos de caminhão pouco coberta. |
| `TruckTypePersistenceMapper` | **46,15%** (6/13) | Mapeamento domínio/entidade pouco coberto. |
| `TruckTypeController` | **40,00%** (6/15) | Endpoints administrativos de tipo de caminhão pouco cobertos. |
| `JpaUserRepositoryAdapter` | **47,06%** (8/17) | Persistência de usuários pouco coberta. |
| `JwtTokenService` | **81,82%** (27/33) | Caminhos de token inválido/expirado ainda têm espaço para teste. |

Além disso:

- **Frontend sem testes:** não há Vitest, Vue Test Utils, Playwright, ESLint ou typecheck. O `package.json` tem apenas `dev`, `build` e `preview`.
- **Sem testes E2E:** o fluxo real de login, cadastro, aviso, edição, cancelamento, relatório e auditoria não é exercitado em navegador.
- **Sem testes de carga/performance:** o comportamento com muitos agendamentos e relatórios de período longo não é medido.
- **Sem testes de segurança:** não há testes de autorização negativa além dos de integração, nem testes de rate limiting, XSS, CSRF ou injeção.
- **Sem mutation testing:** a cobertura de linhas não mede a qualidade das asserções.

### E.6 Conclusão da cobertura

O requisito de 80% nos domínios críticos é **atendido com folga** no backend. A suíte é boa para o núcleo de negócio e para os fluxos principais. O que falta não é quantidade de testes no domínio, e sim cobertura de bordas: e-mail real, tratamento de erros, persistência de tipos de caminhão/usuários, frontend, E2E, carga e segurança. Para a entrega, a evidência deve ser regenerada com `mvn clean verify` e complementada por testes de frontend e um relatório de análise estática.

---

## F. Segurança

### F.1 Ativos e superfície de ataque

**Ativos:** credenciais de usuários, tokens JWT, dados de agendamentos (incluindo nome/e-mail de fornecedores), trilha de auditoria, funções administrativas, banco PostgreSQL e envio de e-mail.

**Superfície:** endpoint público de login, API REST autenticada, Swagger público, frontend SPA, portas do PostgreSQL/MailHog publicadas no Compose e o próprio repositório público.

### F.2 Controles positivos implementados

- **Hash de senha:** BCrypt, sem armazenamento de senha em claro.
- **JWT:** assinatura simétrica HMAC (algoritmo escolhido pelo JJWT conforme o tamanho da chave) com validação de chave de pelo menos 256 bits (`JwtTokenService.java:75-81`) e expiração configurável.
- **Autorização:** regras por método e perfil no `SecurityConfig`; ADMIN para usuários, auditoria e mutações de agendamentos/tipos de caminhão; USER somente leitura.
- **Sessão stateless:** `SessionCreationPolicy.STATELESS`; não há cookie de sessão.
- **CORS:** allowlist configurável, sem wildcard; métodos e headers explícitos (`SecurityConfig.java:64-75`).
- **CSRF:** desabilitado; aceitável para bearer token em header, mas exigiria reavaliação se o token migrar para cookie.
- **Validação:** Bean Validation na borda + validações de domínio; `@Valid` nos requests.
- **SQL injection:** JPA com consultas parametrizadas e `@Param`; não há concatenação de entrada em SQL.
- **XSS:** Vue escapa interpolações; não há `v-html`/`innerHTML`; Thymeleaf usa `th:text`.
- **Erros:** `server.error.include-message: never` e `include-stacktrace: never`; o handler genérico não devolve stacktrace.
- **Auditoria:** não armazena senhas nem tokens; registra ator, ação, entidade e detalhes.
- **Docker:** backend roda como usuário não-root; `.dockerignore` evita copiar `target`, `.env` e logs.

### F.3 Achados de segurança

| ID | Severidade | Achado | Evidência | Impacto | Recomendação |
|---|---|---|---|---|---|
| S1 | **Crítica** | Segredos e credenciais padrão em repositório público | `application.yml:39,48`; `docker-compose.yml:42,46`; `.env.example:7,12`; `DataInitializer.java:22,56-58` | Forja de token admin ou login direto com credenciais conhecidas | Ver C1 |
| S2 | **Alta** | JWT em `localStorage`, 24h, sem revogação; logout só no cliente | `api.js:13-18`; `auth.js:28-33,46-58`; `JwtTokenService.java:44-53`; `LogoutUseCase.java:7-10` | Roubo de token via XSS; token válido até expirar; logout não invalida no servidor | Cookie HttpOnly/Secure ou access curto + refresh revogável; reduzir TTL; denylist |
| S3 | **Alta** | Sem rate limiting/lockout no login; senha admin padrão | `AuthController` público; nenhum filtro de limite; `DataInitializer` | Força bruta viável, especialmente com credenciais padrão | Limite por IP/usuário, atraso progressivo, lockout, MFA para ADMIN |
| S4 | **Alta** | Spring Boot 3.3.5 fora do suporte OSS; sem OWASP/Sonar; advisories de dev no frontend | `pom.xml:10`; ausência de plugins; `npm audit` | CVEs conhecidas sem patch; relatório de segurança ausente | Ver C5 |
| S5 | **Média** | Auditoria incompleta de eventos de segurança | `LoginUseCase.java:40-49`; `RestAuthenticationEntryPoint`; `RestAccessDeniedHandler.java:58-60` | Tentativas de invasão não registradas; `"anonimo"` falha na validação | Ver C7 |
| S6 | **Média** | Sem headers de segurança/CSP/HSTS/TLS; Swagger público | `frontend/nginx.conf` só tem `Cache-Control`; `SecurityConfig.java:41-47` | XSS/clickjacking sem mitigação; documentação exposta | Headers no Nginx, CSP, HSTS, TLS; restringir Swagger em produção |
| S7 | **Média** | `active` primitivo pode desativar conta silenciosamente | `UpdateUserRequest.java:22`; `UpdateUserUseCase.java:66-73` | Indisponibilidade/integridade | Ver C6 |
| S8 | **Média** | Corrida de `orderId` e edições concorrentes sem lock otimista | `CreateAppointmentUseCase.java:89-92`; `V3...:41`; ausência de `@Version` | 500 em conflito; perda de atualização | Ver C8 |
| S9 | **Baixa** | `redirect` pós-login aceita caminho arbitrário | `LoginView.vue:158-159`; `router/index.js:78-103` | Baixo: o Vue Router normaliza URLs externas para caminho interno; `//evil.com` gera warning, mas não navega para fora | Validar que o redirect começa com `/` e não com `//`; usar allowlist de rotas |
| S10 | **Baixa** | Portas do PostgreSQL e MailHog publicadas no Compose | `docker-compose.yml:9-10,22-24` | Exposição desnecessária em ambiente não isolado | Perfis de produção, bind em localhost ou rede interna |
| S11 | **Baixa** | Nginx do frontend roda como root e sem healthcheck | `frontend/Dockerfile`; `docker-compose.yml:50-57` | Superfície e operação | Usuário não-root, healthcheck, read-only FS |
| S12 | **Informativa** | CSRF desabilitado | `SecurityConfig.java:32` | Aceitável com bearer token em header; perigoso se migrar para cookie | Manter bearer ou habilitar CSRF ao usar cookie |
| S13 | **Informativa** | Sem upload de arquivo, SSRF ou desserialização insegura | Inspeção dos controllers | Superfície reduzida | Manter validação e revisar ao adicionar uploads |
| S14 | **Informativa** | Política de senha mínima de 8 caracteres, sem complexidade/MFA | `CreateUserRequest`/`UpdateUserRequest`; `PasswordHasher` | Risco moderado de senhas fracas | Aumentar política, bloquear senhas comuns, MFA para ADMIN |

### F.4 Cenários de ataque e controle atual

1. **Repositório público → segredo JWT padrão → token de admin.** O atacante assina um JWT com o e-mail do admin e a assinatura conhecida. O filtro valida a assinatura, busca o usuário ativo e autentica com o papel do banco. **Controle atual:** nenhum, se o segredo padrão estiver em uso. **Risco:** crítico. **Mitigação:** C1.
2. **Credenciais padrão → login direto.** O e-mail e a senha do admin estão documentados e o seed não força troca. **Controle atual:** BCrypt apenas; nenhum lockout. **Risco:** crítico em demonstração pública. **Mitigação:** C1 + S3.
3. **XSS → roubo de token em `localStorage`.** Qualquer XSS executa JavaScript no origem e lê o token; o token vale 24h e não pode ser revogado. **Controle atual:** Vue escapa conteúdo e não há `v-html`; não há CSP. **Risco:** alto. **Mitigação:** S2 + S6.
4. **Força bruta no login.** Sem rate limiting/lockout, o atacante pode tentar senhas; BCrypt encarece, mas não impede. **Controle atual:** BCrypt e mensagem genérica de erro. **Risco:** alto. **Mitigação:** S3.
5. **USER tenta endpoint ADMIN.** O backend retorna 403 e o `RestAccessDeniedHandler` registra o e-mail autenticado. **Controle atual:** funciona para usuário autenticado. **Risco:** baixo. **Mitigação:** completar auditoria de 401/anônimo (C7).
6. **Injeção SQL.** Consultas JPA parametrizadas; não há concatenação de entrada. **Controle atual:** adequado. **Risco:** baixo.
7. **CSRF.** O token vai no header `Authorization`, não em cookie; um site externo não consegue lê-lo nem enviá-lo automaticamente. **Controle atual:** adequado para o desenho atual. **Risco:** baixo, desde que não migre para cookie sem CSRF.
8. **Open redirect.** O `redirect` é passado ao `router.push`; o Vue Router trata `https://evil.com` como caminho interno (`/https://evil.com`) e `//evil.com` como localização com múltiplas barras, sem navegação externa. **Controle atual:** baixo risco. **Mitigação:** validar o formato.

### F.5 Veredito de segurança

A segurança é **insuficiente para produção** e **crítica para uma demonstração pública**. Os controles de autorização, hash de senha, CORS, validação e prevenção de SQL injection/XSS são adequados para o escopo, mas os segredos padrão e a ausência de rate limiting/revogação dominam o risco. A correção de C1, S2, S3 e C5 é obrigatória antes de qualquer implantação exposta. Não foi realizado pentest; este parecer é baseado em inspeção estática e configuração.

---

## G. Infraestrutura e Documentação

### G.1 Stack e versões

| Componente | Versão/Configuração |
|---|---|
| Java | 21 (`pom.xml:21`) |
| Spring Boot | 3.3.5 (`pom.xml:10`) — fora do suporte OSS desde 30/06/2025 |
| Build backend | Maven Wrapper; `mvnw`/`mvnw.cmd` |
| Banco | PostgreSQL 16 (`docker-compose.yml:3`), Flyway, `ddl-auto: validate` |
| JWT | jjwt 0.12.6 (`pom.xml:22`) |
| OpenAPI | springdoc 2.6.0 (`pom.xml:23`) |
| Testes de arquitetura | ArchUnit 1.3.0 (`pom.xml:24`) |
| Cobertura | JaCoCo 0.8.12 (`pom.xml:25`) |
| Frontend | Vue 3.5, Vite 5.4, Tailwind 3.4, PrimeVue 4.2, Pinia 2.3, Axios 1.7 |
| Node (ambiente auditado) | 18.17.0 / npm 9.6.7; Docker usa `node:20-alpine` |

### G.2 Docker e Compose

**Backend (`backend/Dockerfile`):**
- Build multi-stage: `maven:3.9.9-eclipse-temurin-21` → `eclipse-temurin:21-jre-alpine`.
- `mvn dependency:go-offline` antes de copiar o código, aproveitando cache de camadas.
- `mvn -DskipTests package`; os testes não rodam no build da imagem.
- Usuário `app` não-root; `-XX:MaxRAMPercentage=75.0`.
- `.dockerignore` exclui `target`, `.mvn`, wrappers, logs e `.env`.

**Frontend (`frontend/Dockerfile`):**
- Build multi-stage: `node:20-alpine` → `nginx:1.27-alpine`.
- `npm ci` com `package-lock.json` versionado; `npm run build`.
- Nginx serve `dist`, faz proxy de `/api`, `/swagger-ui` e `/v3/api-docs` para `backend:8080`, e usa `try_files` para SPA.
- Não há usuário não-root explícito; o Nginx roda como root por padrão.
- `.dockerignore` exclui `node_modules`, `dist`, `.vite`, `coverage`, logs e `.env`.

**Compose (`docker-compose.yml`):**
- Serviços: `postgres` (16-alpine), `mailhog` (v1.0.1), `backend` e `frontend`.
- Volume `postgres_data`.
- Healthcheck do PostgreSQL com `pg_isready`; backend depende de `service_healthy` para o banco e `service_started` para o MailHog.
- Portas publicadas: 5432 (PostgreSQL), 1025/8025 (MailHog), 8080 (backend), 5173→80 (frontend).
- Fallbacks de credenciais e segredo JWT — ver C1.
- Não há healthcheck de backend/frontend, limites de recursos, perfis de produção, TLS ou secrets.

**Validação:** não executei `docker compose up`, build das imagens ou teste de fumaça. A análise é estática. O README reconhece que o ambiente completo ainda precisa ser validado (`README.md:567`).

### G.3 Banco e migrations

| Migration | Conteúdo | Observação |
|---|---|---|
| `V1__schema.sql` | `users`, `truck_type`, `palletization_type`, `time_slot`, `appointment`, `audit_log`; índices e FKs | Estrutura inicial; `appointment` ainda sem `order_id`, `notes` e status. |
| `V2__reference_data.sql` | Paletizado/Não Paletizado; 4 faixas; Carreta/Truck/VUC | Dados de referência. |
| `V3__appointment_conformity.sql` | `order_id`, `notes`, status/cancelamento, backfill, índices, constraints e Van | Backfill preserva legado; `CHECK ... NOT VALID` para limites de 80 caracteres; nenhum registro é removido. |
| `V4__administrative_configuration.sql` | `capacity_settings` (480/120), constraint de 2h, unicidade de início, multiplicadores obrigatórios | `capacity_settings` não é lida por nenhum código Java; tabela morta. |

**Pontos positivos:** Flyway com `ddl-auto: validate`, backfill seguro, constraints de status/cancelamento, índice único de `order_id`, índices por data/status/faixa e proteção dos multiplicadores obrigatórios.

**Pontos de atenção:**
- `capacity_settings` não tem uso; a configuração real está em constantes.
- O README lista V1–V3 e omite a V4 (`README.md:490-496`).
- A V4 não aparece na documentação de migrations, embora altere o schema.
- Não há política de retenção/arquivamento de `audit_log` ou de agendamentos antigos.
- Não há teste de migração a partir de uma base legada real; o backfill é testado apenas por leitura de código.

### G.4 Configuração e perfis

- `application.yml` é o único arquivo de configuração; não há `application-prod.yml`/`application-dev.yml`.
- Variáveis principais: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_EXPIRATION`, `CORS_ALLOWED_ORIGINS`, `MAIL_*`, `SEED_ADMIN_*`.
- `spring.jpa.open-in-view: false`; boa prática.
- `spring.jpa.hibernate.ddl-auto: validate`; bom para Flyway.
- `server.error.include-message/stacktrace: never`; bom.
- `springdoc` expõe Swagger publicamente; aceitável para demo, mas deve ser protegido em produção.
- Logging apenas `INFO` para o pacote da aplicação; sem níveis por ambiente, sem JSON estruturado.
- Não há Actuator/health/metrics.

### G.5 README e documentação

**Pontos fortes:**
- Explica Visão geral, tecnologias, funcionalidades, regras de negócio, permissões, e-mail, organização, execução com Docker e em desenvolvimento, configurações, usuário inicial, API, banco, segurança, testes e melhorias pendentes.
- Documenta os endpoints principais e exemplos de payload.
- Explica o cálculo de capacidade e a diferença entre `dailyPallets` e `slotPallets`.
- Lista as pendências de segurança, concorrência, E2E, Docker, telas e entrega (`README.md:560-569`), o que é honesto.

**Lacunas:**
- Não justifica a escolha do PostgreSQL nem as decisões arquiteturais (T3).
- Omite a V4 e `capacity_settings` (`README.md:490-496`).
- A árvore do frontend está desatualizada: lista `LoginView`, `DashboardView` e `UnderConstructionView` (`README.md:211-214`), mas existem `AppointmentsView`, `ReportsView`, `TruckTypesView`, `UsersView` e `AuditView`.
- Não documenta a matriz de permissões endpoint a endpoint.
- Não documenta o comportamento de falha de e-mail além do aviso.
- Não inclui relatório de segurança, vídeo ou PDF.
- Não há ADRs (Architecture Decision Records) para PostgreSQL, JWT, Flyway, Clean Architecture ou estratégia de testes.
- Não há instruções de produção (TLS, secrets, backup, monitoramento).

### G.6 Git, CI e entrega

- Repositório público: `origin` aponta para `https://github.com/ROD8Rodrigues/Rede-Krill.git`.
- `git ls-remote` retorna `refs/heads/main` em `06cee93`; o HEAD local coincide com `origin/main`.
- 6 commits com mensagens descritivas; árvore limpa antes deste relatório.
- 216 arquivos rastreados.
- `.gitignore` exclui `.env`, `.env.local`, `target`, `node_modules`, `dist`, `.vite`, `coverage`, logs e IDEs; `.env.example` é versionado.
- **Não há workflows de CI** (`.github/workflows` ausente).
- **Não há artefatos de entrega** (PDF, vídeo, relatório de segurança).
- O build da imagem backend usa `-DskipTests`; sem CI, os testes não são executados automaticamente em um pipeline.

### G.7 Prontidão operacional

| Item | Situação |
|---|---|
| Healthcheck backend/frontend | Ausente |
| Métricas/tracing | Ausente |
| Logs estruturados/correlation ID | Ausente |
| TLS | Ausente no Compose/Nginx |
| Secrets manager | Ausente; fallbacks no Compose |
| Backup/restore | Não documentado |
| Retenção de auditoria | Não documentada |
| Limites de recurso | Não configurados |
| Perfis dev/prod | Ausente |
| Runbook operacional | Ausente |

**Conclusão da infraestrutura:** a estrutura Docker é boa para demonstração local e o backend está mais bem preparado que o frontend. Falta validação de execução e faltam controles de produção. O Compose é um bom ponto de partida, não um ambiente pronto para exposição pública.

---

## H. Plano de Correção

O plano está ordenado por risco e dependência. As estimativas são de ordem de grandeza e pressupõem uma pessoa familiarizada com o projeto.

### H.1 P0 — Bloqueadores de entrega (fazer antes de qualquer entrega)

| # | Ação | Correlação | Esforço | Critério de saída |
|---|---|---|---|---|
| 1 | Remover segredos/credenciais padrão; fail-fast; rotacionar; limpar histórico; forçar troca de senha do admin | C1/S1 | 1–2 dias | Repositório sem segredo utilizável; Compose sem variáveis falha com mensagem clara; admin sem senha conhecida |
| 2 | Criar telas/endpoints CRUD para faixas horárias e tipos de paletização; decidir destino de `capacity_settings` | C3/B9 | 3–5 dias | ADMIN cadastra/edita/exclui; regras de 2h e multiplicadores validadas; testes |
| 3 | Integrar ocupação percentual ao modal de agendamento | C4/B10/B11 | 1–2 dias | Data/faixa atualizam % antes de salvar; loading/erro tratados |
| 4 | Produzir PDF, vídeo e relatório de segurança; justificar banco e arquitetura | C2/T3/T11-T13 | 2–4 dias | PDF/vídeo/relatório linkados; vídeo ≤ 15 min; banco justificado |
| 5 | Atualizar Spring Boot para linha suportada e corrigir Vite/esbuild; rodar OWASP/Sonar | C5/S4 | 1–2 dias | Versão suportada; análise sem altas/críticas não mitigadas |

**Portão P0:** nenhum segredo padrão; todas as telas obrigatórias; ocupação no formulário; artefatos de entrega; versão suportada. Sem isso, a entrega não deve ser submetida.

### H.2 P1 — Conformidade e robustez (1 a 2 semanas)

| # | Ação | Correlação | Esforço | Critério de saída |
|---|---|---|---|---|
| 6 | Corrigir `UpdateUserRequest.active` para `Boolean` + `@NotNull` | C6/S7 | 0,5 dia | PUT sem `active` retorna 400; testes |
| 7 | Mapear `DataIntegrityViolationException` para 409 e adicionar `@Version` | C8/D1 | 1–2 dias | Corrida de `orderId` retorna 409; edição concorrente não perde dados |
| 8 | Completar auditoria: login inválido, 401, 403 anônimo, leituras sensíveis | C7/D2 | 1–2 dias | Eventos registrados com ator válido; testes |
| 9 | Adicionar rate limiting/lockout no login | S3/D4 | 1 dia | Limite por IP/usuário; lockout temporário; teste |
| 10 | Endurecer JWT: cookie HttpOnly ou access curto + refresh/revogação; reduzir TTL; logout server-side | S2/D3 | 2–4 dias | Token não legível por JS ou TTL curto; revogação; logout efetivo |
| 11 | Validar Docker Compose, adicionar healthchecks, headers, TLS e non-root no frontend | G/D6/D7 | 2–3 dias | `docker compose up` testado; healthchecks; headers; TLS |
| 12 | Criar testes de frontend (Vitest + Vue Test Utils), lint, typecheck e um E2E crítico | D8 | 3–5 dias | Testes de componentes; lint/typecheck no CI; E2E de login/cadastro/cancelamento |
| 13 | Criar CI com build, testes, cobertura, `npm audit` e `docker build` | D9 | 1–2 dias | Pipeline verde; relatórios publicados |
| 14 | Atualizar README, adicionar ADRs, V4 e árvore real do frontend | D11/G5 | 1 dia | Documentação consistente com o código |

**Portão P1:** P0 concluído; testes de regressão para todas as correções; CI verde; auditoria completa; Compose validado; frontend com testes mínimos.

### H.3 P2 — Qualidade e operação (2 a 4 semanas)

| # | Ação | Correlação | Esforço | Critério de saída |
|---|---|---|---|---|
| 15 | Outbox/retry de e-mail + teste com MailHog | D12/B17 | 2–3 dias | E-mail com retry/idempotência; teste real com MailHog |
| 16 | Actuator, métricas, logs estruturados e correlation ID | D13/G7 | 2–3 dias | `/actuator/health`; métricas; logs sem segredos |
| 17 | Revisar performance de relatórios e paginação | D14 | 2–3 dias | Período grande sem degradação; teste de carga |
| 18 | Resolver `capacity_settings` (usar ou remover) | D15/G3 | 1 dia | Tabela usada com tela/endpoint ou removida/documentada |
| 19 | Proteger Swagger em produção e melhorar exemplos de erro | D16/T4 | 1 dia | Swagger restrito; OpenAPI com erros |
| 20 | Runbook, backup/restore, retenção e limites de recurso | D17/G7 | 2–3 dias | Runbook aprovado; backup testado |
| 21 | Revisão de acessibilidade/UX | D18 | 1–2 dias | Navegação por teclado; foco; mensagens |

**Portão P2:** operação documentada, observabilidade mínima, e-mail confiável e dívidas de documentação resolvidas.

### H.4 Ordem recomendada

1. **C1** (segredos) e **C2** (artefatos) em paralelo, pois são os bloqueios mais objetivos.
2. **C3** (telas) e **C4** (ocupação) em seguida, para fechar o escopo funcional.
3. **C5/C6/C7/C8** e **D1–D7** para robustez e segurança.
4. **D8–D11** para qualidade, CI e documentação.
5. **P2** para maturidade operacional.

### H.5 Definition of Done para a entrega

- [ ] Nenhum segredo, senha ou token utilizável no repositório ou no histórico.
- [ ] Admin inicial sem senha conhecida; troca forçada ou seed apenas em dev.
- [ ] Telas de cadastro para todos os itens exigidos; regras de 2h/multiplicadores validadas.
- [ ] Ocupação % visível e atualizada nos campos de Data e Faixa do modal.
- [ ] PDF, vídeo e relatório de segurança entregues e linkados.
- [ ] Spring Boot em linha suportada; análise de dependências sem altas/críticas não mitigadas.
- [ ] `docker compose up` validado com PostgreSQL, MailHog, backend, frontend e Swagger.
- [ ] `mvn clean verify` verde, com relatório JaCoCo limpo e sem XML stale.
- [ ] Testes de frontend, lint e typecheck no CI.
- [ ] `UpdateUserRequest.active` corrigido; concorrência de `orderId` retorna 409.
- [ ] Auditoria cobre login inválido, 401 e 403 anônimo.
- [ ] README atualizado com V4, árvore real do frontend, justificativa do banco e ADRs.

---

## I. Parecer Final

### I.1 Parecer

**Não aprovado para entrega integral no estado atual.**

O projeto tem um núcleo funcional forte e uma suíte de backend acima da média para um desafio técnico. As regras de negócio centrais estão implementadas no domínio, a cobertura dos pacotes críticos é de 100% de linhas e os testes cobrem os principais fluxos. A arquitetura em camadas é real e protegida por ArchUnit. Em uma demonstração local, com segredos sobrescritos e rede isolada, o sistema deve funcionar e demonstrar bem o escopo.

A aprovação integral é impedida por quatro fatores objetivos:

1. **Segurança:** segredos e credenciais padrão em repositório público permitem comprometimento administrativo; não há rate limiting, revogação de JWT ou headers de segurança.
2. **Escopo funcional:** faltam telas de cadastro para faixas horárias e tipos de paletização, e a ocupação percentual não está integrada ao formulário.
3. **Entrega:** não há PDF, vídeo nem relatório de segurança, itens explicitamente exigidos.
4. **Sustentação:** Spring Boot 3.3.5 está fora do suporte OSS, não há CI e o frontend não tem testes.

Nenhum desses fatores exige reescrita. São correções localizadas, com esforço concentrado em P0/P1.

### I.2 O que está bom

- Regras de 480/120, quatro faixas de 2h, dobra de Não Paletizado somente na faixa e cancelamento lógico implementadas corretamente.
- Separação real entre domínio, aplicação, infraestrutura e apresentação, com 4 regras ArchUnit.
- 122 testes, 0 falhas; 100% de linhas e 87% de branches nos pacotes críticos.
- Autorização no backend por perfil, com USER somente leitura e ADMIN nas mutações.
- Relatórios por período, auditoria de mutações, Swagger, Docker multi-stage e Compose completo.
- README detalhado e honesto ao listar suas próprias pendências.

### I.3 O que impede a aprovação

- **C1:** segredo JWT e senha do admin padrão em repo público.
- **C2:** PDF, vídeo e relatório de segurança ausentes.
- **C3:** sem telas de cadastro para faixas horárias e tipos de paletização.
- **C4:** ocupação % não integrada ao modal de agendamento.
- **C5:** Spring Boot 3.3.5 fora do suporte e sem análise de dependências.
- **C6:** `active` primitivo pode desativar usuário silenciosamente.
- **C7:** auditoria incompleta de eventos de segurança.
- **C8:** concorrência de `orderId` pode retornar 500 e edições podem se perder.

### I.4 Condições objetivas para aprovação

1. Concluir o P0 inteiro (segredos, telas, ocupação, artefatos, Spring Boot suportado).
2. Corrigir C6, C7 e C8 com testes de regressão.
3. Validar o Compose de ponta a ponta com PostgreSQL, MailHog, backend, frontend e Swagger.
4. Rodar `mvn clean verify` e arquivar o relatório JaCoCo limpo.
5. Adicionar CI com testes, cobertura, `npm audit` e build Docker.
6. Adicionar testes de frontend e um E2E dos fluxos críticos.
7. Atualizar README/ADRs e anexar o relatório de segurança.

Se o objetivo for apenas **demonstração local**, o sistema é utilizável com estas condições mínimas: sobrescrever todos os segredos, não expor portas publicamente, usar dados fictícios e não habilitar o Swagger em rede aberta. Ainda assim, as lacunas de escopo e entrega permanecem.

### I.5 Considerações finais

O projeto demonstra boa capacidade de modelagem de domínio, organização arquitetural e testes. O ponto mais positivo é que as regras críticas não ficaram na tela: estão no domínio e são verificadas por testes. O ponto mais negativo é que a preocupação com segurança e entrega ficou abaixo da qualidade do núcleo funcional. Como o repositório é público e o desafio exige relatório de segurança, os valores padrão são um risco desproporcional ao esforço de correção.

Com o P0 concluído, o parecer pode passar para **“aprovado com ressalvas”**; com P0 + P1, para **“aprovado”**. A trajetória recomendada é corrigir primeiro o que é objetivo e bloqueante, depois o que é robustez, e por último o que é maturidade operacional.

### I.6 Limitações desta auditoria

- Inspeção estática; não houve execução completa do sistema, Docker, Swagger ou envio real de e-mail.
- Números de teste/cobertura extraídos dos relatórios existentes, não de uma execução limpa do commit.
- Não foi realizado pentest, teste de carga, análise de malware ou revisão de infraestrutura de nuvem.
- O XML stale de `DeleteAppointmentUseCaseTest` foi identificado e excluído da contagem; recomenda-se `mvn clean verify` para eliminar a dúvida.
- A análise de dependências do frontend usou `npm audit`; não substitui SCA contínuo nem revisão de licenças.

---

**Relatório elaborado em 02/11/2026.**  
**Escopo:** auditoria técnica somente leitura do repositório Rede Krill, comparada ao PDF oficial do desafio.  
**Único arquivo criado:** `AUDITORIA_TECNICA_REDE_KRILL.md`.
