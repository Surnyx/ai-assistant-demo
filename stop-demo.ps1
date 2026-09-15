[CmdletBinding()]
param(
    [string]$MySqlHome = "D:\workspace\.tools\mysql-8.0.27-winx64",
    [string]$DbUsername = "root",
    [string]$DbPassword = ""
)

$ErrorActionPreference = "Stop"
$runtimeDir = Join-Path $PSScriptRoot "runtime"
$appPidFile = Join-Path $runtimeDir "app.pid"
$mysqlAdminExe = Join-Path $MySqlHome "bin\mysqladmin.exe"

if (Test-Path -LiteralPath $appPidFile) {
    $appProcessId = [int](Get-Content -Raw -LiteralPath $appPidFile)
    $appProcess = Get-Process -Id $appProcessId -ErrorAction SilentlyContinue
    if ($appProcess -and $appProcess.ProcessName -eq "java") {
        Stop-Process -Id $appProcessId
        Write-Host "Spring Boot 已停止"
    }
}

if (Test-Path -LiteralPath $mysqlAdminExe) {
    $oldMysqlPassword = $env:MYSQL_PWD
    try {
        $env:MYSQL_PWD = $DbPassword
        & $mysqlAdminExe --host=127.0.0.1 --port=3306 --user=$DbUsername shutdown 2>$null
    } finally {
        $env:MYSQL_PWD = $oldMysqlPassword
    }
    Write-Host "MySQL 已停止"
}

