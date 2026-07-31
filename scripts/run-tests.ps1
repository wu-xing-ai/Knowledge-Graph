# 一键运行前后端自动化测试
# 用法: .\scripts\run-tests.ps1

$ErrorActionPreference = "Stop"

Write-Host "=== 运行后端测试 ===" -ForegroundColor Cyan
Push-Location backend
try {
    $env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot"
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    mvn test
    if ($LASTEXITCODE -ne 0) {
        throw "后端测试失败"
    }
} finally {
    Pop-Location
}

Write-Host "`n=== 运行前端测试 ===" -ForegroundColor Cyan
Push-Location frontend
try {
    npm test
    if ($LASTEXITCODE -ne 0) {
        throw "前端测试失败"
    }
} finally {
    Pop-Location
}

Write-Host "`n=== 全部测试通过 ===" -ForegroundColor Green
