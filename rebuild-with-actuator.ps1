$ErrorActionPreference = "Stop"
$REGION = "ap-south-1"
$ECR = "469935552565.dkr.ecr.ap-south-1.amazonaws.com"
$CLUSTER = "cryptovault-dev-cluster"

# Services that need a rebuild (actuator added + application.properties updated)
$services = @("auth-service", "wallet-service", "transaction-service", "notification-service", "api-gateway")

Write-Host "`n=== Step 1: ECR Login ===" -ForegroundColor Cyan
$loginPwd = aws ecr get-login-password --region $REGION
$loginPwd | docker login --username AWS --password-stdin $ECR

Write-Host "`n=== Step 2: Build common-lib ===" -ForegroundColor Cyan
Push-Location backend/common-lib
cmd /c mvnw clean install -DskipTests -q
Pop-Location

Write-Host "`n=== Step 3: Build, Tag & Push Updated Services ===" -ForegroundColor Cyan
foreach ($svc in $services) {
  Write-Host "`n--- $svc ---" -ForegroundColor Yellow
  
  # Maven build
  Push-Location "backend/$svc"
  cmd /c mvnw clean package -DskipTests -q
  Pop-Location

  # Docker build & push
  $imgLatest = "$ECR/cryptovault-$svc`:latest"
  docker build -t $imgLatest -f "backend/$svc/Dockerfile" ./backend
  docker push $imgLatest
  Write-Host "  Pushed: $imgLatest" -ForegroundColor Green
}

Write-Host "`n=== Step 4: Force ECS redeployment for updated services ===" -ForegroundColor Cyan
foreach ($svc in $services) {
  $ecsSvc = "cryptovault-dev-$svc"
  Write-Host "  Updating: $ecsSvc" -ForegroundColor Yellow
  aws ecs update-service --cluster $CLUSTER --service $ecsSvc --force-new-deployment --region $REGION --query "service.{Status:status,Running:runningCount}" --output table 2>&1
}

Write-Host "`n========================================" -ForegroundColor Green
Write-Host "  Rebuild complete! New images with /actuator/health deployed." -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
