param([string]$Cuenta = 'rinconcitodemo@hotmail.com')

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$authorizationPath = Join-Path $projectRoot '.local\outlook-oauth.properties'
$configurationPath = Join-Path $projectRoot '.local\hotmail.properties'
if (!(Test-Path -LiteralPath $authorizationPath)) {
    throw 'Primero ejecuta python tools/autorizar_outlook.py --client-id TU_ID --cuenta TU_HOTMAIL y autoriza en Microsoft.'
}
if (!(Test-Path -LiteralPath $configurationPath)) {
    throw 'Falta .local/hotmail.properties con la conexion local a MySQL. Consulta docs/CORREO_HOTMAIL.md.'
}
if (Get-NetTCPConnection -LocalPort 8082 -State Listen -ErrorAction SilentlyContinue) {
    throw 'Deten primero la copia de prueba en 8082: comparte la cola y podria capturar mensajes destinados al SMTP real.'
}

$env:SMTP_USERNAME = $Cuenta
$env:SMTP_FROM = $Cuenta
$env:CORREO_HABILITADO = 'true'
$env:OUTLOOK_TOKEN_FILE = $authorizationPath
Remove-Item Env:SMTP_PASSWORD -ErrorAction SilentlyContinue
Write-Output "Iniciando correo real desde $Cuenta. La contrasena de la cuenta no se utiliza."
Push-Location -LiteralPath $projectRoot
try {
    & (Join-Path $projectRoot 'mvnw.cmd') '-Dspring-boot.run.profiles=hotmail' `
        '-Dspring-boot.run.arguments=--spring.config.additional-location=file:./.local/hotmail.properties' 'spring-boot:run'
} finally {
    Pop-Location
}
