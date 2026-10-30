# Rede Krill — Agendamento de Caminhões

Sistema para agendamento de caminhões, controle **não bloqueante** de capacidade de pallets,
notificação automática ao fornecedor por e-mail, auditoria de operações e relatórios por período.

O repositório contém:

- **Backend** em Java 21 + Spring Boot 3.3.5, seguindo princípios de Clean Architecture.
- **Frontend** em Vue 3 + Vite, com autenticação JWT, layout responsivo e navegação por perfil.
- **PostgreSQL 16** com migrações Flyway e **MailHog** para captura dos e-mails em desenvolvimento.
- **Docker Compose** para subir toda a aplicação com um único comando.

---

## Sumário

- [Funcionalidades](#funcionalidades)
- [Regras de negócio](#regras-de-negócio)
- [Stack](#stack)
- [Arquitetura](#arquitetura)
- [Como executar com Docker](#como-executar-com-docker)
- [Desenvolvimento local](#desenvolvimento-local)
- [Credenciais iniciais](#credenciais-iniciais)
- [API](#api)
- [Testes e cobertura](#testes-e-cobertura)
- [Banco de dados](#banco-de-dados)
- [Segurança](#segurança)
- [Decisões de projeto](#decisões-de-projeto)
- [Próximos passos](#próximos-passos)

---

## Funcionalidades

| Funcionalidade | Situação |
| --- | --- |
| Autenticação com JWT e perfis `ADMIN` / `USER` | Concluído |
| CRUD de agendamentos | Concluído no backend |
| Cálculo de pallets efetivos conforme paletização | Concluído |
| Avisos de capacidade diária e por faixa (sem bloqueio) | Concluído |
| Envio automático de e-mail ao fornecedor | Concluído no backend |
| Auditoria de criação, atualização e exclusão | Concluído no backend |
| Relatório de agendamentos por período | Concluído no backend |
| CRUD de usuários e tipos de caminhão | Concluído no backend |
| Documentação OpenAPI / Swagger UI | Concluído |
| Frontend com login funcional e layout | Concluído |
| Telas completas de agendamentos, relatórios, usuários e auditoria | Próxima entrega |
| Testes unitários, de integração e de arquitetura com JaCoCo ≥ 80% | Concluído |

---

## Regras de negócio

### Faixas horárias

Existem exatamente **quatro faixas de 2 horas**:

| ID | Faixa |
| --- | --- |
| 1 | 08:00 – 10:00 |
| 2 | 10:00 – 12:00 |
| 3 | 13:00 – 15:00 |
| 4 | 15:00 – 17:00 |

### Capacidade

- Limite **diário**: **480 pallets**.
- Limite por **faixa horária**: **120 pallets**.

Os limites são **apenas informativos**. Ao ultrapassá-los, o agendamento é gravado normalmente
e a resposta inclui avisos em `warnings`. **Nenhuma operação é bloqueada.**

### Pallets efetivos

A quantidade considerada no cálculo de capacidade depende do tipo de paletização:

| Tipo | Multiplicador | Exemplo: 10 pallets informados | Efetivo |
| --- | --- | --- | --- |
| Paletizado | 1 | 10 | 10 |
| Não Paletizado | 2 | 10 | 20 |

O campo `palletQuantity` guarda a quantidade informada e `effectivePallets` guarda a quantidade
usada nos limites de capacidade.

### Perfis de acesso

| Operação | `ADMIN` | `USER` |
| --- | --- | --- |
| Login e consulta do próprio usuário | Sim | Sim |
| Listar/consultar agendamentos | Sim | Sim |
| Criar/editar/excluir agendamentos | Sim | Não |
| Consultar relatórios | Sim | Sim |
| Consultar tipos de caminhão e referências | Sim | Sim |
| Criar/editar/excluir tipos de caminhão | Sim | Não |
| Gerenciar usuários | Sim | Não |
| Visualizar logs de auditoria | Sim | Não |

### E-mail automático

Após o commit da criação de um agendamento, o backend envia um e-mail de confirmação ao
fornecedor. Se o envio falhar, a falha é registrada em log e **o agendamento permanece salvo**.

---

## Stack

### Backend

- Java 21
- Spring Boot 3.3.5
- Spring Web, Spring Data JPA, Spring Security
- Bean Validation
- PostgreSQL 16 + Flyway
- JWT (`jjwt` 0.12.6) + BCrypt
- Spring Mail + Thymeleaf (template HTML do e-mail)
- springdoc OpenAPI 2.6.0
- JUnit 5, Mockito, AssertJ, ArchUnit, JaCoCo

### Frontend

- Vue 3 (Composition API)
- Vite 5
- Vue Router 4
- Pinia
- Tailwind CSS 3
- PrimeVue 4 + PrimeIcons
- Axios

### Infraestrutura

- Docker + Docker Compose
- nginx para servir o frontend e encaminhar `/api` ao backend
- MailHog para inspeção dos e-mails em desenvolvimento

---

## Arquitetura

O backend segue Clean Architecture, com dependências apontando sempre para dentro:

```text
presentation  →  application  →  domain
      │                ▲
      └── infrastructure ┘
```

```text
backend/src/main/java/com/redekrill/scheduling
├── domain
│   ├── model            # Entidades e regras puras (Appointment, User, TimeSlot...)
│   ├── policy           # CapacityPolicy: limites de 480/120 sem bloqueio
│   ├── repository       # Portas de persistência
│   └── exception        # Exceções de domínio
├── application
│   ├── dto              # Commands e Results
│   ├── port             # PasswordHasher, TokenIssuer, EmailNotifier, TransactionRunner
│   ├── service          # AuditService
│   └── usecase          # Casos de uso (auth, appointment, user, trucktype, audit, reference)
├── infrastructure
│   ├── persistence      # Entidades JPA, repositórios Spring Data, mappers e adapters
│   ├── security         # Filtro JWT, BCrypt, SecurityConfig
│   ├── config           # Wiring dos casos de uso, OpenAPI, Clock, propriedades
│   ├── email            # Adapter SMTP + template Thymeleaf
│   ├── seed             # Administrador inicial
│   └── transaction      # Implementação de TransactionRunner
└── presentation
    ├── controller       # Endpoints REST
    ├── request          # DTOs de entrada com Bean Validation
    ├── response         # DTOs de saída
    ├── mapper           # Conversão application → HTTP
    └── exception        # GlobalExceptionHandler
```

O domínio e a aplicação não dependem de Spring, JPA ou de qualquer detalhe de infraestrutura.
Essa regra é verificada automaticamente pelo `ArchitectureTest` (ArchUnit).

### Frontend

```text
frontend/src
├── layouts/AppLayout.vue       # Sidebar, cabeçalho e navegação por perfil
├── views/
│   ├── LoginView.vue           # Login funcional integrado à API
│   ├── DashboardView.vue       # Painel inicial
│   └── UnderConstructionView.vue
├── router/index.js             # Rotas e guards de autenticação/autorização
├── stores/auth.js              # Pinia: token, usuário e login/logout
├── services/api.js             # Axios + interceptor JWT + tratamento de 401
└── style.css                   # Tailwind + camadas do PrimeVue
```

---

## Como executar com Docker

### Pré-requisitos

- Docker Desktop (ou Docker Engine + Compose)

### Passos

```bash
cp .env.example .env
docker compose up --build
```

No Windows (PowerShell):

```powershell
Copy-Item .env.example .env
docker compose up --build
```

Aguarde o PostgreSQL ficar saudável. O Flyway cria o schema e insere os dados de referência
automaticamente na primeira inicialização.

### Serviços

| Serviço | URL |
| --- | --- |
| Frontend | http://localhost:5173 |
| API | http://localhost:8080 |
| Swagger UI | http://localhost:5173/swagger-ui.html |
| MailHog (e-mails) | http://localhost:8025 |
| PostgreSQL | `localhost:5432` |

Para encerrar:

```bash
docker compose down
```

Para remover também os dados do banco:

```bash
docker compose down -v
```

> O arquivo `.env` é opcional; o Compose possui valores padrão para todas as variáveis.
> Em produção, altere obrigatoriamente `JWT_SECRET` e `SEED_ADMIN_PASSWORD`.

---

## Desenvolvimento local

### Pré-requisitos

- JDK 21
- Maven 3.9+ (ou use o Maven Wrapper incluído)
- Node.js 18+ e npm
- PostgreSQL 16 e MailHog (podem ser iniciados via Docker)

### Infraestrutura mínima

```bash
docker compose up postgres mailhog
```

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

No Windows:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

A API ficará disponível em `http://localhost:8080` e o Swagger em
`http://localhost:8080/swagger-ui.html`.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

O frontend ficará em `http://localhost:5173`. O Vite encaminha `/api`, `/swagger-ui`,
`/swagger-ui.html` e `/v3/api-docs` para `http://localhost:8080`.

Para apontar para outro backend:

```bash
VITE_PROXY_TARGET=http://outro-host:8080 npm run dev
```

### Variáveis principais

| Variável | Padrão | Descrição |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/rede_krill` | URL do PostgreSQL |
| `DB_USERNAME` | `redekrill` | Usuário do banco |
| `DB_PASSWORD` | `redekrill` | Senha do banco |
| `JWT_SECRET` | segredo de desenvolvimento | Chave Base64 com no mínimo 256 bits |
| `JWT_EXPIRATION` | `86400s` | Validade do token |
| `MAIL_HOST` | `localhost` | Servidor SMTP |
| `MAIL_PORT` | `1025` | Porta SMTP (MailHog) |
| `MAIL_FROM` | `nao-responda@redekrill.com` | Remetente dos e-mails |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Origens permitidas |
| `SEED_ADMIN_NAME` | `Administrador` | Nome do admin inicial |
| `SEED_ADMIN_EMAIL` | `admin@redekrill.com` | E-mail do admin inicial |
| `SEED_ADMIN_PASSWORD` | `Admin@12345` | Senha do admin inicial |

---

## Credenciais iniciais

Usuário administrador criado automaticamente na primeira execução:

```text
E-mail: admin@redekrill.com
Senha:  Admin@12345
```

Esses valores podem ser alterados pelas variáveis `SEED_ADMIN_*` antes da primeira subida.
O usuário só é criado se ainda não existir.

---

## API

A documentação interativa completa está no Swagger UI. Resumo dos endpoints:

### Autenticação

| Método | Endpoint | Acesso | Descrição |
| --- | --- | --- | --- |
| `POST` | `/api/auth/login` | Público | Autentica e retorna o JWT |
| `GET` | `/api/auth/me` | Autenticado | Retorna o usuário autenticado |

### Agendamentos

| Método | Endpoint | Acesso | Descrição |
| --- | --- | --- | --- |
| `GET` | `/api/appointments?startDate=&endDate=&page=&size=` | `ADMIN`, `USER` | Lista com filtro por período |
| `GET` | `/api/appointments/{id}` | `ADMIN`, `USER` | Consulta por ID |
| `POST` | `/api/appointments` | `ADMIN` | Cria e envia e-mail |
| `PUT` | `/api/appointments/{id}` | `ADMIN` | Atualiza |
| `DELETE` | `/api/appointments/{id}` | `ADMIN` | Exclui |

### Relatórios

| Método | Endpoint | Acesso | Descrição |
| --- | --- | --- | --- |
| `GET` | `/api/reports/appointments?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD` | `ADMIN`, `USER` | Consolidado por período |

### Dados de referência

| Método | Endpoint | Acesso | Descrição |
| --- | --- | --- | --- |
| `GET` | `/api/time-slots` | `ADMIN`, `USER` | Quatro faixas horárias |
| `GET` | `/api/palletization-types` | `ADMIN`, `USER` | Tipos de paletização |

### Tipos de caminhão

| Método | Endpoint | Acesso | Descrição |
| --- | --- | --- | --- |
| `GET` | `/api/truck-types` | `ADMIN`, `USER` | Lista |
| `POST` | `/api/truck-types` | `ADMIN` | Cria |
| `PUT` | `/api/truck-types/{id}` | `ADMIN` | Atualiza |
| `DELETE` | `/api/truck-types/{id}` | `ADMIN` | Exclui se não houver agendamento vinculado |

### Usuários e auditoria

| Método | Endpoint | Acesso | Descrição |
| --- | --- | --- | --- |
| `GET` | `/api/users?page=&size=` | `ADMIN` | Lista usuários |
| `POST` | `/api/users` | `ADMIN` | Cria usuário |
| `PUT` | `/api/users/{id}` | `ADMIN` | Atualiza usuário |
| `GET` | `/api/audit-logs?page=&size=` | `ADMIN` | Lista logs de auditoria |

### Exemplo: login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@redekrill.com","password":"Admin@12345"}'
```

Resposta:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "user": {
    "id": 1,
    "name": "Administrador",
    "email": "admin@redekrill.com",
    "role": "ADMIN",
    "active": true,
    "createdAt": "2026-02-01T12:00:00Z"
  }
}
```

### Exemplo: criar agendamento

```bash
TOKEN="<token-do-login>"

curl -X POST http://localhost:8080/api/appointments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "supplierName": "Fornecedor Krill",
    "supplierEmail": "fornecedor@example.com",
    "truckPlate": "ABC1D23",
    "date": "2026-03-10",
    "timeSlotId": 1,
    "truckTypeId": 1,
    "palletizationTypeId": 2,
    "palletQuantity": 30
  }'
```

Resposta (trecho):

```json
{
  "appointment": {
    "id": 1,
    "supplierName": "Fornecedor Krill",
    "effectivePallets": 60,
    "palletQuantity": 30,
    "timeSlot": { "id": 1, "startTime": "08:00", "endTime": "10:00" }
  },
  "warnings": [],
  "capacity": {
    "dailyPallets": 60,
    "slotPallets": 60,
    "dailyLimit": 480,
    "slotLimit": 120
  }
}
```

Se os limites forem ultrapassados, `warnings` trará mensagens como
`"Limite diário de 480 pallets excedido: 500 pallets no dia (excedente de 20)."` e o
agendamento continuará sendo criado.

---

## Testes e cobertura

```bash
cd backend
./mvnw test       # executa os testes
./mvnw verify     # testes + JaCoCo + verificação de cobertura
```

O relatório HTML é gerado em:

```text
backend/target/site/jacoco/index.html
```

A cobertura mínima de **80% de linhas** é verificada nos pacotes críticos:

- `domain.policy`
- `application.usecase.appointment`
- `application.usecase.auth`
- `application.service`

A suíte inclui:

- Testes da política de capacidade (limites, excedentes e validações).
- Testes dos casos de uso de agendamento, autenticação, usuários, tipos de caminhão e auditoria.
- Teste de integração da API com `MockMvc` e PostgreSQL embarcado (Zonky), cobrindo login,
  autorização por perfil, criação de agendamento, relatório e auditoria.
- Teste de arquitetura com ArchUnit garantindo o isolamento entre camadas.

Os testes de integração não exigem Docker nem um PostgreSQL local: o Zonky inicia uma instância
embarcada e aplica as migrações Flyway automaticamente.

Frontend:

```bash
cd frontend
npm run build
```

---

## Banco de dados

As migrações Flyway ficam em `backend/src/main/resources/db/migration`:

- `V1__schema.sql`: tabelas `users`, `truck_type`, `palletization_type`, `time_slot`,
  `appointment` e `audit_log`, com índices e constraints.
- `V2__reference_data.sql`: tipos de paletização, quatro faixas horárias e tipos de caminhão
  iniciais.

O Hibernate está configurado com `ddl-auto: validate`: o schema é responsabilidade do Flyway.

---

## Segurança

- Senhas armazenadas com **BCrypt**.
- Autenticação **stateless** com JWT assinado (HS256) e validade configurável.
- Autorização por perfil via Spring Security:
  - `ADMIN`: acesso total.
  - `USER`: leitura de agendamentos, referências e relatórios.
- Rotas públicas limitadas a login, documentação e health/error.
- CORS restrito às origens configuradas.
- Respostas de erro padronizadas, sem stack trace.
- Auditoria registra autor, ação, entidade, identificador e detalhes.

> Para produção, use um segredo JWT forte e exclusivo, senhas iniciais diferentes e HTTPS.

---

## Decisões de projeto

- **Capacidade não bloqueante:** a `CapacityPolicy` retorna avisos e nunca lança exceção para
  excesso de limites, conforme o requisito.
- **Domínio puro:** entidades e regras não conhecem JPA nem Spring. Entidades JPA separadas são
  convertidas por mappers na infraestrutura.
- **Transações via porta:** `TransactionRunner` permite que casos de uso controlem transações sem
  depender do Spring.
- **E-mail após o commit:** o envio ocorre depois do retorno do `TransactionRunner`; falhas de SMTP
  são logadas e não desfazem o agendamento.
- **Referências somente leitura:** tipos de paletização e faixas horárias são seedados e expostos
  apenas para consulta.
- **Relatório por período:** retorna total de agendamentos, total de pallets efetivos e a lista
  detalhada.
- **Frontend preparado para evoluir:** login, guarda de rotas, tratamento de 401, layout e
  navegação por perfil já estão integrados à API.

---

## Próximos passos

1. Implementar as telas de agendamentos (listagem, filtros, formulário e avisos de capacidade).
2. Implementar a tela de relatórios com filtro por período e exportação.
3. Implementar as telas administrativas de usuários, tipos de caminhão e auditoria.
4. Adicionar testes end-to-end do fluxo de agendamento e envio de e-mail.
