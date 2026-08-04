# 加载项目根目录 .env 文件到当前进程环境变量
# 用法：在 start-*.ps1 中 . (Join-Path $PSScriptRoot "load-env.ps1")
$envFile = Join-Path $PSScriptRoot "..\.env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
            $parts = $line -split "=", 2
            $name = $parts[0].Trim()
            $value = $parts[1].Trim()
            if ($name) {
                Set-Item -Path "Env:$name" -Value $value
            }
        }
    }
    Write-Host "  [OK] 已从 .env 加载环境变量" -ForegroundColor Green
}