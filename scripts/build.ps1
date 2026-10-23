param(
    [string[]]$Tasks = @('assembleDebug', 'testDebugUnitTest', 'lintDebug'),
    [switch]$Offline
)
$ErrorActionPreference = 'Stop'
$taskProject = Split-Path -Parent $PSScriptRoot
Push-Location $taskProject
try {
    $taskLocalConfig = Join-Path $taskProject '.tools/local-env.ps1'
    if (Test-Path -LiteralPath $taskLocalConfig) { . $taskLocalConfig }
    if (-not $env:JAVA_HOME) { throw 'Configura JAVA_HOME con un JDK 17.' }
    $taskCredentials = Join-Path $taskProject '.tools/signing/credentials.clixml'
    if (-not $env:PORTYLUS_KEYSTORE_PATH -and -not $env:PDF_KEYSTORE_PATH -and (Test-Path -LiteralPath $taskCredentials)) {
        $taskCredential = Import-Clixml -LiteralPath $taskCredentials
        $env:PORTYLUS_KEYSTORE_PATH = Join-Path $taskProject '.tools/signing/portylus-upload.p12'
        $env:PORTYLUS_KEY_ALIAS = $taskCredential.UserName
        $env:PORTYLUS_KEYSTORE_PASSWORD = $taskCredential.GetNetworkCredential().Password
        $env:PORTYLUS_KEY_PASSWORD = $env:PORTYLUS_KEYSTORE_PASSWORD
    }

    $env:GRADLE_USER_HOME = Join-Path $taskProject '.tools/gradle-user-home'
    $env:ANDROID_USER_HOME = Join-Path $taskProject '.tools/android-user'
    $taskArguments = @($Tasks) + @('--console=plain', '--no-daemon')
    if ($Offline) { $taskArguments += '--offline' }
    & (Join-Path $taskProject 'gradlew.bat') @taskArguments
    if ($LASTEXITCODE -ne 0) { throw "Gradle ha terminado con código $LASTEXITCODE." }
} finally { Pop-Location }
