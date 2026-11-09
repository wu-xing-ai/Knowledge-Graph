# 知联汇学 - 停止本地后端（释放 8080 端口，打包前必做）
# 运行：.\scripts\stop-backend.ps1

$port = 8080
$connections = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue

if (-not $connections) {
    Write-Host "8080 端口没有监听进程，后端未在运行" -ForegroundColor Yellow
    exit 0
}

$pids = $connections | Select-Object -ExpandProperty OwningProcess -Unique
foreach ($procId in $pids) {
    $proc = Get-Process -Id $procId -ErrorAction SilentlyContinue
    if ($proc) {
        Write-Host "停止后端进程: $($proc.ProcessName) (PID $procId)" -ForegroundColor Cyan
        Stop-Process -Id $procId -Force
    }
}
Write-Host "后端已停止，8080 端口已释放" -ForegroundColor Green
