. (Join-Path $PSScriptRoot "load-env.ps1")
$JAVA17 = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot"
$env:JAVA_HOME = $JAVA17
$env:Path = "C:\temp\maven\apache-maven-3.9.6\bin;$env:PATH"
$env:MAVEN_OPTS = "-Dfile.encoding=UTF-8"

Write-Host "JAVA_HOME set to JDK 17 ($JAVA17)" -ForegroundColor Green

# 若旧后端仍在运行，先停止，避免 jar 文件被占用导致 mvn clean 失败
$existingBackend = netstat -ano | Select-String ":8080" | Select-String "LISTENING" | Select-Object -First 1
if ($existingBackend) {
    $oldPid = ($existingBackend -split '\s+')[-1]
    Write-Host "检测到旧后端 (PID $oldPid) 正在运行，先停止..." -ForegroundColor Yellow
    Stop-Process -Id $oldPid -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 2
}Write-Host "Starting Spring Boot backend..." -ForegroundColor Cyan

Set-Location -Path (Join-Path $PSScriptRoot "..\backend")
mvn.cmd --% clean package -DskipTests; if ($?) { java -jar target/zhi-platform-1.0.0.jar }
