# Run from the project root after starting PostgreSQL: docker compose up -d db
$ErrorActionPreference = 'Stop'
$project = Split-Path $PSScriptRoot -Parent
Push-Location $project
try {
    if (-not (Test-Path -LiteralPath '.env')) { throw 'Copy .env.example to .env and fill in your local values first.' }
    # Ask Compose to parse .env correctly, including quoting and interpolation.
    $configText = docker compose config --format json
    if ($LASTEXITCODE -ne 0) { throw 'Docker Compose could not read the configuration.' }
    $config = $configText | ConvertFrom-Json
    $env:DB_URL = "jdbc:postgresql://localhost:$($config.services.db.ports[0].published)/universitydb"
    $env:DB_USERNAME = $config.services.db.environment.POSTGRES_USER
    $env:DB_PASSWORD = $config.services.db.environment.POSTGRES_PASSWORD
    $env:BOOTSTRAP_ADMIN_NAME = $config.services.app.environment.BOOTSTRAP_ADMIN_NAME
    $env:BOOTSTRAP_ADMIN_EMAIL = $config.services.app.environment.BOOTSTRAP_ADMIN_EMAIL
    $env:BOOTSTRAP_ADMIN_PASSWORD = $config.services.app.environment.BOOTSTRAP_ADMIN_PASSWORD
    $env:SERVER_PORT = $config.services.app.ports[0].published
    & .\mvnw.cmd spring-boot:run
    if ($LASTEXITCODE -ne 0) { throw "Application exited with code $LASTEXITCODE" }
} finally {
    Remove-Item Env:DB_PASSWORD,Env:BOOTSTRAP_ADMIN_PASSWORD -ErrorAction SilentlyContinue
    Pop-Location
}
