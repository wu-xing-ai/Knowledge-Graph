# 知联汇学 - 一键打包部署包
# 运行：.\scripts\build-deploy.ps1
# 产物：项目根目录 deploy/ 文件夹（上传到服务器后 bash install.sh）

$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
$out = Join-Path $root "deploy"

Write-Host "===== 1/3 打包后端 =====" -ForegroundColor Cyan
Push-Location (Join-Path $root "backend")
try {
    $env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot"
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    $env:MAVEN_OPTS = "-Dfile.encoding=UTF-8"
    mvn clean package -DskipTests
    if ($LASTEXITCODE -ne 0) { throw "后端打包失败" }
} finally { Pop-Location }

Write-Host "===== 2/3 打包前端 =====" -ForegroundColor Cyan
Push-Location (Join-Path $root "frontend")
try {
    # 清空旧构建产物，避免累积过期哈希文件
    if (Test-Path "dist") { Remove-Item -Recurse -Force "dist" }
    # 清理 public 下误入的临时目录（如 brand 工具生成的 venv）
    Get-ChildItem "public" -Directory -ErrorAction SilentlyContinue | Where-Object { $_.Name -in @("brand", ".brandtmp-venv") } | Remove-Item -Recurse -Force
    npm run build
    if ($LASTEXITCODE -ne 0) { throw "前端打包失败" }
} finally { Pop-Location }

Write-Host "===== 3/3 组装部署包 =====" -ForegroundColor Cyan
# 清理旧产物
if (Test-Path $out) { Remove-Item -Recurse -Force $out }
New-Item -ItemType Directory -Force -Path "$out\backend","$out\frontend","$out\nginx","$out\systemd" | Out-Null

Copy-Item (Join-Path $root "backend\target\zhi-platform-1.0.0.jar") "$out\backend\" -Force
Copy-Item (Join-Path $root "backend\src\main\resources\schema.sql") "$out\backend\" -Force
Copy-Item (Join-Path $root "backend\src\main\resources\neo4j-init.cypher") "$out\backend\" -Force
Copy-Item (Join-Path $root "frontend\dist\*") "$out\frontend\" -Recurse -Force
# 排除误入的 brand/venv 等临时目录
Get-ChildItem "$out\frontend" -Directory -Recurse -ErrorAction SilentlyContinue | Where-Object { $_.Name -in @("brand", ".brandtmp-venv", ".venv", "node_modules") } | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue

Copy-Item (Join-Path $PSScriptRoot "deploy-templates\nginx.conf") "$out\nginx\zhi-platform.conf" -Force
Copy-Item (Join-Path $PSScriptRoot "deploy-templates\zhi-platform.service") "$out\systemd\" -Force
Copy-Item (Join-Path $PSScriptRoot "deploy-templates\install.sh") "$out\" -Force
Copy-Item (Join-Path $PSScriptRoot "deploy-templates\env.example") "$out\.env.example" -Force

Write-Host ""
Write-Host "===== 部署包已生成: $out =====" -ForegroundColor Green
Write-Host "上传整个 deploy/ 目录到服务器后："
Write-Host "  1) 编辑 .env.example 为 /etc/zhi-platform.env（填数据库/密钥）"
Write-Host "  2) bash install.sh"