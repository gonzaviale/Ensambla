# Levanta Postgres y el backend de Ensambla leyendo las fotos con un modelo de Anthropic (solo para pruebas).
#
# La API key se pide por pantalla (no se muestra) y vive unicamente en esta ventana: no se guarda en ningun
# archivo. Si ya la tenes en el entorno de esta ventana (ENSAMBLA_IA_API_KEY), no la vuelve a pedir.
#
#   .\scripts\levantar-backend-ia.ps1                      # modelo por defecto
#   .\scripts\levantar-backend-ia.ps1 -Modelo claude-haiku-5-5   # mas barato, menos preciso con letra a mano
param(
    [string]$Modelo = 'claude-sonnet-5-5',
    [switch]$Simulado
)

$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent $PSScriptRoot

if (-not $Simulado) {
    if (-not $env:ENSAMBLA_IA_API_KEY) {
        $segura = Read-Host 'API key de Anthropic (no se muestra)' -AsSecureString
        $puntero = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($segura)
        try { $env:ENSAMBLA_IA_API_KEY = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($puntero) }
        finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($puntero) }
    }
    if (-not $env:ENSAMBLA_IA_API_KEY) { throw 'Hace falta la API key para leer con Anthropic (o usa -Simulado).' }
    $env:ENSAMBLA_IA_PROVEEDOR = 'anthropic'
    $env:ENSAMBLA_IA_MODELO = $Modelo
    Write-Host "Lectura de fotos con Anthropic, modelo $Modelo."
} else {
    $env:ENSAMBLA_IA_PROVEEDOR = 'simulado'
    Write-Host 'Lectura SIMULADA: devuelve siempre la misma pagina de ejemplo.'
}

Write-Host 'Levantando Postgres...'
docker compose -f "$raiz\backend\docker-compose.yml" up -d postgres | Out-Null

Write-Host 'Levantando el backend en http://localhost:8080 (Ctrl+C para frenarlo)...'
Set-Location "$raiz\backend"
.\mvnw.cmd -o spring-boot:run
