# Roteiro de vídeo — demonstração Rede Krill Scheduling

Documento de apoio para gravação da apresentação técnica do sistema.
Duração-alvo: **11 a 12 minutos** (versão curta de 6 min ao final). Formato: tela cheia, terminal + navegador lado a lado, narração gravada (sem música sob a fala).

| Item | Definição |
|---|---|
| Público | quem aprova a entrega: gestão técnica + um desenvolvedor que vai manter o sistema |
| Objetivo | provar três coisas: o fluxo de negócio funciona, a segurança é real e mensurável, e as lacunas estão declaradas (não escondidas) |
| Entregável do vídeo | arquivo `rede-krill-demo-<aaaa-mm-dd>.mp4`, 1080p, 30 fps, com o commit demonstrado identificado na abertura |
| Regra inegociável | nada de resultado inventado: se uma etapa não rodar na máquina de gravação, corte a cena e registre no relatório — não simule |

---

## 1. Pré-requisitos de gravação (checklist antes de ligar o gravador)

**Ambiente — precisa estar funcionando de verdade.** Estas instruções ainda não foram validadas nesta máquina (Docker indisponível; ver `SECURITY_REPORT.md`, `SEC-06`). Rode o checklist abaixo **antes** de definir as cenas; cada cena que depender de um item que falhou deve ser substituída pela alternativa "só código" indicada na tabela.

```bash
# 1) banco
docker compose up -d postgres            # ou um PostgreSQL 16 local com banco rede_krill

# 2) e-mail (perfil dev do compose)
docker compose --profile dev up -d mailhog

# 3) backend, perfil dev
cd backend && mvn spring-boot:run

# 4) frontend
cd frontend && npm install && npm run dev
```

- [ ] Backend sobe sem falha no `ProductionConfigurationValidator` (perfis não-`prod` são tolerantes; em `prod` exige segredos fortes).
- [ ] `http://localhost:8080/actuator/health` → `{"status":"UP"}`.
- [ ] Frontend em `http://localhost:8081` (Vite) ou `${FRONTEND_PORT:-8081}` no compose — **se usar o compose, o perfil default é `prod` com cookies `Secure`; em `http://localhost` o login não completa** (README, seção de execução). Para gravar, prefira `mvn spring-boot:run` + `npm run dev`.
- [ ] Conta ADMIN criada pelo seed: `SEED_ADMIN_ENABLED=true` e `SEED_ADMIN_PASSWORD` com **12+ caracteres, fora da lista de senhas proibidas** (o seed rejeita `Admin@12345`, `redekrill`, `password`, `changeme`, `secret`).
- [ ] `SEED_ADMIN_REQUIRE_PASSWORD_CHANGE=true` → o primeiro login cai na tela de troca de senha (cena 3).
- [ ] Uma segunda conta de perfil OPERADOR criada (para mostrar auditoria com dois atores).
- [ ] Dados de referência mínimos: ao menos 1 tipo de caminhão, 1 tipo de paletização e 1 faixa de horário livre.
- [ ] Terminal com fonte grande, histórico limpo, `.env` **fechado** e sem segredos visíveis na barra lateral do editor.
- [ ] Notificações desligadas, hora do sistema correta, gravador em 1080p.

**Higiene de tela:** não mostrar o valor de `JWT_SECRET`, a senha do banco ou tokens completos. Ao abrir o DevTools, o cookie `redekrill_refresh` deve aparecer com o valor **mascarado** na gravação (no Chrome, segurar `Alt` ao passar o mouse mascara o conteúdo).

---

## 2. Cenas

Tempo acumulado aproximado. "Fala" é o texto a narrar; "Tela" é o que deve estar visível enquanto se fala.

### Cena 1 — Abertura (0:00 – 0:40)
- **Tela:** dashboard do frontend logado, título `Rede Krill Scheduling` visível. Slide de cartão com ramo e commit.
- **Fala:** "Rede Krill Scheduling é o sistema de agendamento de coletas da frota. Nesta demonstração eu mostro o fluxo de negócio completo, depois os controles de sessão, CSRF, limite de tentativa e notificação assíncrona, e no fim o que está medido — inclusive o que ainda não foi validado."
- **Inserir na tela:** ramo `main`, commit demonstrado, data. Se o estado de segurança ainda estiver sem commit, dizer isso em texto no rodapé do slide.

### Cena 2 — Arquitetura (0:40 – 2:00)
- **Tela:** árvore `backend/src/main/java/com/redekrill/scheduling` com as quatro camadas; depois `ArchitectureTest`; depois `docs/arquitetura/README.md`.
- **Fala:** "Clean Architecture em quatro camadas. O domínio não conhece Spring nem JPA, e isso não é convenção verbal: é uma regra executada por ArquUnit a cada build. As decisões estruturais estão em seis ADRs, um por escolha que tem custo — sessão em cookie, CSRF, outbox de e-mail, limites fixos de capacidade, Flyway como dono do schema e concorrência otimista."
- **Corte:** não ler o ADR; mostrar o sumário e seguir.

### Cena 3 — Login seguro e troca obrigatória de senha (2:00 – 3:40)
- **Tela:** DevTools → Application → Cookies. Login com a senha do seed → tela forçada de troca de senha → login novamente.
- **Fala:** "Depois do login, o access token de quinze minutos e o refresh de sete dias viram cookies `HttpOnly` — o JavaScript da página não consegue lê-los, e por isso não existe token em `localStorage`. No primeiro acesso o sistema obriga a trocar a senha; enquanto ela não for trocada, toda rota devolve `403 PASSWORD_CHANGE_REQUIRED`. Trocar a senha revoga todas as sessões ativas."
- **Mostrar:** coluna `HttpOnly` marcada, o atributo `SameSite`, e a chave `sid` dentro do payload do access token.

### Cena 4 — CSRF e rotação de refresh (3:40 – 4:50)
- **Tela:** DevTools → Network, filtro `auth`. Uma criação de agendamento (mostra `X-XSRF-TOKEN` no header) e depois uma requisição com o access expirado.
- **Fala:** "Toda mutação leva o token CSRF que veio em cookie lido pelo frontend; a única exceção é o header `Bearer`, usado por clientes de linha de comando no perfil de desenvolvimento. Quando o access vence, o axios recebe `401`, coloca as requisições em uma fila única, chama `refresh` uma vez e refaz as chamadas originais. O refresh é de uso único: a rotação revoga o anterior — reutilizar um token velho não gera uma nova sessão."

### Cena 5 — Limite de tentativas (4:50 – 5:35)
- **Tela:** `curl` repetido contra `/api/auth/login` com senha errada, mostrando os `401` e depois o `429` com `Retry-After`.
- **Fala:** "Cinco tentativas na mesma conta ou vinte no mesmo IP em quinze minutos geram bloqueio temporário com resposta `429` e `Retry-After`. O contador é progressivo: o segundo bloqueio é quatro vezes maior. Uma ressalva honesta — esse estado vive em memória, então ele é por instância; escala horizontal exige um store compartilhado, e isso está registrado como achado `SEC-01`."
- **Não mostrar:** a senha real em texto puro; use uma senha claramente falsa.

### Cena 6 — Agendamento, capacidade e concorrência (5:35 – 7:45)
- **Tela:** formulário de criação de agendamento → painel de ocupação em tempo real → mesma faixa tentada duas vezes → conflito.
- **Fala:** "A criação valida os limites de capacidade do domínio — quatrocentas e oitenta coletas por dia e cento e vinte por faixa de duas horas — e o `orderId` do fornecedor, que é único: repetir o identificador devolve conflito em vez de duplicar a coleta. Sob concorrência real, a garantia vem da restrição única no banco e do lock otimista com a coluna de versão; os dois casos viram `409`. Cancelamento é lógico, preservando o histórico."
- **Mostrar:** o `409` com código de erro legível e a faixa com ocupação atualizada.

### Cena 7 — Cadastros operacionais (7:45 – 8:40)
- **Tela:** telas de Tipos de Caminhão, Tipos de Paletização e Horários; criar um item, editar, e listar com paginação.
- **Fala:** "Três cadastros sustentam o agendamento: caminhões, tipos de paletização com a regra de metros lineares por pallet, e as faixas de horário. Todos com paginação e validação no backend, e a auditoria registra quem mudou o quê."

### Cena 8 — Outbox de e-mail (8:40 – 9:30)
- **Tela:** `SELECT id, status, attempts, recipient FROM email_outbox ORDER BY id DESC LIMIT 5;` → MailHog (`http://localhost:8025`) com a mensagem → a resposta da API com `emailQueued: true` e `emailSent: false`.
- **Fala:** "O e-mail entra na mesma transação do agendamento e é entregue por um processo agendado, com retentativa exponencial e no máximo cinco tentativas. Por isso o contrato da API separa `emailQueued`, que é verdadeiro no momento da resposta, de `emailSent`, que permanece falso no POST — a entrega é assíncrona por projeto, documentado no ADR-003."

### Cena 9 — Auditoria, relatórios e observabilidade (9:30 – 10:25)
- **Tela:** tela de auditoria com dois atores; relatório paginado; terminal com `/actuator/prometheus` filtrado em `email_outbox_processed_total`; uma linha de log contendo o correlation id.
- **Fala:** "Cada operação sensível deixa trilha com ator, entidade, ação e correlation id, o mesmo identificador que atravessa a resposta e o log estruturado — o que liga a ponta de uma chamada de cliente à linha de servidor. Métricas por perfil: saúde é pública sem detalhes, Prometheus e Swagger só para administrador. Relatórios são paginados."

### Cena 10 — Testes e cobertura (10:25 – 11:25)
- **Tela:** `mvn verify` terminando; depois `backend/target/site/jacoco/index.html` nos quatro pacotes do gate.
- **Fala:** "Cento e vinte e cinco testes em vinte e cinco classes, com zero falhas — incluindo regras de negócio puras, use cases com mock e a API contra PostgreSQL embarcado. O gate de cobertura exige oitenta por cento em linhas nos quatro pacotes críticos e está em noventa e um vírgula vinte e cinco por cento. Onde a cobertura é fraca também está dito: a classe de bloqueio de login e o caso de uso de troca de senha ficam abaixo disso em instruções, e o processador de outbox não tem teste próprio — são os achados `SEC-02` e `SEC-14`."
- **Comando na tela:** `mvn -f backend/pom.xml verify`
- ⚠️ Só grave esta cena ao vivo se o comando rodar na máquina de gravação. Os números acima foram lidos dos artefatos já presentes em `backend/target` e podem ser mostrados como tela estática caso o build não rode ao vivo.

### Cena 11 — Fechamento com lacunas (11:25 – 12:10)
- **Tela:** tabela de achados de `SECURITY_REPORT.md` (SEC-01, SEC-03, SEC-06) e a lista de entregáveis em `docs/`.
- **Fala:** "Fechando com o que ainda não posso afirmar. Não executei análise de composição das dependências do backend, então não há declaração de ausência de CVE. Não validei o caminho Docker e CI nesta máquina, porque o Docker não está disponível. E o bloqueio de tentativas é por instância. Os três estão listados como prioridades P0 e P1, com o plano de re-teste no relatório de segurança e as rotinas operacionais no runbook."
- **Último frame:** `README.md` · `SECURITY_REPORT.md` · `docs/arquitetura/` · `docs/operacao/RUNBOOK.md` · `docs/entrega/`.

---

## 3. Versão curta de 6 minutos

Cortar: cena 2 (arquitetura) reduzida a 20 s de slides, cena 7 a 30 s, cena 9 sem Prometheus.
Manter obrigatoriamente: **cena 3** (cookies/troca de senha), **cena 4** (rotação), **cena 6** (capacidade/conflito), **cena 8** (outbox), **cena 11** (lacunas).
Um vídeo de demonstração que não mostra as lacunas perde valor diante do relatório escrito, que as lista.

---

## 4. Falas prontas para ler sem improviso

**Abertura (Cena 1).** "Olá. Estou demonstrando o Rede Krill Scheduling, sistema de agendamento de coletas. O que está rodando aqui é o estado atual do código: backend em Spring Boot quatro vírgula um vírgula dois, frontend em Vue três e PostgreSQL dezesseis. Vou seguir a ordem: arquitetura, segurança de sessão, fluxo de agendamento, notificação, observabilidade, testes e — por último — as lacunas que ainda existem."

**Encerramento (Cena 11).** "Isso encerra a parte funcional. A entrega escrita traz o relatório de segurança com catorze achados numerados, seis ADRs com evidência em arquivo e linha, o runbook com o SQL de operação e este roteiro. O resumo: o sistema está pronto para operação interna controlada, em instância única e com HTTPS obrigatório, e as três prioridades antes de produção são análise de dependências do backend, validação do caminho Docker e CI, e reforço de cobertura nas classes de controle de acesso."

---

## 5. Pós-gravação

- [ ] Conferir que nenhum segredo apareceu em frame (cookies, `.env`, cabeçalhos `Authorization`).
- [ ] Legendas em português; termos técnicos (`refresh token`, `outbox`) mantidos no original.
- [ ] Nome do arquivo: `rede-krill-demo-<data>.mp4`; registrar junto o commit demonstrado (na descrição do vídeo ou em `docs/entrega/`).
- [ ] Se alguma cena foi cortada por falha de ambiente, registrar o corte em `docs/VALIDACAO_FINAL.md` — o relatório é a fonte de verdade, o vídeo é apoio.
- [ ] Não prometer no vídeo nada que o relatório marque como ⚠️.

---

## 6. Erros de gravação mais prováveis (e o que fazer)

| Sintoma | Causa provável | Ação |
|---|---|---|
| Login volta para a tela de credenciais sem erro visível | stack do compose com perfil `prod` e cookies `Secure` em `http://localhost` | gravar com backend em perfil `dev`, ou servir o frontend em HTTPS |
| `403` em toda mutação | `ensureCsrf()` não rodou antes (aba recém-aberta sem GET prévio) | recarregar a aplicação; demonstrar o cookie `XSRF-TOKEN` antes |
| `403 PASSWORD_CHANGE_REQUIRED` inesperado | conta ainda com a senha do seed | trocar a senha uma vez antes de gravar o fluxo normal |
| Seed não cria o administrador | `SEED_ADMIN_PASSWORD` ausente, curto ou proibido | usar senha de 12+ caracteres fora da lista; ver runbook, seção de provisionamento |
| Nenhuma mensagem no MailHog | serviço fora do perfil ativo (`mailhog` só sob `--profile dev`) ou outbox desabilitado | subir MailHog e confirmar `EMAIL_OUTBOX_ENABLED=true` |
| E-mail nunca sai de `PENDING` | processador desligado no perfil de teste (`outbox.enabled: false`) | gravar em perfil `dev`, não `test` |
