# Validação final — Rede Krill Scheduling

Relatório de fechamento da entrega. Cada linha declara **como** o item foi conhecido, porque
"está pronto" e "foi verificado" são afirmações diferentes.

- Data: **2026-11-02**
- Ramo: `main` · último commit publicado: `06cee93` (2026-11-02)
- Estado da árvore de trabalho neste momento: **156 entradas não commitadas** (83 modificadas, 72 não rastreadas, 1 renomeada)
- `.github/` está **fora do versionamento** → nenhum workflow validou este estado
- Decisão registrada: a "Fase 0" (re-executar as suítes de teste antes de documentar) foi **pulada por orientação do responsável**. Os números de teste e cobertura vêm dos artefatos já gerados, não de execução desta sessão.

---

## 1. Escala de classificação

| Código | Significado | Como provar |
|---|---|---|
| **V** | **Validado nesta sessão** — comando executado aqui, saída conferida | comando e resultado registrados na seção 3 |
| **A** | **Validado por artefato** — executado antes (02/11 22:19–22:22), lido do arquivo de saída | caminho do artefato + timestamp |
| **C** | **Conferido por leitura de código** — implementação existe e está correta no fonte, mas não exercitada em runtime | `arquivo:linha` |
| **N** | **Não validado** — depende de algo indisponível nesta máquina (Docker, chave NVD, ambiente de CI) | bloqueio declarado na seção 5 |
| **L** | **Lacuna conhecida** — validado que *falta*; tem ID em `SECURITY_REPORT.md` | ID do achado |

Um item pode ser **C** e **L** ao mesmo tempo: o controle existe no código e, ainda assim, tem um defeito conhecido.

---

## 2. Resultado consolidado

| # | Item da entrega original | Classe | Valor / evidência |
|---|---|---|---|
| 1 | README reescrito | **V** | `README.md`, 446 linhas, com tabela de variáveis, API, migrações e limitações |
| 2 | `SECURITY_REPORT.md` | **V** | 282 linhas, 14 achados `SEC-01`…`SEC-14`, plano de re-teste de 12 itens |
| 3 | ADRs em `docs/arquitetura/` | **V** | índice + 6 ADRs (001 sessão, 002 CSRF, 003 outbox, 004 capacidade, 005 Flyway, 006 concorrência) |
| 4 | Runbook | **V** | `docs/operacao/RUNBOOK.md`, 272 linhas, com SQL de operação real |
| 5 | `docs/entrega/` com PDF + roteiro | **V** | `ENTREGA.html` → `Rede-Krill-Scheduling-Entrega.pdf` (6 páginas, Chrome headless) + `ROTEIRO_VIDEO.md` (11 cenas) |
| 6 | OWASP Dependency-Check no backend | **N** | sem `target/dependency-check`; exige `NVD_API_KEY` → **L** (`SEC-03`) |
| 7 | `npm audit` | **V** | produção: **0 vulnerabilidades**; árvore completa: 12 moderadas (só dev) |
| 8 | Validação final classificada | **V** | este documento |

| Área do produto | Classe |
|---|---|
| Build e testes backend | **A** |
| Cobertura e gate de qualidade | **A** |
| Portas de qualidade do frontend (lint, tipos, formatação, build) | **V** |
| E2E de autenticação | **A** |
| Migrações V1–V6 | **A** (em Postgres embarcado nos testes) + **C** (SQL lido) |
| Controles de segurança implementados | **C** |
| Comportamento ponta a ponta em runtime (CSRF, `429`, rotação, outbox) | **N** → **L** (`SEC-06`) |
| Docker Compose | **N** → **L** (`SEC-06`) |
| Pipeline de CI | **N** |
| Escala horizontal | **N** → **L** (`SEC-01`) |

---

## 3. O que foi executado nesta sessão

Saídas reais, reproduzíveis com os comandos da seção 7.

| Comando | Resultado | Classe |
|---|---|---|
| `npm audit --omit=dev` (em `frontend/`) | `found 0 vulnerabilities` | **V** |
| `npm audit` (em `frontend/`) | `12 moderate severity vulnerabilities` — árvore de desenvolvimento (`eslint-config-prettier`, `vue-eslint-parser`, `editorconfig`/`minimatch`); sem efeito no bundle entregue | **V** |
| `npm run lint` → `eslint .` | código de saída **0**, nenhuma saída de erro | **V** |
| `npm run typecheck` → `vue-tsc --noEmit` | código de saída **0** | **V** |
| `npm run format:check` → `prettier --check .` | `All matched files use Prettier code style!` (saída 0) | **V** |
| `npm run build` → `vite build` | `✓ built in 4.08s` (saída 0); maior chunk `dist/assets/column-*.js` 374,48 kB · gzip 79,56 kB | **V** |
| leitura de `backend/target/surefire-reports/*.xml` | **125** testes, **25** classes, 0 falhas, 0 erros, 0 ignorados | **A** |
| leitura de `backend/target/site/jacoco/jacoco.csv` | ver seção 4 | **A** |
| leitura de `frontend/test-results/.last-run.json` | `{"status":"passed","failedTests":[]}` | **A** |
| `git status --porcelain` | 156 entradas; `ApiError.java` renomeado para `application/dto/error/` | **V** |

**Não executado nesta sessão** (por decisão de pular a Fase 0): `mvn verify`, `mvn test`, `vitest run`, `playwright test`, `docker compose up`. Nada destas linhas é apresentado como resultado obtido agora.

### 3.1 Executado depois, ao habilitar o acesso ADMIN local

| Ação | Resultado | Classe |
|---|---|---|
| `.\start-dev.ps1` (carrega `.env`, mapeia `POSTGRES_*` → `DB_*`, roda `mvnw spring-boot:run`) | backend sobe: `Tomcat started on port 8080`, log do seed `Usuário administrador inicial já existe: admin@redekrill.com` | **V** |
| `npx vitest run` (em `frontend/`) | **20** testes em **3** arquivos, 0 falhas | **V** |
| `npm run lint` / `format:check` / `typecheck` / `build` após a correção | todos com saída 0; `✓ built in 1.52s` | **V** |
| Login pela UI em `http://localhost:5173/login` | `POST /api/auth/login` **200** → `Painel` renderizado como `Administrador` / perfil ADMIN; `GET /api/appointments/capacity?date=…` **200** | **V** |
| `POST /api/auth/refresh` via `api` do app, já autenticado | **200** — confirma que o token CSRF do pós-login é aceito | **V** |

Defeito encontrado e corrigido durante esta etapa (estava mascarado porque o Playwright mocka `/api/auth/*`):

- `frontend/src/services/api.js` — `ensureCsrf()` preferia o cookie `XSRF-TOKEN`, que traz o token **bruto**; o Spring Security (proteção BREACH) só aceita o valor **mascarado** devolvido no corpo de `GET /api/auth/csrf`. Além disso o axios, com `withXSRFToken` no padrão, sobrescrevia o cabeçalho pelo valor do cookie **depois** dos interceptors. Resultado: todo `POST /api/auth/login` pela UI devolvia `403 ACCESS_DENIED`. Correção: `withXSRFToken: false` nos dois clientes e token novo a cada requisição de escrita (login/logout geram token bruto novo, cache fica inválido).
- Configuração local — `CORS_ALLOWED_ORIGINS` só listava 5173/8081; caindo o Vite para 5174/5175 o backend respondia `403 Invalid CORS request`. Acrescentadas essas portas ao `.env`/`.env.example`.
- Armadilha registrada — `LoginAttemptService` bloqueou o IP durante o diagnóstico (`429` com `Retry-After`); o estado é em memória, e reiniciar o backend o zera (`RUNBOOK.md` §4.2).

Nada do §3.1 altera os números de cobertura ou de testes do backend: a suíte Java não foi reexecutada nesta sessão.

---

## 4. Cobertura medida (artefato JaCoCo)

Contra o gate real do build — quatro pacotes, contador **LINHA**, mínimo **0,80** (`backend/pom.xml:175-186`):

| Escopo | Linhas | Instruções | Contra o gate |
|---|---|---|---|
| 4 pacotes críticos (gate) | **438/480 = 91,25%** | 1966/2240 = **87,77%** | **passa** |
| `domain.policy` | 100% | — | — |
| `application.usecase.appointment` | 100% | — | — |
| `application.usecase.auth` | 88,8% | — | — |
| `application.service` | 78,2% | — | abaixo de 80 isoladamente |
| projeto inteiro | 1782/2226 = 80,05% linhas · 304/534 = 56,93% ramos | | |

Dentro do agregado que passa, as classes de controle de acesso ficam abaixo (achado `SEC-02`):

| Classe | Linhas | Instruções |
|---|---|---|
| `LoginAttemptService` | 69,4% | 57,4% |
| `ChangePasswordUseCase` | 75,6% | 68,5% |
| `JwtTokenService` | 72,7% | 73,3% |
| `EmailOutboxProcessor` (`infrastructure.email`) | **0%** (49 linhas) | **0%** |

> A nota de 0% em `EmailOutboxProcessor` foi lida agora do CSV e entrou na matriz de achados como `SEC-14` (Médio). Somar teste do processador — retentativa, backoff, esgotamento de tentativas e transições de estado — é a ação mais barata desta tabela.

**Precisão de número:** "87,77%" é instrução; o gate é medido em linhas (91,25%). Usar os dois, sempre com o contador nomeado.

---

## 5. Bloqueios externos — nada aqui é falha de implementação

| Item | Por que não foi validado | O que destrava | Data/hora em que passa a **V** |
|---|---|---|---|
| Docker Compose (stack completo) | Docker indisponível nesta máquina | rodar `docker compose up` em máquina com Docker; `docker compose ps` com os 4 serviços `healthy` | +30 min |
| Login real pelo compose em `http://localhost` | o compose default é perfil `prod`, com cookies `Secure` → o navegador descarta o cookie | usar HTTPS **ou** `AUTH_COOKIE_SECURE=false` em ambiente local | junto com o item acima |
| GitHub Actions | `.github/` não está versionado e o estado atual não tem commit | commit de checkpoint + push; workflow `ci.yml` executa | após o push |
| OWASP Dependency-Check | perfil `security` existe (`backend/pom.xml`, `failBuildOnCVSS=7`), mas exige `NVD_API_KEY` e baixa base de CVE (~15 min na primeira vez) | `mvn -Psecurity verify` com chave | +20 min após a chave |
| CSRF / `429` / rotação / outbox em runtime | sem servidor de pé | uma sessão de fumaça com o checklist da seção 6 | +45 min |
| Escala horizontal (≥2 réplicas) | restrição de projeto (rate limit em memória, outbox sem *claim*) | decisão de arquitetura nova + store compartilhado | nova iteração |

---

## 6. Checklist de fumaça para fechar `SEC-06` (executar após ter runtime)

```text
[ ] 1  GET  /actuator/health                            → {"status":"UP"}
[ ] 2  GET  /api/auth/csrf                              → set-cookie XSRF-TOKEN
[ ] 3  POST /api/auth/login (senha do seed)             → 200 + 2 cookies HttpOnly, sem token no corpo
[ ] 4  POST /api/appointments sem X-XSRF-TOKEN          → 403 (CSRF ativo)
[ ] 5  POST /api/appointments com X-XSRF-TOKEN          → 201, emailQueued=true, emailSent=false
[ ] 6  5× POST /api/auth/login com senha errada         → 401…401, depois 429 + Retry-After
[ ] 7  expirar o access (15′) e chamar rota protegida    → 401 → refresh → 200, em uma única rotação
[ ] 8  reutilizar o refresh antigo                      → falha de autenticação (uso único)
[ ] 9  SELECT status, attempts FROM email_outbox        → PENDING → SENT; mensagem no MailHog
[ ] 10 criar dois agendamentos para o mesmo orderId     → 201 + 409
[ ] 11 trocar a senha obrigatória                       → 200 e sessões anteriores revogadas (auth_session.revoked_at)
[ ] 12 GET /actuator/prometheus como OPERADOR           → 403; como ADMIN → 200
```

Cada item corresponde a um controle já classificado **C** por leitura de código. Passar nos 12 converte a superfície inteira de **C** para **V**.

---

## 7. Como reproduzir qualquer número deste relatório

```bash
# testes e cobertura (re-executa; ~2 min)
cd backend && mvn verify
python - <<'PY'   # total de testes a partir dos XML
import xml.etree.ElementTree as ET, glob
t=f=e=s=0
for p in glob.glob('target/surefire-reports/*.xml'):
    r=ET.parse(p).getroot()
    t+=int(r.get('tests',0)); f+=int(r.get('failures',0)); e+=int(r.get('errors',0)); s+=int(r.get('skipped',0))
print('tests',t,'failures',f,'errors',e,'skipped',s)
PY

# cobertura dos pacotes do gate (linhas) a partir do CSV do JaCoCo
python - <<'PY'
import csv
rows=list(csv.DictReader(open('target/site/jacoco/jacoco.csv',encoding='utf-8')))
pre=('com.redekrill.scheduling.domain.policy','com.redekrill.scheduling.application.usecase.appointment',
     'com.redekrill.scheduling.application.usecase.auth','com.redekrill.scheduling.application.service')
sel=[r for r in rows if any(r['PACKAGE']==p or r['PACKAGE'].startswith(p+'.') for p in pre)]
mi=sum(int(r['LINE_MISSED']) for r in sel); c=sum(int(r['LINE_COVERED']) for r in sel)
print('gate LINE: %d/%d = %.2f%%'%(c,c+mi,100*c/(c+mi)))
for n in ('LoginAttemptService','ChangePasswordUseCase','JwtTokenService','EmailOutboxProcessor'):
    for r in sel+rows:
        if r['CLASS']==n:
            lm,lc=int(r['LINE_MISSED']),int(r['LINE_COVERED'])
            im,ic=int(r['INSTRUCTION_MISSED']),int(r['INSTRUCTION_COVERED'])
            print('%-22s line %.1f%% instr %.1f%%'%(n,100*lc/(lc+lm),100*ic/(ic+im)))
PY

# frontend
cd ../frontend && npm audit --omit=dev && npm run lint && npm run typecheck && npm run format:check && npm run build

# PDF de entrega (HTML é a fonte editável)
cd .. && "/c/Program Files/Google/Chrome/Application/chrome.exe" --headless=new --disable-gpu \
  --no-pdf-header-footer \
  --print-to-pdf="docs/entrega/Rede-Krill-Scheduling-Entrega.pdf" \
  "file:///$(pwd)/docs/entrega/ENTREGA.html"
```

---

## 8. Veredito

**Pronto, com três afirmativas que não podem ser feitas ainda.** O sistema tem arquitetura limpa e verificada por
ArquUnit, sessão revogável com rotação de refresh, CSRF, lockout de login, troca obrigatória de senha inicial,
cadastros operacionais, capacidade com concorrência tratada, auditoria, relatórios paginados e notificação por
outbox transacional — com 125 testes passando e gate de cobertura crítico superado em linhas (91,25%), portas de
qualidade do frontend verdes nesta sessão e zero vulnerabilidades de produção nas dependências JavaScript.

As três afirmativas pendentes são de **evidência**, não de arquitetura, e todas fecham em menos de duas horas de
trabalho com um runtime disponível:

1. **Não se pode dizer que as dependências do backend estão livres de CVE** (`SEC-03`) — a análise nunca rodou.
2. **Nenhum controle de segurança foi exercitado em runtime** (`SEC-06`) — CSRF, `429`, rotação e outbox estão
   provados no fonte e em testes isolados, não contra o stack completo.
3. **A CI nunca viu este estado** — 156 entradas continuam sem commit e `.github/` não está versionado.

Para uso interno controlado, em instância única, com HTTPS e com o checklist da seção 6 executado uma vez, a
entrega é aceitável. Para escala horizontal, primeiro migre o estado de tentativas de login (`SEC-01`).

**Condição para o registro ficar verdadeiro:** os itens 1 e 2 só mudam de **N** para **V** depois de executados;
nada neste documento antecipa esse resultado.
