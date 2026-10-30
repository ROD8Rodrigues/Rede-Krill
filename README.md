# Rede Krill — Agendamento de Caminhões

Sistema desenvolvido para organizar o agendamento de caminhões e acompanhar a movimentação de pallets no centro de distribuição da Rede Krill.

A aplicação permite cadastrar agendamentos, consultar horários, acompanhar a capacidade de recebimento, enviar confirmações por e-mail e consultar registros de operações.

## Tecnologias utilizadas

**Backend**
- Java 21
- Spring Boot 3.3.5
- Spring Security
- Spring Data JPA
- PostgreSQL 16
- Flyway
- JWT e BCrypt
- Spring Mail e Thymeleaf

**Frontend**
- Vue 3
- Vite 5
- Vue Router
- Pinia
- Tailwind CSS
- PrimeVue 4
- Axios

**Outras ferramentas**
- Docker e Docker Compose
- Nginx
- MailHog
- Maven
- JUnit 5, Mockito, AssertJ e ArchUnit
- JaCoCo

## Funcionalidades

Atualmente, o projeto conta com:

- Login com autenticação JWT.
- Controle de acesso para administradores e usuários.
- Cadastro, consulta, edição e exclusão de agendamentos.
- Cálculo de pallets conforme o tipo de paletização.
- Consulta de capacidade por dia e faixa horária.
- Envio de confirmação por e-mail ao fornecedor.
- Registro de operações em log de auditoria.
- Consulta de agendamentos por período.
- Gerenciamento de usuários.
- Cadastro e gerenciamento de tipos de caminhão.
- Documentação da API pelo Swagger.
- Interface web com login e navegação conforme o perfil de acesso.

A interface ainda precisa receber as telas completas de agendamentos, relatórios, usuários e auditoria.

## Regras de negócio

### Horários de recebimento

O sistema trabalha com quatro faixas horárias:

| ID | Horário |
|---|---|
| 1 | 08:00 às 10:00 |
| 2 | 10:00 às 12:00 |
| 3 | 13:00 às 15:00 |
| 4 | 15:00 às 17:00 |

### Capacidade de pallets

Os limites utilizados pelo sistema são:

- 480 pallets por dia.
- 120 pallets por faixa horária.

Esses valores servem para acompanhamento da capacidade, não para impedir agendamentos.

Quando algum limite é ultrapassado, o sistema retorna um aviso no campo `warnings`, mas mantém o agendamento normalmente.

### Cálculo de pallets

O cálculo depende do tipo de paletização informado:

| Tipo | Quantidade informada | Quantidade considerada |
|---|---:|---:|
| Paletizado | 10 | 10 |
| Não paletizado | 10 | 20 |

O sistema mantém dois campos:

- `palletQuantity`: quantidade informada no cadastro.
- `effectivePallets`: quantidade utilizada no cálculo da capacidade.

### Permissões

Existem dois perfis de acesso:

**ADMIN**
- Gerenciar agendamentos.
- Consultar relatórios.
- Gerenciar usuários.
- Gerenciar tipos de caminhão.
- Consultar registros de auditoria.
- Consultar dados de referência.

**USER**
- Consultar agendamentos.
- Consultar relatórios.
- Consultar tipos de caminhão e dados de referência.
- Consultar os próprios dados de usuário.

### Envio de e-mails

Depois que um agendamento é salvo, o sistema envia uma confirmação ao fornecedor.

Caso ocorra algum problema no envio, a falha é registrada no log. Isso não cancela nem desfaz o agendamento.

## Organização do projeto

O repositório está dividido em duas partes principais:

```text
Rede-Krill/
│
├── backend/
│   └── src/
│
├── frontend/
│   └── src/
│
├── docker-compose.yml
├── .env.example
└── README.md
```

### Backend

O backend utiliza uma estrutura baseada em Clean Architecture.

```text
com.redekrill.scheduling
│
├── domain/
│   ├── model/
│   ├── policy/
│   ├── repository/
│   └── exception/
│
├── application/
│   ├── dto/
│   ├── port/
│   ├── service/
│   └── usecase/
│
├── infrastructure/
│   ├── persistence/
│   ├── security/
│   ├── config/
│   ├── email/
│   ├── seed/
│   └── transaction/
│
└── presentation/
    ├── controller/
    ├── request/
    ├── response/
    ├── mapper/
    └── exception/
```

A separação permite manter as regras de negócio independentes dos detalhes de banco de dados, autenticação e comunicação HTTP.

### Frontend

A estrutura principal do frontend é:

```text
frontend/src/
│
├── layouts/
│   └── AppLayout.vue
│
├── views/
│   ├── LoginView.vue
│   ├── DashboardView.vue
│   └── UnderConstructionView.vue
│
├── router/
│   └── index.js
│
├── stores/
│   └── auth.js
│
├── services/
│   └── api.js
│
└── style.css
```

O Vue Router controla a navegação, o Pinia mantém os dados de autenticação e o Axios faz a comunicação com a API.

## Como executar

### Requisitos

Antes de iniciar, é necessário ter instalado:

- Docker Desktop
- Docker Compose

Para executar os serviços separadamente, também serão necessários:

- JDK 21
- Node.js 18 ou superior
- npm

### Executando com Docker

Na pasta principal do projeto, execute:

```bash
docker compose up --build
```

Se quiser utilizar as configurações do arquivo de exemplo:

```bash
cp .env.example .env
docker compose up --build
```

No PowerShell do Windows:

```powershell
Copy-Item .env.example .env
docker compose up --build
```

Aguarde os serviços iniciarem. Na primeira execução, o Flyway será responsável por criar as tabelas e inserir os dados iniciais.

### Endereços locais

| Serviço | Endereço |
|---|---|
| Frontend | http://localhost:5173 |
| Backend | http://localhost:8080 |
| Swagger UI | http://localhost:5173/swagger-ui.html |
| MailHog | http://localhost:8025 |
| PostgreSQL | localhost:5432 |

Para encerrar:

```bash
docker compose down
```

Para encerrar e remover também os dados persistidos do banco:

```bash
docker compose down -v
```

**Atenção:** o segundo comando apaga os volumes do Docker, incluindo os dados do PostgreSQL.

## Executando em desenvolvimento

Também é possível iniciar o backend e o frontend separadamente.

### Banco de dados e MailHog

Na raiz do projeto:

```bash
docker compose up postgres mailhog
```

### Backend

Entre na pasta:

```bash
cd backend
```

Linux:

```bash
./mvnw spring-boot:run
```

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

A API ficará disponível em:

```text
http://localhost:8080
```

### Frontend

Em outro terminal:

```bash
cd frontend
npm install
npm run dev
```

A interface ficará disponível em:

```text
http://localhost:5173
```

O Vite encaminha as requisições da API para o backend. Caso seja necessário utilizar outro endereço:

```bash
VITE_PROXY_TARGET=http://outro-host:8080 npm run dev
```

## Configurações

As principais variáveis de ambiente utilizadas pelo projeto são:

| Variável | Valor padrão |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/rede_krill` |
| `DB_USERNAME` | `redekrill` |
| `DB_PASSWORD` | `redekrill` |
| `JWT_SECRET` | Segredo de desenvolvimento |
| `JWT_EXPIRATION` | `86400s` |
| `MAIL_HOST` | `localhost` |
| `MAIL_PORT` | `1025` |
| `MAIL_FROM` | `nao-responda@redekrill.com` |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` |
| `SEED_ADMIN_NAME` | `Administrador` |
| `SEED_ADMIN_EMAIL` | `admin@redekrill.com` |
| `SEED_ADMIN_PASSWORD` | `Admin@12345` |

Os valores acima são destinados ao ambiente local. Antes de qualquer implantação em produção, altere as credenciais e configure um segredo JWT próprio e seguro.

## Usuário inicial

Na primeira inicialização, o sistema cria um usuário administrador caso ele ainda não exista.

```text
E-mail: admin@redekrill.com
Senha: Admin@12345
```

Os dados podem ser alterados pelas variáveis `SEED_ADMIN_*`.

Não utilize essas credenciais em um ambiente de produção.

## API

A API utiliza endpoints REST e autenticação por JWT.

A documentação interativa está disponível no Swagger.

### Autenticação

| Método | Endpoint | Acesso |
|---|---|---|
| POST | `/api/auth/login` | Público |
| GET | `/api/auth/me` | Autenticado |

### Agendamentos

| Método | Endpoint | Acesso |
|---|---|---|
| GET | `/api/appointments` | ADMIN / USER |
| GET | `/api/appointments/{id}` | ADMIN / USER |
| POST | `/api/appointments` | ADMIN |
| PUT | `/api/appointments/{id}` | ADMIN |
| DELETE | `/api/appointments/{id}` | ADMIN |

A listagem permite filtros por período e paginação.

### Relatórios

| Método | Endpoint | Acesso |
|---|---|---|
| GET | `/api/reports/appointments?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD` | ADMIN / USER |

O relatório retorna os dados dos agendamentos e os totais calculados para o período.

### Dados de referência

| Método | Endpoint | Acesso |
|---|---|---|
| GET | `/api/time-slots` | ADMIN / USER |
| GET | `/api/palletization-types` | ADMIN / USER |

### Tipos de caminhão

| Método | Endpoint | Acesso |
|---|---|---|
| GET | `/api/truck-types` | ADMIN / USER |
| POST | `/api/truck-types` | ADMIN |
| PUT | `/api/truck-types/{id}` | ADMIN |
| DELETE | `/api/truck-types/{id}` | ADMIN |

A exclusão de um tipo de caminhão depende da existência de agendamentos vinculados.

### Usuários e auditoria

| Método | Endpoint | Acesso |
|---|---|---|
| GET | `/api/users?page=&size=` | ADMIN |
| POST | `/api/users` | ADMIN |
| PUT | `/api/users/{id}` | ADMIN |
| GET | `/api/audit-logs?page=&size=` | ADMIN |

## Banco de dados

O projeto utiliza PostgreSQL 16 e Flyway para controlar as alterações no banco.

As migrações estão em:

```text
backend/src/main/resources/db/migration/
```

Arquivos principais:

- `V1__schema.sql`: criação das tabelas, índices e constraints.
- `V2__reference_data.sql`: inserção dos dados iniciais de referência.

O Hibernate utiliza `ddl-auto: validate`, portanto a estrutura do banco é controlada pelas migrações do Flyway.

## Segurança

A autenticação e o controle de acesso são implementados com Spring Security.

O sistema utiliza:

- JWT assinado com HS256.
- BCrypt para armazenamento de senhas.
- Controle de permissões por perfil.
- Configuração de CORS.
- Validação de dados recebidos pela API.
- Tratamento centralizado de exceções.
- Registro de operações para auditoria.

O token possui validade configurável pela variável `JWT_EXPIRATION`.

## Testes

Os testes do backend podem ser executados com Maven.

Para executar a suíte:

```bash
cd backend
./mvnw test
```

Para executar os testes e as verificações de cobertura:

```bash
./mvnw verify
```

No Windows, utilize `.\mvnw.cmd` no lugar de `./mvnw`.

A cobertura mínima configurada é de 80% de linhas nos pacotes críticos:

- `domain.policy`
- `application.usecase.appointment`
- `application.usecase.auth`
- `application.service`

A suíte inclui testes de regras de negócio, autenticação, agendamentos, usuários, auditoria, integração da API e arquitetura.

Os testes de integração utilizam MockMvc e PostgreSQL embarcado pelo Zonky, sem exigir uma instância local do PostgreSQL ou Docker.

Para verificar o relatório de cobertura:

```text
backend/target/site/jacoco/index.html
```

Também é possível verificar a compilação do frontend:

```bash
cd frontend
npm run build
```

## Melhorias pendentes

As próximas etapas do desenvolvimento são:

- Finalizar as telas de agendamento.
- Implementar os filtros e formulários da interface.
- Desenvolver a área de relatórios.
- Criar as telas de gerenciamento de usuários.
- Implementar a consulta de auditoria pela interface.
- Adicionar testes end-to-end dos principais fluxos.

---

Projeto desenvolvido como parte do desafio técnico da Rede Krill.