$ErrorActionPreference = 'Stop'
$destination = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../.env'))
if (Test-Path -LiteralPath $destination) { throw '.env already exists; it will not be overwritten.' }
function New-LocalSecret { [Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(32)).ToLowerInvariant() }
$graylogPassword = New-LocalSecret
$graylogHash = [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes($graylogPassword))).ToLowerInvariant()
$lines = @(
    "OPERATIONS_TOKEN=$(New-LocalSecret)",
    "GRAYLOG_PASSWORD_SECRET=$(New-LocalSecret)$(New-LocalSecret)",
    "GRAYLOG_ADMIN_PASSWORD=$graylogPassword",
    "GRAYLOG_ROOT_PASSWORD_SHA2=$graylogHash"
)
[System.IO.File]::WriteAllLines($destination, $lines, [System.Text.UTF8Encoding]::new($false))
Write-Output 'Local .env generated. Graylog login: admin; password is stored in GRAYLOG_ADMIN_PASSWORD. Do not commit this file.'
