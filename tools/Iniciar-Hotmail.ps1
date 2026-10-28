param([string]$Cuenta = 'rinconcito.demo@hotmail.com')

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$authorizationPath = Join-Path $projectRoot '.local\outlook-oauth.properties'
$configurationPath = Join-Path $projectRoot '.local\application.properties'
# Compatibilidad con la configuracion privada de la primera prueba local.
if (!(Test-Path -LiteralPath $configurationPath)) {
    $configurationPath = Join-Path $projectRoot '.local\hotmail.properties'
}
if (!(Test-Path -LiteralPath $authorizationPath)) {
    throw 'Primero ejecuta python tools/autorizar_outlook.py --client-id TU_ID --cuenta TU_HOTMAIL y autoriza en Microsoft.'
}
if (!(Test-Path -LiteralPath $configurationPath)) {
    throw 'Falta .local/application.properties con la conexion local a MySQL. Consulta docs/CORREO_HOTMAIL.md.'
}
if (Get-NetTCPConnection -LocalPort 8081,8082 -State Listen -ErrorAction SilentlyContinue) {
    throw 'Deten las instancias anteriores en 8081 y 8082 antes de iniciar el demo con correo.'
}

$previousMailEnvironment = @{}
foreach ($variableName in @('SMTP_USERNAME','SMTP_FROM','CORREO_HABILITADO','OUTLOOK_TOKEN_FILE','SMTP_PASSWORD')) {
    $previousMailEnvironment[$variableName] = [Environment]::GetEnvironmentVariable($variableName, 'Process')
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
        "-Dspring-boot.run.arguments=--spring.config.additional-location=$([Uri]::new($configurationPath).AbsoluteUri)" 'spring-boot:run'
    if ($LASTEXITCODE -ne 0) { throw "Maven termino con codigo $LASTEXITCODE. Revisa el error de arranque." }
} finally {
    Pop-Location
    foreach ($variableName in $previousMailEnvironment.Keys) {
        [Environment]::SetEnvironmentVariable($variableName, $previousMailEnvironment[$variableName], 'Process')
    }
}
