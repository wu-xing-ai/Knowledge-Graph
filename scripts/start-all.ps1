# 一键启动知联汇学平台所有服务
# 需要：MySQL 已启动，Neo4j 已解压到 D:\neo4j-community-4.4.4

. (Join-Path $PSScriptRoot "load-env.ps1")
$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Yellow
Write-Host "  知联汇学平台启动脚本" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Yellow
Write-Host ""

# 检查 MySQL
Write-Host "[检查] MySQL 服务..." -ForegroundColor Cyan
$mysql = netstat -ano | Select-String ":3306" | Select-String "LISTENING"
if (-not $mysql) {
    Write-Host "  [错误] MySQL 未启动或 3306 端口未监听，请先启动 MySQL" -ForegroundColor Red
    exit 1
}
Write-Host "  [OK] MySQL 正在运行" -ForegroundColor Green

# 1. 启动 Neo4j
Write-Host "`n[1/3] 启动 Neo4j..." -ForegroundColor Cyan
$JAVA11 = "C:\Program Files\Eclipse Adoptium\jdk-11.0.32.9-hotspot"
$neoHome = "D:\neo4j-community-4.4.4"

if (-not (Test-Path $neoHome)) {
    Write-Host "  [错误] Neo4j 目录不存在: $neoHome" -ForegroundColor Red
    exit 1
}

$neoJob = $null
$neoRunning = netstat -ano | Select-String ":7687" | Select-String "LISTENING"
if ($neoRunning) {
    Write-Host "  [OK] Neo4j 已在运行，跳过启动 (bolt://localhost:7687)" -ForegroundColor Green
} else {
    $neoJob = Start-Job -ScriptBlock {
        param($javaHome, $neoHome)
        $env:JAVA_HOME = $javaHome
        Set-Location $neoHome
        & "bin\neo4j.bat" console
    } -ArgumentList $JAVA11, $neoHome

    Write-Host "  Neo4j 启动中 (Job ID: $($neoJob.Id))..." -ForegroundColor Gray

    # 等待 Neo4j 就绪
    $maxWait = 60
    $waited = 0
    do {
        Start-Sleep -Seconds 2
        $waited += 2
        $ready = netstat -ano | Select-String ":7687" | Select-String "LISTENING"
    } while (-not $ready -and $waited -lt $maxWait)

    if ($ready) {
        Write-Host "  [OK] Neo4j 已就绪 (bolt://localhost:7687)" -ForegroundColor Green
    } else {
        Write-Host "  [警告] Neo4j 可能尚未就绪，继续启动后端..." -ForegroundColor Yellow
    }
}

# 2. 启动后端
Write-Host "`n[2/3] 启动 Spring Boot 后端..." -ForegroundColor Cyan
$JAVA17 = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot"
$env:JAVA_HOME = $JAVA17
$env:Path = "$JAVA17\bin;$env:Path"
$env:MAVEN_OPTS = "-Dfile.encoding=UTF-8"

$backendDir = Join-Path $PSScriptRoot "..\backend"
Set-Location $backendDir

# 检查 Maven
$mvn = Get-Command mvn -ErrorAction SilentlyContinue
if (-not $mvn) {
    Write-Host "  [错误] 未找到 mvn 命令，请确保 Maven 在 PATH 中" -ForegroundColor Red
    exit 1
}

# 若旧后端仍在运行，jar 文件会被占用导致 mvn clean 失败，先停止所有本项目后端进程
$backendProcesses = Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction SilentlyContinue |
    Where-Object { $_.CommandLine -match 'zhi-platform-1\.0\.0\.jar' }
foreach ($bp in $backendProcesses) {
    Write-Host "  检测到旧后端 (PID $($bp.ProcessId)) 正在运行，先停止以解锁 jar..." -ForegroundColor Yellow
    Stop-Process -Id $bp.ProcessId -Force -ErrorAction SilentlyContinue
}
if ($backendProcesses) {
    Start-Sleep -Seconds 2
}
Write-Host "  编译并打包后端..." -ForegroundColor Gray
mvn clean package -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "  [错误] 后端编译失败" -ForegroundColor Red
    exit 1
}

# 后台启动后端
$backendJob = Start-Job -ScriptBlock {
    param($javaHome, $backendDir)
    $env:JAVA_HOME = $javaHome
    $env:Path = "$javaHome\bin;$env:Path"
    Set-Location $backendDir
    java "-Dfile.encoding=UTF-8" -jar target/zhi-platform-1.0.0.jar
} -ArgumentList $JAVA17, $backendDir

Write-Host "  后端启动中 (Job ID: $($backendJob.Id))..." -ForegroundColor Gray

# 等待后端就绪
$waited = 0
do {
    Start-Sleep -Seconds 2
    $waited += 2
    $ready = netstat -ano | Select-String ":8080" | Select-String "LISTENING"
} while (-not $ready -and $waited -lt 60)

if ($ready) {
    Write-Host "  [OK] 后端已就绪 (http://localhost:8080)" -ForegroundColor Green
} else {
    Write-Host "  [警告] 后端可能尚未就绪，最近输出如下：" -ForegroundColor Yellow
    Receive-Job -Id $backendJob.Id 2>&1 | Select-Object -Last 40 | ForEach-Object { Write-Host "    $_" -ForegroundColor Gray }
}

# 3. 启动前端
Write-Host "`n[3/3] 启动前端开发服务器..." -ForegroundColor Cyan
$frontendDir = Join-Path $PSScriptRoot "..\frontend"

$frontendRunning = netstat -ano | Select-String ":3000" | Select-String "LISTENING"
if ($frontendRunning) {
    Write-Host "  [OK] 前端开发服务器已在运行，跳过启动 (http://localhost:3000)" -ForegroundColor Green
} else {
    if (-not (Test-Path (Join-Path $frontendDir "node_modules"))) {
        Write-Host "  首次运行，安装前端依赖..." -ForegroundColor Gray
        Set-Location $frontendDir
        npm install
    }

    $frontendJob = Start-Job -ScriptBlock {
        param($frontendDir)
        Set-Location $frontendDir
        npm run dev
    } -ArgumentList $frontendDir

    Write-Host "  前端启动中 (Job ID: $($frontendJob.Id))..." -ForegroundColor Gray
    Start-Sleep -Seconds 3
}

Write-Host "`n========================================" -ForegroundColor Yellow
Write-Host "  服务状态:" -ForegroundColor Yellow
Write-Host "  Neo4j Browser: http://localhost:7474" -ForegroundColor White
Write-Host "  Backend API:   http://localhost:8080" -ForegroundColor White
Write-Host "  Frontend:      http://localhost:3000" -ForegroundColor White
Write-Host "========================================" -ForegroundColor Yellow
Write-Host "`n后台 Job 查看: Get-Job" -ForegroundColor Gray
Write-Host "后台 Job 停止: Stop-Job <id>; Remove-Job <id>" -ForegroundColor Gray
Write-Host "按 Ctrl+C 退出本脚本不会停止后台服务" -ForegroundColor Gray
