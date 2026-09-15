[CmdletBinding()]
param(
    [string]$JavaHome = "D:\workspace\.tools\jdk21\jdk-21.0.11+10",
    [string]$MavenHome = "D:\workspace\.tools\maven\apache-maven-3.9.16",
    [string]$MySqlHome = "D:\workspace\.tools\mysql-8.0.27-winx64",
    [string]$DbUsername = "root",
    [string]$DbPassword = ""
)

$ErrorActionPreference = "Stop"

$projectRoot = $PSScriptRoot
$runtimeDir = Join-Path $projectRoot "runtime"
$javaExe = Join-Path $JavaHome "bin\java.exe"
$mavenExe = Join-Path $MavenHome "bin\mvn.cmd"
$mysqlServerExe = Join-Path $MySqlHome "bin\mysqld.exe"
$mysqlClientExe = Join-Path $MySqlHome "bin\mysql.exe"
$mysqlConfig = Join-Path $MySqlHome "my.ini"
$sqlFile = Join-Path $projectRoot "sql\ai_assistant_demo.sql"
$jarFile = Join-Path $projectRoot "target\ai-assistant-demo-0.0.1-SNAPSHOT.jar"

New-Item -ItemType Directory -Path $runtimeDir -Force | Out-Null

function Test-TcpPort {
    param([int]$Port)

    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $result = $client.BeginConnect("127.0.0.1", $Port, $null, $null)
        if (-not $result.AsyncWaitHandle.WaitOne(500)) {
            return $false
        }
        $client.EndConnect($result)
        return $true
    } catch {
        return $false
    } finally {
        $client.Dispose()
    }
}

function Wait-ForPort {
    param(
        [int]$Port,
        [int]$TimeoutSeconds = 30
    )

    for ($i = 0; $i -lt ($TimeoutSeconds * 2); $i++) {
        if (Test-TcpPort -Port $Port) {
            return $true
        }
        Start-Sleep -Milliseconds 500
    }
    return $false
}

foreach ($requiredFile in @($javaExe, $mavenExe, $mysqlServerExe, $mysqlClientExe, $mysqlConfig, $sqlFile)) {
    if (-not (Test-Path -LiteralPath $requiredFile)) {
        throw "缺少运行文件：$requiredFile"
    }
}

Write-Host "[1/4] 检查 MySQL..." -ForegroundColor Cyan
if (-not (Test-TcpPort -Port 3306)) {
    $mysqlProcess = Start-Process `
        -FilePath $mysqlServerExe `
        -ArgumentList "--defaults-file=$mysqlConfig", "--console" `
        -WorkingDirectory $MySqlHome `
        -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $runtimeDir "mysql-out.log") `
        -RedirectStandardError (Join-Path $runtimeDir "mysql-error.log") `
        -PassThru
    $mysqlProcess.Id | Set-Content -LiteralPath (Join-Path $runtimeDir "mysql.pid")

    if (-not (Wait-ForPort -Port 3306)) {
        throw "MySQL 启动失败，请查看 runtime/mysql-error.log"
    }
    Write-Host "MySQL 已启动，PID=$($mysqlProcess.Id)" -ForegroundColor Green
} else {
    Write-Host "MySQL 已在 3306 端口运行" -ForegroundColor Green
}

Write-Host "[2/4] 初始化数据库..." -ForegroundColor Cyan
$oldMysqlPassword = $env:MYSQL_PWD
try {
    $env:MYSQL_PWD = $DbPassword
    & $mysqlClientExe `
        --host=127.0.0.1 `
        --port=3306 `
        --user=$DbUsername `
        --default-character-set=utf8mb4 `
        --execute="source $($sqlFile.Replace('\', '/'))"
    if ($LASTEXITCODE -ne 0) {
        throw "数据库初始化失败"
    }
} finally {
    $env:MYSQL_PWD = $oldMysqlPassword
}
Write-Host "数据库已就绪" -ForegroundColor Green

Write-Host "[3/4] 检查项目构建..." -ForegroundColor Cyan
$needsBuild = -not (Test-Path -LiteralPath $jarFile)
if (-not $needsBuild) {
    $jarTime = (Get-Item -LiteralPath $jarFile).LastWriteTime
    $newerSource = Get-ChildItem -LiteralPath (Join-Path $projectRoot "src"), (Join-Path $projectRoot "pom.xml") -Recurse -File |
        Where-Object { $_.LastWriteTime -gt $jarTime } |
        Select-Object -First 1
    $needsBuild = $null -ne $newerSource
}

if ($needsBuild) {
    $oldJavaHome = $env:JAVA_HOME
    $oldPath = $env:Path
    try {
        $env:JAVA_HOME = $JavaHome
        $env:Path = "$(Join-Path $JavaHome 'bin');$env:Path"
        & $mavenExe -DskipTests package
        if ($LASTEXITCODE -ne 0) {
            throw "Maven 构建失败"
        }
    } finally {
        $env:JAVA_HOME = $oldJavaHome
        $env:Path = $oldPath
    }
    Write-Host "项目构建完成" -ForegroundColor Green
} else {
    Write-Host "JAR 已是最新版本" -ForegroundColor Green
}

Write-Host "[4/4] 检查 Spring Boot..." -ForegroundColor Cyan
if (-not (Test-TcpPort -Port 8080)) {
    $oldDbUsername = $env:DB_USERNAME
    $oldDbPassword = $env:DB_PASSWORD
    try {
        $env:DB_USERNAME = $DbUsername
        $env:DB_PASSWORD = $DbPassword
        $appProcess = Start-Process `
            -FilePath $javaExe `
            -ArgumentList "-jar", $jarFile `
            -WorkingDirectory $projectRoot `
            -WindowStyle Hidden `
            -RedirectStandardOutput (Join-Path $runtimeDir "app-out.log") `
            -RedirectStandardError (Join-Path $runtimeDir "app-error.log") `
            -PassThru
        $appProcess.Id | Set-Content -LiteralPath (Join-Path $runtimeDir "app.pid")
    } finally {
        $env:DB_USERNAME = $oldDbUsername
        $env:DB_PASSWORD = $oldDbPassword
    }

    if (-not (Wait-ForPort -Port 8080)) {
        throw "Spring Boot 启动失败，请查看 runtime/app-error.log 和 runtime/app-out.log"
    }
    Write-Host "Spring Boot 已启动，PID=$($appProcess.Id)" -ForegroundColor Green
} else {
    Write-Host "Spring Boot 已在 8080 端口运行" -ForegroundColor Green
}

Write-Host ""
Write-Host "AI 智能助手 Demo 已启动：" -ForegroundColor Green
Write-Host "http://localhost:8080/"
Write-Host "登录账号：test / 123456"

