# 本机验收启动器：不建表、不保存密码、不关闭其他服务、不改变原项目配置。
param(
    [string]$JavaPath = 'java.exe',
    [string]$NodePath = 'node.exe'
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$jarPath = Join-Path $projectRoot 'target\LabCore-0.0.1-SNAPSHOT.jar'
$frontPath = Join-Path $projectRoot 'web-study-1.0.1\package'
$vitePath = Join-Path $frontPath 'node_modules\vite\bin\vite.js'
if (!(Test-Path -LiteralPath $jarPath) -or !(Test-Path -LiteralPath $vitePath)) {
    throw '请先构建后端并安装前端依赖，详见 docs/favorites-learning-plan-deployment.md。'
}
foreach ($port in @(8080, 5175)) {
    if (Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue) {
        throw "端口 $port 已被占用；本脚本不会停止其他项目，请先核对服务。"
    }
}
$javaExecutable = (Get-Command $JavaPath -ErrorAction Stop).Source
$nodeExecutable = (Get-Command $NodePath -ErrorAction Stop).Source
$logPath = Join-Path $projectRoot 'logs\module-local'
New-Item -ItemType Directory -Path $logPath -Force | Out-Null
$envNames = @('LABCORE_DB_PASSWORD','LABCORE_ACCESS_TOKEN_SECRET','LABCORE_CORS_ALLOWED_ORIGINS','LABCORE_SERVER_ADDRESS')
$previous = @{}
foreach ($envName in $envNames) { $previous[$envName] = [Environment]::GetEnvironmentVariable($envName, 'Process') }
$backendProcess = $null
try {
    if (!$env:LABCORE_DB_PASSWORD) {
        $securePassword = Read-Host '请输入本机 MySQL 密码（仅传给后台进程，不保存）' -AsSecureString
        $passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
        try { $env:LABCORE_DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer) }
        finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer); $securePassword.Dispose() }
    }
    if (!$env:LABCORE_ACCESS_TOKEN_SECRET) {
        $randomBytes = New-Object byte[] 32
        $randomSource = [Security.Cryptography.RandomNumberGenerator]::Create()
        try { $randomSource.GetBytes($randomBytes) } finally { $randomSource.Dispose() }
        $env:LABCORE_ACCESS_TOKEN_SECRET = [Convert]::ToBase64String($randomBytes)
        Write-Host '使用本地临时登录密钥：本次服务重启后请重新登录。正式部署请沿用原安全密钥。'
    }
    $env:LABCORE_CORS_ALLOWED_ORIGINS = if ($previous['LABCORE_CORS_ALLOWED_ORIGINS']) { $previous['LABCORE_CORS_ALLOWED_ORIGINS'] + ',http://127.0.0.1:5175' } else { 'https://labcore.henu.edu.cn,http://localhost:5173,http://127.0.0.1:5173,http://localhost:8080,http://127.0.0.1:8080,http://127.0.0.1:5175' }
    $env:LABCORE_SERVER_ADDRESS = '127.0.0.1'
    $backendProcess = Start-Process -FilePath $javaExecutable -ArgumentList ('-jar "{0}"' -f $jarPath) -WorkingDirectory $projectRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $logPath 'backend.out') -RedirectStandardError (Join-Path $logPath 'backend.err')
    # 等待仅限本次启动的进程；失败时仅清理该进程，不影响其他 Java/Node。
    $healthy = $false
    for ($attempt = 0; $attempt -lt 45; $attempt++) {
        if ($backendProcess.HasExited) { throw '后端启动退出，请查看 logs/module-local/backend.err 和 backend.out。' }
        try { $health = Invoke-RestMethod -Uri 'http://127.0.0.1:8080/actuator/health' -TimeoutSec 1; if ($health.status -eq 'UP') { $healthy = $true; break } } catch {}
        Start-Sleep -Milliseconds 500
    }
    if (!$healthy) { throw '后端健康检查未通过，请核对数据库及日志。' }
    $frontendProcess = Start-Process -FilePath $nodeExecutable -ArgumentList ('"{0}" --host 127.0.0.1 --port 5175 --strictPort' -f $vitePath) -WorkingDirectory $frontPath -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $logPath 'frontend.out') -RedirectStandardError (Join-Path $logPath 'frontend.err')
    Write-Host ('后端 PID={0}，前端 PID={1}' -f $backendProcess.Id, $frontendProcess.Id)
    Write-Host '访问 http://127.0.0.1:5175 并使用原平台账号登录。数据库密码不是平台密码。'
} catch {
    if ($backendProcess -and !$backendProcess.HasExited) { Stop-Process -Id $backendProcess.Id }
    throw
} finally {
    foreach ($envName in $envNames) { [Environment]::SetEnvironmentVariable($envName, $previous[$envName], 'Process') }
}
