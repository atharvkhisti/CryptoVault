# CryptoVault AP-SOUTH-1 Automatic Deployment Script
# Run this script in PowerShell to automate the entire dev deployment.

$ErrorActionPreference = "Stop"

# Configurations
$AWS_ACCOUNT_ID = "469935552565"
$REGION = "ap-south-1"
$ECR_REGISTRY = "${AWS_ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com"
$SERVICES = @("api-gateway", "auth-service", "wallet-service", "transaction-service", "notification-service", "risk-service", "audit-service", "kyc-service")

Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "  Starting CryptoVault Cloud Deployment (ap-south-1)" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan

# 1. Verify AWS Identity
Write-Host "`n[1/6] Verifying AWS credentials..." -ForegroundColor Yellow
try {
    $identity = aws sts get-caller-identity | ConvertFrom-Json
    Write-Host "Authenticated as IAM User: $($identity.Arn)" -ForegroundColor Green
} catch {
    Write-Error "AWS Authentication failed. Please run 'aws configure' first."
}

# 2. Authenticate Docker with ECR
Write-Host "`n[2/6] Logging Docker into AWS ECR..." -ForegroundColor Yellow
aws ecr get-login-password --region $REGION | docker login --username AWS --password-stdin $ECR_REGISTRY

# 3. Create ECR Repositories if missing
Write-Host "`n[3/6] Ensuring ECR repositories exist..." -ForegroundColor Yellow
foreach ($service in $SERVICES) {
    try {
        aws ecr describe-repositories --repository-names "cryptovault-$service" --region $REGION > $null
        Write-Host "Repository 'cryptovault-$service' already exists." -ForegroundColor Gray
    } catch {
        Write-Host "Creating ECR repository: cryptovault-$service" -ForegroundColor Green
        aws ecr create-repository --repository-name "cryptovault-$service" --region $REGION | Out-Null
    }
}

# 4. Build and Push Backend Microservices
Write-Host "`n[4/6] Compiling backend and pushing Docker images..." -ForegroundColor Yellow

# Build shared library dependency
Write-Host "Building shared library: common-lib..." -ForegroundColor Cyan
cd backend/common-lib
mvn clean install -DskipTests
cd ../..

foreach ($service in $SERVICES) {
    Write-Host "Processing $service..." -ForegroundColor Cyan
    
    # Run maven packaging
    cd "backend/$service"
    mvn clean package -DskipTests
    cd ../..
    
    # Build Docker Image
    $imageTag = "${ECR_REGISTRY}/cryptovault-${service}:latest"
    Write-Host "Building image: $imageTag" -ForegroundColor Gray
    docker build -t $imageTag -f "backend/$service/Dockerfile" backend
    
    # Push Image to ECR
    Write-Host "Pushing image to registry..." -ForegroundColor Gray
    docker push $imageTag
}

# 5. Provision Infrastructure with Terraform
Write-Host "`n[5/6] Applying Terraform changes..." -ForegroundColor Yellow
$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
cd terraform/environments/dev
terraform init
terraform apply -auto-approve

# Get output variables
$s3_bucket = terraform output -raw s3_bucket_name
$cloudfront_url = terraform output -raw cloudfront_endpoint
$load_balancer_endpoint = terraform output -raw load_balancer_endpoint
cd ../../..

# 6. Build and Deploy React Frontend
Write-Host "`n[6/6] Packaging and deploying frontend web application..." -ForegroundColor Yellow
$env:VITE_API_BASE_URL = "https://$cloudfront_url"
cd frontend/web-app
npm install
npm run build
Write-Host "Uploading build assets to S3 bucket: $s3_bucket..." -ForegroundColor Cyan
aws s3 sync dist/ "s3://${s3_bucket}/" --delete
cd ../..

Write-Host "`n=============================================" -ForegroundColor Green
Write-Host "  CryptoVault Deployment Completed Successfully!" -ForegroundColor Green
Write-Host "  Front-end Access: https://$cloudfront_url" -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Green
