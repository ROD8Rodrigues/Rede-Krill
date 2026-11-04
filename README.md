# Rede Krill — Sistema de Agendamento de Caminhões

Organizar o recebimento de mercadorias é uma parte importante da rotina de um centro de distribuição. Pensando nisso, desenvolvemos o **Rede Krill Scheduling**, um sistema criado para facilitar o agendamento de caminhões e oferecer uma visão mais clara da ocupação dos horários de recebimento.

A aplicação permite cadastrar e acompanhar agendamentos, consultar a capacidade diária e por faixa horária, controlar informações dos veículos e fornecedores e acompanhar as operações realizadas no sistema.

Além disso, conta com notificações por e-mail, relatórios e diferentes níveis de acesso para manter as informações organizadas e facilitar o trabalho da equipe.

Este projeto foi desenvolvido como parte de um desafio técnico, utilizando uma arquitetura dividida entre backend e frontend, com banco de dados PostgreSQL e ferramentas voltadas à qualidade, segurança e manutenção do código.

---

## Sobre o projeto

O objetivo do Rede Krill Scheduling é tornar o processo de agendamento mais organizado, transparente e fácil de acompanhar.

Entre as principais funcionalidades estão:

- Cadastro, consulta, edição e cancelamento de agendamentos.
- Acompanhamento da ocupação diária e por horário.
- Avisos quando os limites de capacidade são ultrapassados.
- Controle dos tipos de veículos e paletização.
- Notificações por e-mail aos fornecedores.
- Relatórios com filtros por período.
- Gerenciamento de usuários e permissões.
- Histórico das operações realizadas no sistema.

O projeto foi desenvolvido com atenção especial à organização do código, segurança das informações e confiabilidade das regras de negócio.

## Tecnologias utilizadas

O sistema utiliza tecnologias diferentes em cada camada, permitindo que o frontend e o backend evoluam de forma independente.

### Backend

- Java 21 e Spring Boot 4.1.2.
- Spring Security para autenticação e controle de acesso.
- Spring Data JPA e Hibernate.
- PostgreSQL 16.
- Flyway para versionamento do banco de dados.
- JWT para autenticação.
- Refresh tokens com armazenamento protegido por hash SHA-256.
- Spring Mail e Thymeleaf para notificações por e-mail.
- Outbox transacional para processamento assíncrono de mensagens.
- Springdoc OpenAPI para documentação da API.
- Actuator, Micrometer e Prometheus para monitoramento.
- Lombok.

### Frontend

- Vue 3.5 e Vite.
- Vue Router 4 e Pinia 2.
- PrimeVue 4 com tema Aura.
- PrimeIcons e Tailwind CSS 3.
- Axios para comunicação com a API.
- Vitest e Vue Test Utils para testes unitários.
- Playwright para testes automatizados de interface.
- ESLint, Prettier e vue-tsc para qualidade e validação do código.

### Ferramentas e qualidade

- Maven Wrapper.
- JUnit 5, Mockito e AssertJ.
- ArchUnit para testes arquiteturais.
- PostgreSQL embarcado nos testes.
- JaCoCo para análise de cobertura.
- Docker Compose e Nginx.
- GitHub Actions para integração contínua.
- Dependabot para acompanhamento de dependências.
- OWASP Dependency-Check para análise de vulnerabilidades.

---

## Como o sistema foi organizado

O backend segue os princípios da **Clean Architecture**, separando as responsabilidades em quatro camadas.

Essa organização ajuda a manter as regras de negócio independentes dos detalhes técnicos, facilitando futuras alterações, manutenção e testes.

```text
com.redekrill.scheduling
├── domain/
│   └── model, policy, repository, exception
│
├── application/
│   └── dto, port, service, usecase
│
├── infrastructure/
│   └── persistence, security, config,
│       email, seed, observability, transaction
│
└── presentation/
    └── controller, request, response,
        mapper, exception
```

As dependências entre as camadas seguem as regras da arquitetura, verificadas por testes automatizados com ArchUnit.

As decisões técnicas mais importantes também foram documentadas em ADRs, disponíveis em [`docs/arquitetura/`](docs/arquitetura/README.md).

### Comunicação entre frontend e backend

O frontend utiliza a API REST do backend para consultar e enviar informações.

Durante o desenvolvimento, o Vite encaminha as requisições para o backend. Em produção, essa comunicação é feita pelo Nginx.

Essa configuração permite manter uma origem única no navegador, simplificando a comunicação entre as duas aplicações.

A autenticação utiliza cookies `HttpOnly`, com proteção CSRF e renovação de sessão. Os tokens não são armazenados no `localStorage`.

---

## O que o sistema oferece

### Agendamentos

É possível cadastrar, consultar, editar e cancelar agendamentos de recebimento.

Cada registro possui informações como:

- Número do pedido.
- Fornecedor.
- E-mail do fornecedor.
- Data de recebimento.
- Horário selecionado.
- Tipo de veículo.
- Tipo de paletização.
- Quantidade de pallets.
- Observações.

O número do pedido é obrigatório e único em todo o sistema.

### Acompanhamento da capacidade

O sistema permite acompanhar a ocupação diária e a ocupação de cada faixa horária.

Durante o cadastro de um agendamento, o usuário consegue visualizar os percentuais de ocupação antes de confirmar a operação.

Quando a capacidade de referência é ultrapassada, o sistema apresenta um aviso, mas permite que o agendamento seja realizado.

### Gerenciamento administrativo

Os usuários com perfil ADMIN possuem acesso às funções de gerenciamento, incluindo:

- Agendamentos.
- Tipos de veículos.
- Faixas horárias.
- Tipos de paletização.
- Usuários.
- Relatórios.
- Histórico de auditoria.

O perfil USER possui acesso de consulta, respeitando as permissões definidas para cada recurso.

### Notificações por e-mail

Quando um agendamento é criado, uma notificação é registrada para envio ao fornecedor.

O sistema utiliza uma outbox transacional para evitar que o registro do agendamento e a criação da mensagem fiquem inconsistentes.

O processamento ocorre de forma assíncrona, com novas tentativas em caso de falha.

### Relatórios e auditoria

Os relatórios permitem consultar os agendamentos dentro de um período específico.

O histórico de auditoria registra operações importantes, como criação, edição, cancelamento, autenticação e eventos de segurança.

Essas informações ficam disponíveis para consulta administrativa.

---

## Regras de negócio

### Identificação dos pedidos

Todo agendamento precisa possuir um `orderId` numérico, inteiro, maior que zero e exclusivo.

Caso o número já esteja cadastrado, o sistema retorna HTTP `409 Conflict`, evitando duplicidade.

### Cancelamento de agendamentos

O cancelamento é lógico, ou seja, o registro não é apagado do banco de dados.

Ele permanece com o status `CANCELED`, acompanhado das informações de data e responsável pelo cancelamento.

Isso permite preservar o histórico das operações.

Agendamentos cancelados:
- Não podem ser editados.
- Não podem ser cancelados novamente.
- Não entram nos cálculos de capacidade.
- Continuam disponíveis para consulta histórica e auditoria.

### Horários de recebimento

O sistema possui quatro faixas horárias padrão:

| ID | Horário |
|---|---|
| 1 | 08:00 às 10:00 |
| 2 | 10:00 às 12:00 |
| 3 | 13:00 às 15:00 |
| 4 | 15:00 às 17:00 |

Cada faixa deve possuir exatamente duas horas de duração. O administrador também pode gerenciar as faixas disponíveis.

### Controle de capacidade

Os limites de referência são:

| Período | Capacidade |
|---|---:|
| Diário | 480 pallets |
| Por faixa horária | 120 pallets |

Esses valores servem para acompanhamento da operação, não para bloquear agendamentos.

Quando um limite é ultrapassado, o sistema retorna um aviso em `warnings`, mantendo a possibilidade de concluir o cadastro.

Os limites são definidos pelas regras de domínio, conforme documentado no [ADR-004](docs/arquitetura/ADR-004-limites-fixos-de-capacidade.md).

### Cálculo de pallets

O cálculo considera o tipo de paletização informado no agendamento.

| Tipo | Quantidade declarada | Ocupação diária | Ocupação por horário |
|---|---:|---:|---:|
| Paletizado | 10 | 10 | 10 |
| Não paletizado | 10 | 10 | 20 |

A quantidade declarada é mantida em `palletQuantity`.

O cálculo diário utiliza `dailyPallets`, enquanto o cálculo por faixa horária considera `effectivePallets`.

Dessa forma, a carga não paletizada conta em dobro na ocupação do horário, sem alterar a quantidade utilizada no cálculo diário.

### Envio de e-mails

O cadastro do agendamento e o registro da notificação são realizados na mesma transação.

O envio efetivo acontece posteriormente, por meio do processador da outbox.

| Campo | Significado |
|---|---|
| `emailQueued` | A mensagem foi registrada para envio. |
| `emailSent` | O servidor SMTP confirmou o envio. |

Por isso, é normal que uma resposta de criação apresente `emailQueued: true` e `emailSent: false`. Isso significa que a mensagem está aguardando processamento, não necessariamente que houve uma falha.

Mais detalhes estão disponíveis no [ADR-003](docs/arquitetura/ADR-003-outbox-transacional-de-email.md).

---

## Segurança e controle de acesso

A segurança foi considerada em diferentes partes da aplicação, desde a autenticação até o registro das operações realizadas.

Entre os mecanismos implementados estão:

- Access token com validade de 15 minutos.
- Refresh token rotativo e de uso único.
- Cookies `HttpOnly`.
- Proteção contra CSRF.
- Rate limiting para tentativas de login.
- Bloqueio temporário progressivo.
- BCrypt para armazenamento de senhas.
- Refresh tokens armazenados somente como hash SHA-256.
- Troca obrigatória da senha inicial.
- Validação de configurações sensíveis na inicialização.
- Registro de eventos de segurança.
- Tratamento de conflitos de dados e operações simultâneas.

A aplicação também evita expor tokens, senhas e detalhes internos de exceções nas respostas da API.

O detalhamento dos mecanismos, evidências e riscos residuais está disponível no [`SECURITY_REPORT.md`](SECURITY_REPORT.md).

---

## Banco de dados

O projeto utiliza PostgreSQL 16 para armazenar os dados.

A escolha permite trabalhar com integridade referencial, transações, índices, constraints e recursos importantes para um sistema que precisa manter consistência nas operações de agendamento.

As alterações no banco são controladas pelo Flyway. O Hibernate utiliza `ddl-auto=validate`, verificando se a estrutura existente corresponde ao mapeamento da aplicação, sem assumir a responsabilidade de criar ou modificar o schema.

### Histórico de migrações

| Migração | Descrição |
|---|---|
| V1 | Estrutura inicial das tabelas e índices. |
| V2 | Dados iniciais de referência. |
| V3 | Adequação dos agendamentos, identificação única dos pedidos e cancelamento. |
| V4 | Configurações administrativas e regras de integridade. |
| V5 | Sessões de autenticação e integridade adicional. |
| V6 | Estrutura da outbox para envio de e-mails. |
| V7 | Estado de cancelamento na outbox de e-mails. |

---

## Estrutura do projeto

```text
.
├── backend/
│   ├── src/main/java/
│   │   └── com/redekrill/scheduling/
│   ├── src/main/resources/
│   │   ├── application*.yml
│   │   ├── db/migration/
│   │   └── templates/
│   ├── src/test/java/
│   ├── src/test/resources/
│   ├── pom.xml
│   ├── mvnw
│   ├── mvnw.cmd
│   └── Dockerfile
│
├── frontend/
│   ├── src/
│   │   ├── router/
│   │   ├── stores/
│   │   ├── services/
│   │   ├── layouts/
│   │   └── views/
│   ├── e2e/
│   ├── vite.config.js
│   ├── playwright.config.js
│   ├── nginx.conf
│   └── Dockerfile
│
├── docs/
│   ├── arquitetura/
│   ├── operacao/
│   ├── entrega/
│   └── VALIDACAO_FINAL.md
│
├── .github/
│   └── workflows/
│
├── docker-compose.yml
├── .env.example
├── README.md
└── SECURITY_REPORT.md
```

---

## Como executar o projeto

Existem duas maneiras de executar o sistema: utilizando Docker Compose ou iniciando o backend e o frontend separadamente.

### Requisitos

Antes de começar, tenha instalado:

- JDK 21.
- Node.js `^20.19.0` ou `>= 22.12.0` (requisito do Vite 8; ambiente de validação usou Node 26).
- npm.
- PostgreSQL 16 ou Docker Compose.
- Um servidor SMTP para testar as notificações por e-mail.

**Observação:** a execução com Docker não foi validada no ambiente de desenvolvimento utilizado. A execução local sem Docker foi a trilha validada nessa máquina.

### 1. Preparar as variáveis de ambiente

Na raiz do projeto, copie o arquivo de exemplo:

```bash
cp .env.example .env
```

No Windows PowerShell, também é possível utilizar:

```powershell
Copy-Item .env.example .env
```

Configure as credenciais necessárias, incluindo:

```env
POSTGRES_PASSWORD=sua_senha_forte
JWT_SECRET=seu_segredo_aleatorio
```

Para criar o primeiro usuário administrador em ambiente de desenvolvimento:

```env
SEED_ADMIN_ENABLED=true
SEED_ADMIN_PASSWORD=sua_senha_forte_com_pelo_menos_12_caracteres
SPRING_PROFILES_ACTIVE=dev
```

Não existem mais credenciais administrativas padrão. A senha precisa atender à política de segurança definida pelo projeto.

### 2. Executar com Docker Compose

Para iniciar o ambiente de desenvolvimento:

```bash
SPRING_PROFILES_ACTIVE=dev docker compose --profile dev up --build
```

Para iniciar o ambiente de produção:

```bash
docker compose up --build
```

Serviços disponíveis no ambiente de desenvolvimento:

| Serviço | Endereço |
|---|---|
| Frontend | http://localhost:8081 |
| Backend | http://localhost:8080 |
| Swagger UI | http://localhost:8081/swagger-ui.html |
| MailHog | http://localhost:8025 |
| PostgreSQL | localhost:5432 |

Para encerrar:

```bash
docker compose down
```

Para remover também os dados persistidos do PostgreSQL:

```bash
docker compose down -v
```

**Atenção:** o perfil de produção utiliza cookies com a flag `Secure`, portanto a aplicação deve ser acessada por HTTPS. Para testes locais em HTTP, utilize o perfil `dev`.

### 3. Executar sem Docker — Windows

Se preferir iniciar os serviços separadamente, siga os passos abaixo.

O PostgreSQL 16 precisa estar instalado e funcionando, com as credenciais configuradas no ambiente.

Na raiz do projeto, execute o script de desenvolvimento:

```powershell
.\start-dev.ps1
```

Esse script lê as variáveis do `.env`, prepara as configurações necessárias e inicia o backend utilizando o Maven Wrapper.

Em seguida, abra outro terminal e inicie o frontend:

```powershell
cd frontend
npm install
npm run dev
```

A aplicação ficará normalmente disponível em:

http://localhost:5173

Se a porta estiver ocupada, o Vite poderá utilizar outra porta. Nesse caso, confira a configuração `CORS_ALLOWED_ORIGINS` do backend para garantir que a origem utilizada esteja autorizada.

### 4. Executar sem Docker — Linux/macOS

Para iniciar o backend:

```bash
cd backend
JAVA_HOME=/caminho/para/jdk21 ./mvnw spring-boot:run
```

Para iniciar o frontend:

```bash
cd frontend
npm install
npm run dev
```

---

## Configurações disponíveis

As principais variáveis de ambiente estão descritas abaixo. Os valores apresentados são referências de configuração, não credenciais reais.

| Variável | Finalidade |
|---|---|
| `DB_URL` | Endereço JDBC do PostgreSQL. |
| `DB_USERNAME` / `DB_PASSWORD` | Credenciais do banco. |
| `SPRING_PROFILES_ACTIVE` | Perfil de execução. |
| `JWT_SECRET` | Segredo utilizado na assinatura dos JWTs. |
| `JWT_ACCESS_EXPIRATION` | Validade do access token. |
| `JWT_REFRESH_EXPIRATION` | Validade do refresh token. |
| `AUTH_COOKIE_SECURE` | Define a flag Secure dos cookies. |
| `AUTH_COOKIE_SAME_SITE` | Política SameSite dos cookies. |
| `LOGIN_MAX_ATTEMPTS_ACCOUNT` | Limite de tentativas por conta. |
| `LOGIN_MAX_ATTEMPTS_IP` | Limite de tentativas por IP. |
| `CORS_ALLOWED_ORIGINS` | Origens permitidas. |
| `MAIL_HOST` / `MAIL_PORT` | Servidor SMTP. |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | Credenciais SMTP. |
| `EMAIL_OUTBOX_ENABLED` | Ativa o processador da outbox. |
| `EMAIL_OUTBOX_MAX_ATTEMPTS` | Quantidade máxima de tentativas de envio. |
| `SEED_ADMIN_ENABLED` | Permite criar o administrador inicial. |
| `SEED_ADMIN_PASSWORD` | Senha do administrador inicial. |
| `SPRINGDOC_ENABLED` | Habilita a documentação Swagger. |

A lista completa, com valores padrão e descrições adicionais, está disponível no arquivo `.env.example` e no [RUNBOOK](docs/operacao/RUNBOOK.md).

---

## API e documentação

A aplicação disponibiliza uma API REST para comunicação entre o frontend e o backend.

A documentação interativa é gerada com Springdoc OpenAPI.

Para habilitar o Swagger, configure:

```env
SPRINGDOC_ENABLED=true
```

### Principais endpoints

**Autenticação**

| Método | Endpoint | Descrição |
|---|---|---|
| GET | `/api/auth/csrf` | Obtém o token CSRF. |
| POST | `/api/auth/login` | Realiza o login. |
| POST | `/api/auth/refresh` | Renova a sessão. |
| POST | `/api/auth/logout` | Encerra a sessão. |
| POST | `/api/auth/change-password` | Altera a senha. |
| GET | `/api/auth/me` | Consulta o usuário autenticado. |

**Agendamentos**

| Método | Endpoint | Descrição |
|---|---|---|
| GET | `/api/appointments` | Lista agendamentos. |
| GET | `/api/appointments/{id}` | Consulta um agendamento. |
| GET | `/api/appointments/capacity` | Consulta a ocupação. |
| POST | `/api/appointments` | Cria um agendamento. |
| PUT | `/api/appointments/{id}` | Atualiza um agendamento. |
| DELETE | `/api/appointments/{id}` | Realiza o cancelamento lógico. |

**Relatórios e administração**

| Método | Endpoint | Descrição |
|---|---|---|
| GET | `/api/reports/appointments` | Consulta relatórios por período. |
| GET | `/api/time-slots` | Lista faixas horárias. |
| GET | `/api/palletization-types` | Lista tipos de paletização. |
| GET | `/api/truck-types` | Lista tipos de veículos. |
| GET | `/api/users` | Consulta usuários. |
| GET | `/api/audit-logs` | Consulta registros de auditoria. |
| GET | `/actuator/health` | Verifica a saúde da aplicação. |
| GET | `/actuator/info` | Consulta informações da aplicação. |
| GET | `/actuator/prometheus` | Disponibiliza métricas. |

As operações administrativas de criação, edição e exclusão são protegidas pelas permissões correspondentes.

---

## Testes e qualidade

A qualidade do sistema é acompanhada por testes automatizados, análise de cobertura e verificações estáticas.

### Backend

Para executar os testes:

```bash
cd backend
./mvnw -B clean verify
```

No Windows PowerShell:

```powershell
cd backend
.\mvnw.cmd -B clean verify
```

Resultado registrado em 03/11/2026 (re-execução à noite: `mvnw test` às 23:19 e `mvnw clean verify` às 23:20 — BUILD SUCCESS):

| Métrica | Resultado |
|---|---:|
| Testes aprovados | 214 |
| Falhas | 0 |
| Erros | 0 |
| Testes ignorados | 0 |
| Cobertura do gate dos pacotes críticos (linhas) | 98,85% (516/522) |
| Cobertura global de linhas | 87,61% |
| Cobertura global de branches | 71,43% |

A exigência do gate é de 80% em linhas nos quatro pacotes críticos, e o resultado ficou acima desse valor. O registro de 02/11 (125 testes, 77,6% global) é histórico; a suíte cresceu em 03/11 com testes de JWT/sessão, rate limiting, troca de senha e do processador da outbox — as mesmas classes citadas pelos achados `SEC-02` e `SEC-14` do relatório de segurança, hoje cobertas (ver `SECURITY_REPORT.md` §11).

O relatório do JaCoCo pode ser encontrado em:

```text
backend/target/site/jacoco/index.html
```

Os testes incluem regras de negócio, autenticação, casos de uso, arquitetura e integração HTTP utilizando PostgreSQL embarcado.

### Frontend

Na pasta `frontend`, os seguintes comandos permitem validar a aplicação:

```bash
npm run lint
npm run format:check
npm run typecheck
npm test
npm run test:e2e
npm run build
```

Essas verificações cobrem qualidade do código, tipagem, testes unitários, testes de interface e compilação de produção.

### Análise de dependências

Para verificar dependências do backend:

```bash
cd backend
./mvnw -B -Psecurity org.owasp:dependency-check-maven:check
```

Para verificar dependências do frontend:

```bash
cd frontend
npm audit --omit=dev
npm audit
```

Resultado registrado em 03/11/2026:

- OWASP Dependency-Check do backend: **BUILD SUCCESS — 0 vulneráveis em 86 componentes** (plugin 12.2.2, `failBuildOnCVSS=7`, cache local do NVD com 911 registros, sem `NVD_API_KEY`; relatório em `backend/target/dependency-check/`). Ressalva: a frescura da base vem do cache local; no CI a varredura roda agendada com a chave.
- Frontend (`npm audit`): **0 vulnerabilidades** (produção e desenvolvimento) — as 12 moderadas anteriores, todas na cadeia de dev-dependencies (`glob`/`minimatch`), foram eliminadas com `overrides` (`glob ^14.0.2`, `minimatch ^11.0.1`) em `frontend/package.json`, sem subir majors de ferramentas.
- Swagger UI fixado no webjar **5.34.1** (embarca DOMPurify 3.4.17, sem as CVEs do 3.4.16 que o springdoc trazia via 5.34.0) — `backend/pom.xml` documenta o acoplamento.

Os resultados e as limitações estão documentados em [`SECURITY_REPORT.md`](SECURITY_REPORT.md).

---

## Integração contínua

O projeto possui uma configuração de CI com GitHub Actions.

O workflow contempla:

- Build e testes do backend.
- Lint e formatação do frontend.
- Verificação de tipos.
- Testes automatizados.
- Build de produção.
- Testes e2e.
- Validação da configuração Docker Compose.
- Verificação periódica de dependências.

O Dependabot também está configurado para acompanhar atualizações.

A configuração está disponível em:

```text
.github/workflows/ci.yml
.github/dependabot.yml
```

A existência do workflow não significa que a execução atual já foi validada. A primeira execução útil após as últimas alterações ainda precisa ser conferida.

---

## Documentação complementar

Além deste README, o projeto possui documentos que aprofundam aspectos específicos da implementação.

| Documento | Conteúdo |
|---|---|
| [SECURITY_REPORT.md](SECURITY_REPORT.md) | Análise de segurança, evidências e riscos identificados. |
| [docs/arquitetura/](docs/arquitetura/README.md) | Decisões arquiteturais e respectivos ADRs. |
| [RUNBOOK.md](docs/operacao/RUNBOOK.md) | Instruções de operação, configuração e solução de problemas. |
| [docs/entrega/](docs/entrega/) | Materiais de apresentação, PDF e roteiro de demonstração. |
| [VALIDACAO_FINAL.md](docs/VALIDACAO_FINAL.md) | Resultados de validação e itens ainda sem evidência. |
| [AUDITORIA_TECNICA_REDE_KRILL.md](AUDITORIA_TECNICA_REDE_KRILL.md) | Histórico da auditoria técnica do projeto. |

---

## Limitações e pontos de atenção

O desenvolvimento também envolveu a identificação de pontos que ainda precisam de validação ou evolução.

Entre eles:

- A execução com Docker não foi validada no ambiente de desenvolvimento.
- O workflow de CI ainda precisa demonstrar uma execução bem-sucedida sobre o estado atual do código.
- O rate limiting utiliza armazenamento em memória e precisa de uma solução compartilhada para ambientes com múltiplas instâncias.
- O processador da outbox precisa de melhorias para cenários com múltiplas instâncias.
- Alguns fluxos ainda precisam de cobertura adicional de testes ponta a ponta.
- A política de retenção de informações pessoais nos registros de auditoria e na outbox ainda precisa ser definida.

Esses pontos estão documentados para que possam ser acompanhados sem confundir funcionalidades implementadas com validações ainda pendentes.

---

## Considerações finais

O Rede Krill Scheduling foi desenvolvido com o objetivo de transformar um processo operacional de agendamento em uma aplicação organizada, segura e de fácil manutenção.

Mais do que implementar telas e operações de cadastro, o projeto busca demonstrar a importância de regras de negócio bem definidas, integridade dos dados, controle de acesso, testes automatizados e documentação técnica.

O resultado reúne uma aplicação web completa em sua estrutura, com backend e frontend separados, integração com PostgreSQL, mecanismos de segurança e ferramentas para acompanhar a qualidade do software.

O projeto também representa uma oportunidade de aplicar conhecimentos de desenvolvimento Java, Spring Boot, Vue, banco de dados, arquitetura de software e engenharia de qualidade em um cenário próximo de uma necessidade real de negócio.