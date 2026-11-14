$ErrorActionPreference = 'Stop'
$envPath = Join-Path $PSScriptRoot '.env'
$examplePath = Join-Path $PSScriptRoot '.env.example'
$content = if (Test-Path -LiteralPath $envPath) {
    [System.IO.File]::ReadAllText($envPath)
} else {
    [System.IO.File]::ReadAllText($examplePath)
}
$match = [regex]::Match($content, '(?m)^JWT_SECRET=([^\r\n]*)')
if ($match.Success -and $match.Groups[1].Value.Trim().Length -gt 0) {
    Write-Output 'JWT signing key already configured in .env.'
    exit 0
}
$keyBytes = New-Object byte[] 32
$random = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try { $random.GetBytes($keyBytes) } finally { $random.Dispose() }
$key = [Convert]::ToBase64String($keyBytes)
if ($match.Success) {
    $content = [regex]::Replace($content, '(?m)^JWT_SECRET=[^\r\n]*', ('JWT_SECRET=' + $key))
} else {
    $content = $content.TrimEnd() + "`nJWT_SECRET=" + $key + "`n"
}
[System.IO.File]::WriteAllText($envPath, $content, (New-Object System.Text.UTF8Encoding($false)))
Write-Output 'Generated a random JWT signing key in .env. Keep this file private.'
