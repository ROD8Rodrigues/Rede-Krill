# Arquitetura — Rede Krill Scheduling

Este arquivo explica como o backend está organizado e por quê. Ele foi escrito para quem
abre o projeto pela primeira vez e precisa responder três perguntas: "onde entra a minha
requisição", "onde mora a regra que preciso mexer" e "o que acontece se eu fizer isso
aqui". Cada afirmação abaixo corresponde ao código como ele está hoje — nomes de pacote,
de classe e de arquivo são reais e podem ser abertos enquanto você lê.

As decisões que deram origem a essa forma estão registradas uma por uma em
[docs/arquitetura](arquitetura/), nos ADRs de 001 a 006. Este documento é o mapa geral;
os ADRs são o "por que aquilo e não outra coisa".

---

## 1. O que é Clean Architecture, dito sem rodeio

A ideia central é uma só: **o código que decide as regras do negócio não deve conhecer o
mecanismo que as executa**. "Um agendamento não pode estourar os 120 pallets da faixa" é
uma regra. "Essa regra é conferida numa consulta JPQL contra o Postgres" é um mecanismo.
Se os dois ficam na mesma classe, a regra só é testável com banco ligado, e trocar o banco
arrasta a regra junto.

Daí vem a organização em camadas concêntricas, das regras mais estáveis para as mais
trocaáveis:

```
   domain  ←  application  ←  infrastructure / presentation
 (o quê)      (em que ordem)      (com qual tecnologia)
```

E a regra que segura tudo, conhecida como **regra da dependência**: uma camada só pode
conhecer camadas mais internas. `domain` não sabe que Spring existe. `application` não sabe
que o banco é Postgres. `presentation` não sabe que existe `infrastructure`. Nunca o
contrário.

O que costuma confundir é que a seta do **fluxo** de dados vai de fora para dentro e volta
— request HTTP entra, resultado sai — enquanto a seta de **dependência de código** aponta
sempre para dentro. As duas não são a mesma coisa, e é por isso que a interface do
repositório está na aplicação, mas a implementação está na infraestrutura: assim o chamador
aponta para a interface interna e o framework, para fora.

Neste projeto a variação adotada é a **arquitetura hexagonal** (portos e adaptadores):
`application` declara o que precisa (`application/port`), `infrastructure` oferece como
fazer (`infrastructure.*`), e um arquivo de configuração junta os dois em tempo de boot.

---

## 2. As quatro camadas

Total em `backend/src/main/java/com/redekrill/scheduling`: 191 arquivos `.java` — um na raiz
(`SchedulingApplication`, só o boot) e, nas quatro camadas, `domain` 31, `application` 64,
`infrastructure` 60, `presentation` 35.

### domain (31 arquivos)

O vocabulário e as regras do negócio, em classes planas de Java.

- `model/` — os agregados (`Appointment`, `User`, `TimeSlot`, `TruckType`,
  `PalletizationType`, `AuthSession`, `AuditLog`, `EmailOutboxMessage`), seus enums de
  estado (`AppointmentStatus`, `EmailOutboxStatus`, `PalletizationCode`, `Role`,
  `AuditAction`), o suporte de validação (`DomainValidation`) e o contentor de paginação
  (`PageResult`). São imutáveis: um "update" constrói uma nova instância em vez de
  alterar a existente, e por isso cada método devolve objeto novo.
- `policy/` — as regras que precisam de mais de um objeto para existir: `CapacityPolicy`
  (480 pallets/dia, 120/faixa, com `CapacityAssessment` como resultado) e `PasswordPolicy`
  (mínimo de 12 caracteres, rejeição de senha conhecida).
- `repository/` — as **interfaces** de acesso a dados (`UserRepository`,
  `AppointmentRepository`, `AuthSessionRepository`, `AuditLogRepository`,
  `EmailOutboxRepository`, `TimeSlotRepository`, `TruckTypeRepository`,
  `PalletizationTypeRepository`). Nada de SQL aqui; apenas nomes de método que expressam
  a intenção do negócio (`sumSlotPalletsByDateAndTimeSlot`, `revokeIfActive`).
- `exception/` — `DomainException` e os quatro filhos com status próprio
  (`AuthenticationFailedException`, `ResourceNotFoundException`, `ConflictException`,
  `TooManyRequestsException`).

**Não pode haver aqui:** anotação do Spring, `jakarta.*`, JPA, JSON, Servlet, nada que
venha de fora. Isso é vigiado por teste (seção 5).

### application (64 arquivos)

Os casos de uso: "criar agendamento", "rotacionar sessão", "listar auditoria". Orquestram
a ordem das chamadas, chamam as políticas do domínio e registram auditoria. Não decidem
regra de domínio nem sabem de HTTP.

- `usecase/` — 27 casos de uso agrupados por funcionalidade (`appointment/` 7,
  `reference/` 8, `auth/` 4, `trucktype/` 4, `user/` 3, `audit/` 1) mais `Pagination`, o
  ajuste de page/size usado por todas as rotas paginadas.
- `service/` — os quatro serviços compartilhados por mais de um caso de uso: `AuditService`,
  `AuthSessionService`, `EmailOutboxService`, `LoginAttemptService`.
- `port/` — os seis portos que a aplicação declara e a infraestrutura implementa:
  `TokenIssuer` (+ `IssuedToken`), `RefreshTokenGenerator`, `PasswordHasher`,
  `EmailNotifier`, `TransactionRunner`.
- `dto/` — `Command` de entrada, `Result` de saída e `ApiError`. Um caso de uso recebe um
  command e devolve um result; nunca devolve um agregado do domínio nem recebe um JSON.

**Não pode haver aqui:** dependência de `infrastructure`, de `presentation`, de Spring ou
de `jakarta.*`. Concretamente: os casos de uso não têm `@Service`, não têm
`@Transactional` e não conhecem `HttpServletRequest`.

### infrastructure (60 arquivos)

Tudo que toca o mundo externo: Postgres, SMTP, JWT, BCrypt, cookies de servlet, agendamento
de job. É a única camada com anotação do Spring além da apresentação.

- `persistence/` — quatro pastas de oito arquivos cada: `entity/` (sombras JPA das
  entidades de domínio), `repository/` (interfaces `Spring Data Jpa`), `adapter/` (as
  implementações dos repositórios do domínio) e `mapper/` (conversão entity ⇄ model).
- `security/` — 11 arquivos: cadeia de filtros (`SecurityConfig`,
  `JwtAuthenticationFilter`, `PasswordChangeRequiredFilter`), emissão/validação de token
  (`JwtTokenService`), refresh opaco (`SecureRefreshTokenGenerator`), hash de senha
  (`BCryptPasswordHasherAdapter`, `SecurityBeansConfig`), matcher de Bearer
  (`BearerAuthorizationRequestMatcher`), manipuladores de 401/403
  (`RestAuthenticationEntryPoint`, `RestAccessDeniedHandler`) e o job de limpeza de sessão
  (`AuthSessionCleanupJob`).
- `email/` — `SmtpEmailNotifier` (implementa `EmailNotifier`), `EmailOutboxProcessor`
  (entrega a fila) e `EmailDeliveryException`.
- `transaction/` — `SpringTransactionRunner`, que implementa `TransactionRunner`.
- `config/` — `UseCaseConfig` (montagem dos casos de uso), `SecurityConfig`-auxiliares
  (`ClockConfig`, `OpenApiConfig`), `ProductionConfigurationValidator` e `properties/`
  com os sete registros de configuração (`app.auth`, `app.jwt`, `app.security`, `app.cors`,
  `app.email`, `app.email.outbox`, `app.seed`).
- `observability/` — `CorrelationIdFilter`.
- `seed/` — `DataInitializer`, que cria o primeiro ADMIN.

### presentation (35 arquivos)

O contrato HTTP. Nada de decisão.

- `controller/` — 7: `AuthController`, `AppointmentController`, `ReferenceDataController`,
  `TruckTypeController`, `UserController`, `AuditLogController`, `ReportController`.
- `request/` — 12 records de entrada com Bean Validation.
- `response/` — 13 records de saída, incluindo `PageResponse<T>` como envelope de lista.
- `mapper/` — `WebMapper`: `Result` da aplicação → `Response` do cliente.
- `exception/` — `GlobalExceptionHandler`: exceção → `ApiError`.
- `support/` — `ClientIpResolver`.

**Não pode haver aqui:** dependência de `infrastructure`. Os controllers falam com casos de
uso e com tipos de `application`/`domain` — nunca com um adaptador JPA ou com
`JwtTokenService`.

---

## 3. Como as camadas conversam

Quatro mecanismos, e é só.

**1 — Portos e implementações.** A aplicação declara `TokenIssuer`; `JwtTokenService`
implementa. A aplicação declara `UserRepository`; `JpaUserRepositoryAdapter` implementa. O
caso de uso recebe a interface no construtor. É essa inversão que permite ao
`CreateAppointmentUseCaseTest` existir sem Spring e sem banco.

**2 — Command e Result, no lugar de entidade.** `CreateAppointmentUseCase.execute(CreateAppointmentCommand)`
devolve `AppointmentCommandResult`. Isso tem dois efeitos úteis: o domínio pode evoluir
campos sem mudar a API, e o JSON de resposta não é decidido "por acidente" pela forma do
agregado.

**3 — Dois mappers, cada um no seu lado da borda.**
`infrastructure/persistence/mapper` converte entity JPA ⇄ model de domínio. `WebMapper`
converte Result de aplicação → record de resposta. Não existe conversão de HTTP direto sobre
entidade JPA em lugar nenhum.

**4 — Exceção de domínio, traduzida uma única vez.** Um caso de uso lança
`ConflictException`; `GlobalExceptionHandler` decide o 409 e o corpo. Nenhuma rota escreve
`ResponseEntity.status(409)` por conta própria.

---

## 4. Injeção de dependência na prática

O ponto não óbvio: **as classes de aplicação não são beans anotados**. Não há `@Service`
nem `@Component` em `application/`. Os 27 casos de uso, os 4 serviços de aplicação e as 2
políticas de domínio — 33 objetos no total — são criados por
um único arquivo, [`UseCaseConfig`](../backend/src/main/java/com/redekrill/scheduling/infrastructure/config/UseCaseConfig.java),
com 33 métodos `@Bean`, cada um com um único `new` no corpo — sem `if`, sem laço, sem
lógica de decisão.

```java
@Bean
public LoginUseCase loginUseCase(UserRepository userRepository,
                                 PasswordHasher passwordHasher,
                                 AuditService auditService,
                                 LoginAttemptService loginAttemptService,
                                 AuthSessionService authSessionService) {
    return new LoginUseCase(userRepository, passwordHasher, auditService,
            loginAttemptService, authSessionService);
}
```

Quem preenche os parâmetros é o Spring: os repositórios vêm dos adaptadores JPA
(`@Repository`/interfaces Spring Data), os ports das implementações em
`infrastructure/security` e `infrastructure/email`, o `Clock` de `ClockConfig`, as
propriedades dos records `@ConfigurationProperties`.

O porquê cabe em uma frase: **o caso de uso é instanciável com `new`**. Em
`LoginUseCaseTest`, a montagem é feita à mão com falsos; o `ImmediateTransactionRunner` do
pacote `support` substitui o runner real sem subir contexto nenhum. O custo é escrever
33 métodos de montagem à mão, e isso é pago de bom grado porque o arquivo é mecânico.

Fora de `UseCaseConfig` há apenas cinco beans: três em `SecurityConfig`
(`SecurityFilterChain`, `CorsConfigurationSource`, `AuthCookieSettings`), um em
`ClockConfig` e um em `SecurityBeansConfig` (o `PasswordEncoder`), além do `OpenAPI` de
`OpenApiConfig`. Os trinta e nove estão em `infrastructure` — nenhum em `application`.

---

## 5. Vigília da arquitetura (e por que ela não é "costume")

As quatro regras abaixo estão em
[`ArchitectureTest`](../backend/src/test/java/com/redekrill/scheduling/architecture/ArchitectureTest.java),
rodam com ArchUnit 1.4.1 dentro de `mvnw verify` e quebram o build se alguém as violar:

| Regra | O que proíbe |
|---|---|
| `domainMustNotDependOnOuterLayersOrFrameworks` | `domain` depende de application/infrastructure/presentation, `org.springframework..` ou `jakarta..` |
| `applicationMustNotDependOnInfrastructurePresentationOrSpring` | `application` depende de infrastructure, presentation, Spring ou `jakarta..` |
| `presentationMustNotDependOnInfrastructure` | controller/request/response conhece adaptador JPA, filtro ou `JwtTokenService` |
| `infrastructureMustNotDependOnPresentation` | a infraestrutura conhece `HttpServletRequest` de controller ou record de resposta |

Uma regra que não é testada é o costume que morre na segunda contribuição. Aqui a fronteira
é verificável, então "dependências apontam para dentro" deixa de ser intenção declarada e
vira fato de build.

---

## 6. O caminho de uma requisição

Percorrendo `POST /api/appointments` com um `Authorization: Bearer <jwt>`, na ordem real:

1. **Servlet container** recebe e monta a request.
2. `CorrelationIdFilter` (`@Order(HIGHEST_PRECEDENCE)`) — define o `correlationId` no MDC e
   ecoa `X-Correlation-ID` na resposta.
3. **CORS** (`corsConfigurationSource`) — origin permitido, cabeçalhos autorizados.
4. **CSRF** (`CookieCsrfTokenRepository`, mascarado) — a request é ignorada nesse passo
   porque `BearerAuthorizationRequestMatcher` reconhece o `Authorization: Bearer`; uma request por
   cookie precisaria mandar `X-XSRF-TOKEN`.
5. `JwtAuthenticationFilter` (antes de `UsernamePasswordAuthenticationFilter`) — lê o token,
   chama `JwtTokenService.parse` (assinatura + expiração, com o `Clock` injetado), exige o
   claim `sid`, confere `AuthSessionService.isActive(sid)`, carrega o usuário e exige
   `active`. Sucesso: coloca `UsernamePasswordAuthenticationToken` no contexto com a
   autoridade `ROLE_<role>` e grava os atributos `MUST_CHANGE_PASSWORD` e `SESSION_ID`.
   Falha: segue a cadeia sem autenticação — o 401 não é decidido aqui.
6. `PasswordChangeRequiredFilter` (depois do anterior) — se a conta ainda usa senha inicial
   e a rota não é `me`/`csrf`/`change-password`/`logout`, responde 403 com
   `PASSWORD_CHANGE_REQUIRED` e para.
7. **Autorização por rota** (`SecurityFilterChain`) — `POST /api/appointments/**` exige
   `ROLE_ADMIN`. Sem papel, a exceção vai a `RestAccessDeniedHandler`, que responde 403 com
   `ACCESS_DENIED` e audita a tentativa.
8. `AppointmentController.create` — Bean Validation aceita ou produz
   `MethodArgumentNotValidException` (400 com `fieldErrors`). Aprovado, monta o
   `CreateAppointmentCommand` com `authentication.getName()` como autor.
9. `CreateAppointmentUseCase.execute` — carrega faixa/tipo/paletização e usuário pelos
   repositórios, chama `CapacityPolicy` para medir o impacto, decide entre aceita com avisos
   ou recusa, grava dentro de `TransactionRunner` junto do `AuditService.record` e da
   entrada no outbox.
10. **Persistência** — `JpaAppointmentRepositoryAdapter` usa `AppointmentJpaRepository`,
    converte entity → model no mapper e devolve `Appointment.withId(saved.id())`.
11. **Resposta** — `WebMapper.toResponse` monta `AppointmentCommandResponse`; status 201.
12. **Assíncrono** — segundos depois, `EmailOutboxProcessor` pega a fila em outro thread e
    entrega o SMTP, fora da transação da request (ADR-003).

O que **não** aconteceu: nenhuma regra de negócio em filtro ou controller, nenhuma consulta
JPQL fora do adaptador, nenhum objeto de domínio no JSON de resposta.

---

## 7. Fluxos completos

### 7.1 Login, emissão de token e criação de sessão

```
POST /api/auth/login  { email, password }
  → AuthController.login
    → validação de corpo (LoginRequest: e-mail válido, senha ≤ 72 caracteres)
    → LoginCommand com e-mail, senha, IP (ClientIpResolver) e User-Agent
    → LoginUseCase.execute
      1. LoginAttemptService.assertAllowed(email, ip)   ← a trava vem antes da senha
      2. UserRepository.findByEmail(email normalizado)
      3. PasswordHasher.matches(senha, hash)            ← BCrypt
      4. fracasso → registra tentativa e lança a MESMA frase para
         "conta inexistente" e "senha errada"; sucesso → zera a conta, não o IP
      5. AuthSessionService.start(user, ip, userAgent)
         → TokenIssuer.issue(user, sessionId)          ← JWT com sid
         → RefreshTokenGenerator.generate()            ← 32 bytes, Base64 URL sem padding
         → hash SHA-256 do refresh guardado no banco (nunca o valor que sai)
    → AuthController escreve duas cookies HttpOnly:
         access  → path "/"        (Max-Age = expiração do JWT)
         refresh → path "/api/auth"(Max-Age = app.auth.refresh-expiration)
    → LoginResponse: token só vem preenchido se exposeTokenInResponse=true;
      no fluxo por cookie os campos ficam nulos e a sessão está nas Set-Cookie
```

O ponto que costuma ser mal interpretado: **a senha é comparada por último de propósito**.
BCrypt é caro por design, e um usuário já bloqueado não deve fazer o servidor pagar esse
custo a cada tentativa.

### 7.2 Validação do token e autorização (a request seguinte)

Toda chamada autenticada refaz o mesmo caminho: o `JwtAuthenticationFilter` decide *quem é*,
a `SecurityFilterChain` decide *se pode*. Três pontos valem a atenção:

- **Duas fontes de token, com prioridade.** O header `Authorization: Bearer` vence o cookie
  de acesso quando os dois existem — header é escolha explícita de quem chama.
- **O token não é suficiente.** Mesmo assinado e dentro da expiração, a request só é
  autenticada se a sessão do claim `sid` estiver ativa no banco **e** o usuário estiver
  ativo. Isso é o que faz logout e desativação de conta valerem imediatamente, e é o que
  torna a revogação uma linha no banco em vez de esperar o JWT vencer.
- **Papel vem da conta, não do claim.** O claim `role` é lido, mas a autoridade aplicada é
  `ROLE_<role>` do `User` carregado do banco.
- **Rotas administrativas:** `/api/users/**`, `/api/audit-logs/**`, `/actuator/**` e o POST,
  PUT e DELETE de `truck-types`, `time-slots`, `palletization-types` e `appointments` exigem
  ADMIN. Em produção, `swagger-ui` e `/v3/api-docs` também.

### 7.3 Criação de agendamento (capacidade, auditoria e e-mail)

```
POST /api/appointments → ADMIN → CreateAppointmentUseCase
  1. carrega TimeSlot, TruckType, PalletizationType e o autor
  2. soma pallets do dia e da faixa (variantes "Excluding" na edição, para o registro em
     edição não contar duas vezes), sempre filtrando status ACTIVE
  3. CapacityPolicy.assess → aceita, aceita com avisos ou recusa
     (480/dia e 120/faixa são limites não bloqueantes: estourar gera aviso, ADR-004)
  4. dentro de TransactionRunner: AppointmentRepository.save + AuditService.record
     + EmailOutboxService.enqueue
  5. devolve AppointmentCommandResult com o agendamento, os avisos, a capacidade
     resultante e emailQueued=true (emailSent é sempre false no instante da resposta)
  ─── segundos depois, em outro thread ───
  6. EmailOutboxProcessor: lê a fila com lock pessimista, verifica se o agendamento
     ainda existe e não foi cancelado, envia via SmtpEmailNotifier e grava o resultado
     em transação separada; falha temporária dobra o backoff até o teto de uma hora
```

Se o SMTP estiver fora, o agendamento **não** falha: é essa a razão de o envio ser uma
fila transacional em vez de uma chamada dentro da request.

### 7.4 Cancelamento

```
DELETE /api/appointments/{id} → ADMIN → CancelAppointmentUseCase
  1. Appointment.cancel(...) marca CANCELED com quem e quando — nada é apagado
  2. grava + audita dentro da mesma transação
  3. EmailOutboxService.cancelPending suprime confirmações ainda não enviadas
  4. resposta 204, sem corpo
```

Como a soma de capacidade filtra `status = ACTIVE`, o pallets cancelado volta a ficar livre
na faixa automaticamente, sem `delete`.

### 7.5 Renovação com token de refresh rotativo e detecção de reuso

```
POST /api/auth/refresh  (usa a cookie de refresh, path /api/auth)
  1. AuthSessionService.rotate: hash SHA-256 do token recebido → busca no banco
  2. sessão não encontrada ou vencida          → rejeita com 401
  3. sessão já revogada (reuso!)               → revoga TODAS as sessões do usuário
     dentro da transação, audita REFRESH_TOKEN_REUSE e só depois lança a rejeição
  4. corrida de rotação (revokeIfActive afetou 0 linhas) → mesmo tratamento de reuso
  5. sucesso                                   → nova sessão, novos tokens, cookies trocadas
```

O detalhe que importa para a corretude: a revogação em cascata é escrita **dentro** da
transação e a exceção é lançada **depois** dela. Se a exceção estivesse dentro, o rollback
desfaria justamente a punição que a detecção de reuso aplica.

### 7.6 Troca de senha

```
POST /api/auth/change-password { currentPassword, newPassword }
  1. ChangePasswordUseCase confere a senha atual, aplica PasswordPolicy na nova
  2. troca o hash e liga mustChangePassword=false
  3. AuthSessionService revoga as sessões ativas da conta
  4. AuthController limpa as duas cookies na mesma resposta 204
```

Sem o passo 3, quem troca a senha manteria um par de credenciais válido no navegador; sem o
passo 4, a cookie antiga continuaria sendo enviada até expirar.

---

## 8. Onde fica cada coisa (consulta rápida)

| Pergunta | Resposta |
|---|---|
| Regra de negócio | `domain/model` (regras do próprio objeto) e `domain/policy` (regras que cruzam objetos) |
| Ordem das operações, transação, auditoria | `application/usecase` e `application/service` |
| Interface de acesso a dados | `domain/repository` (é interface, não implementação) |
| SQL / JPQL / consulta em lote | `infrastructure/persistence/repository` + `adapter` |
| Formato do banco (colunas, índices, constraints) | `infrastructure/persistence/entity` e as migrations em `backend/src/main/resources/db/migration` |
| Validação de entrada de API | Bean Validation nos records de `presentation/request`; validação de domínio em `domain` |
| Validação de configuração | `infrastructure/config/properties` (`@Validated`) + `ProductionConfigurationValidator` |
| Autenticação, autorização, 401/403 | `infrastructure/security` |
| Formato de erro | `application/dto/error/ApiError` + `presentation/exception/GlobalExceptionHandler` |
| Envio de e-mail | `infrastructure/email` (fila em `EmailOutboxProcessor`, corpo em template Thymeleaf) |
| Montagem das dependências | `infrastructure/config/UseCaseConfig` |

---

## 9. Organização dos testes

37 arquivos em `backend/src/test/java/com/redekrill/scheduling` — 35 classes de teste e 2
auxiliares em `support/` —, divididos por **propósito**, não por camada apenas:

- **Unidade, sem Spring** (24 casos de uso e serviços de aplicação + `PaginationTest`): a mesma forma do
  `application/` principal — `application/usecase/appointment/CreateAppointmentUseCaseTest`,
  `application/service/AuthSessionServiceTest`. Montam o caso de uso com `new`, falsos nos
  ports e `ImmediateTransactionRunner`. É aqui que o tempo é controlado: entra um `Clock`
  congelado, então "sessão expirada" e "faixa de amanhã" não dependem de quando o build roda.
- **Domínio puro** (`domain/model/AppointmentTest`, `domain/policy/CapacityPolicyTest`,
  `domain/policy/PasswordPolicyTest`): as regras sem nenhum colateral.
- **Arquitetura** (`architecture/ArchitectureTest`): as quatro regras da seção 5.
- **Integração HTTP** (`integration/SchedulingApiIntegrationTest`): um único arquivo, 10 testes,
  com o contexto Spring inteiro e PostgreSQL embarcado (zonky). É a suíte que prova os
  atravessamentos: login com cookie e recusa sem CSRF, rotação de refresh com revogação da
  sessão, fluxo completo de agendamento com restrição de papel, `orderId` duplicado (409),
  cancelamento lógico com histórico, e-mail enfileirado sem travar a criação, registro de
  logout e de acesso negado na auditoria, validação do payload e o `active` obrigatório na
  atualização de usuário. Filtros, CSRF e o `SecurityConfig` rodam de verdade aqui.
- **Infraestrutura isolada** (`infrastructure/email/EmailOutboxProcessorTest`,
  `infrastructure/security/JwtTokenServiceTest`, `infrastructure/security/AuthSessionCleanupJobTest`).
- **Apresentação isolada** (`presentation/exception/GlobalExceptionHandlerTest`,
  `presentation/support/ClientIpResolverTest`) — a primeira não precisa de servidor para
  provar que um `ConflictException` vira 409.
- **`support/`** — `TestFixtures` e `ImmediateTransactionRunner`, compartilhados entre as
  suítes.

Cobertura: o gate do JaCoCo (`pom.xml`, execução `check` em `verify`) exige **≥ 0,80 de
linhas** sobre quatro agregados — `domain.policy.*`, `application.usecase.appointment.*`,
`application.usecase.auth.*` e `application.service.*`. É uma exigência deliberada sobre o
núcleo que decide regra, não uma média geral que sobe com getter coberto.

Frontend (`frontend/`): `src/services/api.test.js`, `src/stores/auth.test.js` e
`src/utils/capacity.test.js` com Vitest; `e2e/auth.spec.js` com Playwright.

---

## 10. Árvore de diretórios (nomes reais)

```
backend/
├── pom.xml                          # Spring Boot 4.1.2, Java 21, ArchUnit, JaCoCo, OWASP DC
├── mvnw / mvnw.cmd                  # build sem Maven instalado
└── src/
    ├── main/
    │   ├── java/com/redekrill/scheduling/
    │   │   ├── SchedulingApplication.java   # apenas o boot
    │   │   │
    │   │   ├── domain/                      # 31 · zero dependência externa
    │   │   │   ├── model/        (15)       # agregados, enums de estado, PageResult
    │   │   │   ├── policy/       (3)        # CapacityPolicy, CapacityAssessment, PasswordPolicy
    │   │   │   ├── repository/   (8)        # interfaces de acesso a dados
    │   │   │   └── exception/    (5)        # DomainException + 4 filhos com status
    │   │   │
    │   │   ├── application/                 # 64 · casos de uso; sem Spring
    │   │   │   ├── usecase/      (1 + 27)   # Pagination + appointment, reference, auth,
    │   │   │   │                            #   trucktype, user, audit
    │   │   │   ├── service/      (4)        # AuthSession, LoginAttempt, EmailOutbox, Audit
    │   │   │   ├── port/         (6)        # TokenIssuer, IssuedToken, RefreshTokenGenerator,
    │   │   │   │                            #   PasswordHasher, EmailNotifier, TransactionRunner
    │   │   │   └── dto/          (26)       # commands, results, ApiError
    │   │   │
    │   │   ├── infrastructure/              # 60 · o contato com o mundo externo
    │   │   │   ├── persistence/
    │   │   │   │   ├── entity/     (8)      # sombras JPA
    │   │   │   │   ├── repository/ (8)      # interfaces Spring Data + @Query
    │   │   │   │   ├── adapter/    (8)      # implementam os repositórios de domain
    │   │   │   │   └── mapper/     (8)      # entity ⇄ model
    │   │   │   ├── security/     (11)       # filtros, JWT, refresh, BCrypt, 401/403, job
    │   │   │   ├── config/       (4 + 7)    # UseCaseConfig, OpenApi, Clock, validator de prod
    │   │   │   │   └── properties/          # app.auth|jwt|security|cors|email|email.outbox|seed
    │   │   │   ├── email/        (3)        # SmtpEmailNotifier, EmailOutboxProcessor, exceção
    │   │   │   ├── transaction/  (1)        # SpringTransactionRunner
    │   │   │   ├── observability/(1)        # CorrelationIdFilter
    │   │   │   └── seed/         (1)        # DataInitializer
    │   │   │
    │   │   └── presentation/                # 35 · contrato HTTP
    │   │       ├── controller/   (7)
    │   │       ├── request/      (12)       # Bean Validation na entrada
    │   │       ├── response/     (13)       # PageResponse<T> como envelope comum
    │   │       ├── mapper/       (1)        # WebMapper
    │   │       ├── exception/    (1)        # GlobalExceptionHandler
    │   │       └── support/      (1)        # ClientIpResolver
    │   │
    │   └── resources/
    │       ├── application.yml (+ -dev / -prod)
    │       ├── db/migration/                # Flyway: dono do schema (ADR-005)
    │       └── templates/email/             # corpo do e-mail de confirmação
    │
    └── test/java/com/redekrill/scheduling/
        ├── architecture/                    # as 4 regras de dependência
        ├── domain/                          # modelos e políticas
        ├── application/                     # casos de uso e serviços, sem Spring
        ├── infrastructure/                  # processador de outbox, JWT, job de sessão
        ├── presentation/                    # handler de erro e resolvedor de IP
        ├── integration/                     # contexto completo + Postgres embarcado
        └── support/                         # TestFixtures, ImmediateTransactionRunner

docs/
├── ARCHITECTURE.md                          # este arquivo
├── arquitetura/                             # ADR-001 a ADR-006 + README
├── operacao/RUNBOOK.md                      # como operar
├── VALIDACAO_FINAL.md                       # o que foi validado e como
└── entrega/                                 # ENTREGA.html, PDF, roteiro de vídeo

frontend/                                    # Vue 3 + Vite, separado do build do backend
```

---

## 11. O que foi deixado de fora de propósito

Registrar o que **não** existe evita que alguém o adicione "para completar":

- **Nenhuma fábrica, nenhuma abstração extra.** Não há `Abstract*`, `*Factory` nem
  injeção por perfil para cada port. Um porto tem uma implementação e é montado em `new` —
  a segunda implementação apareceria com um motivo concreto (outro banco, outro provedor de
  e-mail), não antes.
- **Nenhum microserviço.** Um deploy, um processo, um banco. A separação de camadas já dá o
  isolamento que se precisa aqui.
- **Nenhum CQRS, event sourcing ou mediador.** Casos de uso são chamados direto pelos
  controllers; interpor um dispatcher entre eles seria um degrau sem função.
- **Nenhuma anotação de segurança nos controllers.** Toda autorização está na
  `SecurityFilterChain`. Não há `@PreAuthorize` espalhado — a superfície de decisão é uma
  lista só, revisável em um diff.
- **Nenhum DTO de domínio no JSON.** Se um agregado ganhar campo novo, a API não muda até
  que alguém decida mudar `WebMapper`.

---

## 12. Comandos que provam o que este arquivo afirma

```bash
# backend — build completo: compila, testa, roda as 4 regras de arquitetura e o gate de cobertura
cd backend && ./mvnw -B clean verify

# só a arquitetura, em segundos
cd backend && ./mvnw -B test -Dtest=ArchitectureTest

# um caso de uso isolado, sem banco nem Spring
cd backend && ./mvnw -B test -Dtest=CreateAppointmentUseCaseTest

# relatório de cobertura gerado em verify
# backend/target/site/jacoco/index.html

# frontend
cd frontend && npm run test && npm run typecheck && npm run lint
```

O perfil `security` (`./mvnw -B verify -Psecurity`) adiciona a varredura do
OWASP Dependency-Check ao build e precisa de rede na primeira execução para baixar os feeds
de vulnerabilidade — ele **não** funciona com `-o`.
