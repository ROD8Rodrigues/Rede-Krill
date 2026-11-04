<#
    start-dev.ps1 — sobe o backend no perfil dev carregando o .env da raiz.

    Por que este script existe: o Spring Boot nao le arquivo .env (o projeto nao tem
    a dependencia spring-dotenv). Quem le .env e o docker compose. Rodando .\mvnw.cmd
    direto, SPRING_PROFILES_ACTIVE fica vazio -> perfil base -> seed desligado e
    JWT_SECRET obrigatorio ("JWT_SECRET e obrigatorio").

    Uso:
      powershell -ExecutionPolicy Bypass -File .\start-dev.ps1
#>

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$envFile = Join-Path $root '.env'

if (-not (Test-Path $envFile)) {
    Write-Host "Arquivo .env nao encontrado em $root" -ForegroundColor Red
    Write-Host "Copie o .env.example para .env e preencha os valores de desenvolvimento."
    exit 1
}

$pares = 0
foreach ($linha in Get-Content -Path $envFile -Encoding UTF8) {
    $t = $linha.Trim()
    if ($t -eq '' -or $t.StartsWith('#')) { continue }

    $i = $t.IndexOf('=')
    if ($i -lt 1) { continue }

    $nome = $t.Substring(0, $i).Trim()
    $valor = $t.Substring($i + 1).Trim()

    if ($valor.Length -ge 2 -and (($valor.StartsWith('"') -and $valor.EndsWith('"')) -or ($valor.StartsWith("'") -and $valor.EndsWith("'")))) {
        $valor = $valor.Substring(1, $valor.Length - 2)
    }

    Set-Item -Path ("Env:$nome") -Value $valor
    $pares++
}

# --- Mapeamento compose -> variaveis lidas pelo Spring Boot ---
# O .env descreve a stack do docker compose (POSTGRES_*), mas o backend le DB_*.
# O compose faz isso no bloco environment: do docker-compose.yml; aqui e a mesma coisa
# para a execucao local sem Docker. Valores ja presentes no ambiente/nao sao sobrescritos.
if (-not $env:DB_URL) {
    $banco = 'rede_krill'
    if ($env:POSTGRES_DB) { $banco = $env:POSTGRES_DB }
    $hostDb = 'localhost'
    if ($env:POSTGRES_HOST) { $hostDb = $env:POSTGRES_HOST }
    $portaDb = '5432'
    if ($env:POSTGRES_PORT) { $portaDb = $env:POSTGRES_PORT }
    $env:DB_URL = 'jdbc:postgresql://' + $hostDb + ':' + $portaDb + '/' + $banco
}
if (-not $env:DB_USERNAME -and $env:POSTGRES_USER) { $env:DB_USERNAME = $env:POSTGRES_USER }
if (-not $env:DB_PASSWORD -and $env:POSTGRES_PASSWORD) { $env:DB_PASSWORD = $env:POSTGRES_PASSWORD }

if ([string]::IsNullOrWhiteSpace($env:SPRING_PROFILES_ACTIVE)) {
    $env:SPRING_PROFILES_ACTIVE = 'dev'
    Write-Host 'SPRING_PROFILES_ACTIVE ausente no .env; usando dev.' -ForegroundColor Yellow
}

if ([string]::IsNullOrWhiteSpace($env:JWT_SECRET)) {
    $bytes = New-Object byte[] 32
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    $env:JWT_SECRET = [Convert]::ToBase64String($bytes)
    Write-Host 'JWT_SECRET ausente no .env; gerado um segredo efemero (tokens caem a cada restart).' -ForegroundColor Yellow
}

# Sem Docker o backend nao esta na rede do compose: o SMTP e o host local e nao ha
# proxy na frente dele. Os dois padroes abaixo so entram quando a variavel esta ausente,
# exatamente como o mapeamento DB_* acima — quem definiu no .env mantem o valor.
if ([string]::IsNullOrWhiteSpace($env:MAIL_HOST)) {
    $env:MAIL_HOST = 'localhost'
    Write-Host 'MAIL_HOST ausente no .env; usando localhost (MailHog publicado na porta do host).' -ForegroundColor Yellow
}
if ([string]::IsNullOrWhiteSpace($env:TRUST_FORWARDED_HEADERS)) {
    $env:TRUST_FORWARDED_HEADERS = 'false'
    Write-Host 'TRUST_FORWARDED_HEADERS ausente no .env; usando false (backend exposto sem proxy).' -ForegroundColor Yellow
}

$porta = '8080'
if ($env:BACKEND_PORT) { $porta = $env:BACKEND_PORT }
Write-Host ('.env carregado: ' + $pares + ' variaveis | perfil=' + $env:SPRING_PROFILES_ACTIVE + ' | porta=' + $porta)

$backend = Join-Path $root 'backend'
Push-Location $backend
try {
    & .\mvnw.cmd spring-boot:run
} finally {
    Pop-Location
}
