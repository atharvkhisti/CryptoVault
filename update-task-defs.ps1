$ErrorActionPreference = "Stop"
$REGION = "ap-south-1"
$CLUSTER = "cryptovault-dev-cluster"
$DB_HOST = "cryptovault-dev-db.cd2myqieownu.ap-south-1.rds.amazonaws.com"
$DB_PASSWORD = "dummy_db_password"
$JWT_SECRET = "Y29uZmlndXJhYmxlc2VjcmV0a2V5Zm9yY3J5cHRvdmF1bHRhdXRoc2VydmljZWJhc2U2NA=="
$ECR = "469935552565.dkr.ecr.ap-south-1.amazonaws.com"

$services = @(
  @{ name="auth-service";         family="cryptovault-dev-auth-service";         port=8083; image="cryptovault-auth-service" },
  @{ name="wallet-service";       family="cryptovault-dev-wallet-service";       port=8081; image="cryptovault-wallet-service" },
  @{ name="transaction-service";  family="cryptovault-dev-transaction-service";  port=8082; image="cryptovault-transaction-service" },
  @{ name="notification-service"; family="cryptovault-dev-notification-service"; port=8084; image="cryptovault-notification-service" },
  @{ name="risk-service";         family="cryptovault-dev-risk-service";         port=8085; image="cryptovault-risk-service" },
  @{ name="audit-service";        family="cryptovault-dev-audit-service";        port=8086; image="cryptovault-audit-service" },
  @{ name="kyc-service";          family="cryptovault-dev-kyc-service";          port=8087; image="cryptovault-kyc-service" },
  @{ name="api-gateway";          family="cryptovault-dev-api-gateway";          port=8080; image="cryptovault-api-gateway" }
)

# Get IAM roles from existing task definition
$existingTd = aws ecs describe-task-definition --task-definition "cryptovault-dev-auth-service" --region $REGION | ConvertFrom-Json
$execRoleArn = $existingTd.taskDefinition.executionRoleArn
$taskRoleArn = $existingTd.taskDefinition.taskRoleArn

foreach ($svc in $services) {
  Write-Host "`nRegistering new task definition for: $($svc.name)" -ForegroundColor Cyan

  $containerDef = @{
    name      = $svc.name
    image     = "${ECR}/$($svc.image):latest"
    essential = $true
    portMappings = @(@{ containerPort = $svc.port; hostPort = $svc.port })
    environment = @(
      @{ name = "DB_HOST";     value = $DB_HOST },
      @{ name = "DB_PORT";     value = "5432" },
      @{ name = "DB_USER";     value = "postgres" },
      @{ name = "DB_PASSWORD"; value = $DB_PASSWORD },
      @{ name = "DB_NAME";     value = "cryptovault" },
      @{ name = "SPRING_PROFILES_ACTIVE"; value = "dev" },
      @{ name = "JWT_SECRET_KEY"; value = $JWT_SECRET }
    )
    logConfiguration = @{
      logDriver = "awslogs"
      options = @{
        "awslogs-group"         = "/ecs/$($svc.family)"
        "awslogs-region"        = $REGION
        "awslogs-stream-prefix" = "ecs"
      }
    }
  }

  $tmpFile = [System.IO.Path]::GetTempFileName() + ".json"
  @{
    family                  = $svc.family
    networkMode             = "awsvpc"
    requiresCompatibilities = @("FARGATE")
    cpu                     = "256"
    memory                  = "512"
    executionRoleArn        = $execRoleArn
    taskRoleArn             = $taskRoleArn
    containerDefinitions    = @($containerDef)
  } | ConvertTo-Json -Depth 10 | Set-Content $tmpFile

  $newTd = aws ecs register-task-definition --cli-input-json "file://$tmpFile" --region $REGION --query "taskDefinition.taskDefinitionArn" --output text 2>&1
  Remove-Item $tmpFile -Force
  Write-Host "  Registered: $newTd" -ForegroundColor Green

  # Update service to use new task definition
  $ecsService = "cryptovault-dev-$($svc.name)"
  Write-Host "  Updating service: $ecsService" -ForegroundColor Yellow
  aws ecs update-service --cluster $CLUSTER --service $ecsService --task-definition $newTd --region $REGION --query "service.{Status:status,Running:runningCount}" --output table 2>&1
}

Write-Host "`n========================================" -ForegroundColor Green
Write-Host "  All task definitions updated with DB_PASSWORD!" -ForegroundColor Green
Write-Host "  ECS will deploy new tasks shortly..." -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
