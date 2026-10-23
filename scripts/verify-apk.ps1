param(
    [string]$Apk = 'app/build/outputs/apk/debug/app-debug.apk',
    [Parameter(Mandatory = $true)][string]$SdkRoot
)
$ErrorActionPreference = 'Stop'
$taskProject = Split-Path -Parent $PSScriptRoot
$taskConfig = Join-Path $taskProject '.tools/local-env.ps1'
if (Test-Path -LiteralPath $taskConfig) { . $taskConfig }
if (-not [System.IO.Path]::IsPathRooted($Apk)) { $Apk = Join-Path $taskProject $Apk }
$taskBuildTools = Join-Path $SdkRoot 'build-tools/35.0.0'
& (Join-Path $taskBuildTools 'zipalign.exe') -c -P 16 4 $Apk
if ($LASTEXITCODE -ne 0) { throw 'La alineación ZIP para 16 KiB no es válida.' }
& (Join-Path $taskBuildTools 'apksigner.bat') verify --verbose $Apk
if ($LASTEXITCODE -ne 0) { throw 'La firma APK no es válida.' }
$taskPermissions = & (Join-Path $taskBuildTools 'aapt.exe') dump permissions $Apk
if ($LASTEXITCODE -ne 0) { throw 'No se pudo inspeccionar el manifest.' }
$taskPermissions
if ($taskPermissions -match 'android.permission.(INTERNET|MANAGE_EXTERNAL_STORAGE|READ_EXTERNAL_STORAGE|WRITE_EXTERNAL_STORAGE)') {
    throw 'La APK contiene permisos de red o almacenamiento global no autorizados.'
}
Add-Type -AssemblyName System.IO.Compression.FileSystem
$taskZip = [System.IO.Compression.ZipFile]::OpenRead($Apk)
try {
    foreach ($taskEntry in $taskZip.Entries | Where-Object { $_.FullName -match '^lib/(arm64-v8a|x86_64)/.+\.so$' }) {
        $taskMemory = [System.IO.MemoryStream]::new()
        $taskStream = $taskEntry.Open()
        try { $taskStream.CopyTo($taskMemory) } finally { $taskStream.Dispose() }
        $taskBytes = $taskMemory.ToArray()
        $taskMemory.Dispose()
        if ($taskBytes[0] -ne 0x7f -or $taskBytes[1] -ne 0x45 -or $taskBytes[4] -ne 2 -or $taskBytes[5] -ne 1) {
            throw "Formato ELF64 little-endian inesperado: $($taskEntry.FullName)"
        }
        $taskHeaderOffset = [BitConverter]::ToUInt64($taskBytes, 32)
        $taskHeaderSize = [BitConverter]::ToUInt16($taskBytes, 54)
        $taskHeaderCount = [BitConverter]::ToUInt16($taskBytes, 56)
        for ($taskIndex = 0; $taskIndex -lt $taskHeaderCount; $taskIndex++) {
            $taskOffset = [int]($taskHeaderOffset + $taskIndex * $taskHeaderSize)
            if ([BitConverter]::ToUInt32($taskBytes, $taskOffset) -eq 1) {
                $taskAlignment = [BitConverter]::ToUInt64($taskBytes, $taskOffset + 48)
                if ($taskAlignment -lt 16384) { throw "Segmento ELF incompatible con 16 KiB: $($taskEntry.FullName)" }
            }
        }
        Write-Output "ELF 16 KiB: $($taskEntry.FullName)"
    }
} finally { $taskZip.Dispose() }
Get-FileHash -LiteralPath $Apk -Algorithm SHA256
