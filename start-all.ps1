Write-Host "=============================================" -ForegroundColor Green
Write-Host "   CryptoVault - Service Orchestrator        " -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Green
Write-Host ""

# Define ports for each service
$servicePorts = @{
    "api-gateway"          = 8080
    "wallet-service"       = 8081
    "transaction-service"  = 8082
    "auth-service"         = 8083
    "notification-service" = 8084
    "risk-service"         = 8085
    "audit-service"        = 8086
    "kyc-service"          = 8087
}

$servicesOrder = @(
    "auth-service",
    "wallet-service",
    "transaction-service",
    "notification-service",
    "risk-service",
    "audit-service",
    "kyc-service",
    "api-gateway"
)

# 1. Check port usage and free them if needed
Write-Host "Checking for port conflicts..." -ForegroundColor Yellow
foreach ($service in $servicesOrder) {
    $port = $servicePorts[$service]
    $proc = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($proc) {
        $pid = $proc.OwningProcess
        Write-Host "Port $port is currently in use by process ID $pid. Stopping process..." -ForegroundColor Cyan
        Stop-Process -Id $pid -Force -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 1
    }
}

# 2. Launch all backend services in separate windows
Write-Host ""
Write-Host "Launching microservices..." -ForegroundColor Yellow

foreach ($service in $servicesOrder) {
    $port = $servicePorts[$service]
    Write-Host "Starting $service..." -ForegroundColor Green
    
    # Launch standard powershell process without modifying window titles
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "mvn spring-boot:run" -WorkingDirectory "D:\CryptoVault\backend\$service"
    
    # Wait for database connection pooling to start
    Start-Sleep -Seconds 2
}

Write-Host ""
Write-Host "=============================================" -ForegroundColor Green
Write-Host "All backend services successfully launched!  " -ForegroundColor Green
Write-Host "Check the new terminal windows for log output." -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Green
