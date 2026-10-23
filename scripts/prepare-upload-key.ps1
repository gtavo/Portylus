param()
$ErrorActionPreference = 'Stop'
$taskProject = Split-Path -Parent $PSScriptRoot
$taskLocalConfig = Join-Path $taskProject '.tools/local-env.ps1'
if (Test-Path -LiteralPath $taskLocalConfig) { . $taskLocalConfig }
if (-not $env:JAVA_HOME) { throw 'Configura JAVA_HOME con un JDK 17.' }
$taskSigningDir = Join-Path $taskProject '.tools/signing'
$taskStore = Join-Path $taskSigningDir 'portylus-upload.p12'
$taskCredentials = Join-Path $taskSigningDir 'credentials.clixml'
if ((Test-Path -LiteralPath $taskStore) -or (Test-Path -LiteralPath $taskCredentials)) {
    throw 'La clave o las credenciales ya existen. No se sobrescriben ni se regeneran.'
}
New-Item -ItemType Directory -Path $taskSigningDir -Force | Out-Null
$taskBytes = New-Object byte[] 32
$taskRng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try { $taskRng.GetBytes($taskBytes) } finally { $taskRng.Dispose() }
$taskPassword = [Convert]::ToBase64String($taskBytes)
$taskKeytool = Join-Path $env:JAVA_HOME 'bin/keytool.exe'
$env:PORTYLUS_CREATE_PASSWORD = $taskPassword
try {
    & $taskKeytool -genkeypair -alias portylus-upload -keyalg RSA -keysize 3072 -sigalg SHA256withRSA -validity 10000 -storetype PKCS12 -keystore $taskStore -storepass:env PORTYLUS_CREATE_PASSWORD -keypass:env PORTYLUS_CREATE_PASSWORD -dname 'CN=Portylus Upload, OU=gtavo.apps' -noprompt
    if ($LASTEXITCODE -ne 0) { throw 'No se ha podido generar la clave de subida.' }
    $taskSecure = ConvertTo-SecureString -String $taskPassword -AsPlainText -Force
    $taskCredential = [System.Management.Automation.PSCredential]::new('portylus-upload', $taskSecure)
    $taskCredential | Export-Clixml -LiteralPath $taskCredentials
    & $taskKeytool -exportcert -rfc -alias portylus-upload -keystore $taskStore -storepass:env PORTYLUS_CREATE_PASSWORD -file (Join-Path $taskSigningDir 'portylus-upload-certificate.pem')
    if ($LASTEXITCODE -ne 0) { throw 'No se ha podido exportar el certificado público.' }
} finally {
    Remove-Item Env:PORTYLUS_CREATE_PASSWORD -ErrorAction SilentlyContinue
    $taskPassword = $null
}
Write-Output 'Clave de subida creada. Contraseña aleatoria protegida con DPAPI del usuario de Windows.'
Write-Output 'Los archivos de .tools/signing están excluidos de Git y del archivo de fuentes.'
