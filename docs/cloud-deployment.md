# Cloud Deployment & Rollback Runbook — CryptoVault

This runbook outlines the deployment lifecycle, CI/CD automation, and rollback strategies for the CryptoVault containerized microservices and static frontend.

---

## 1. Automated Deployment Architecture

Deployments are executed in three primary phases: Build, Provision, and Deploy.

```
[Developer Push]
       │
       ▼
┌──────────────┐      Builds container images
│  Jenkins CI  │ ───► Pushes to AWS ECR Registry
└──────┬───────┘
       │
       ▼ Triggers Terraform changes
┌──────────────┐      Provisions DB, ALB, ECS Tasks
│  Terraform   │ ───► Configures Auto Scaling Groups
└──────┬───────┘
       │
       ▼ Triggers rolling ECS update
┌──────────────┐      Rotates Fargate Tasks (Zero-downtime)
│ ECS Fargate  │ ───► Verifies Health Checks
└──────────────┘
```

---

## 2. CI/CD Pipeline Integration (Jenkins)

The pipeline uses Jenkins to automate building images, applying infrastructure modifications, and updating tasks.

### Jenkins Pipeline Stages
1. **Source Code Checkout**: Pulls repository branches from GitHub on commits.
2. **Maven Compile & Test**: Compiles Java code, runs unit tests, generates JaCoCo test coverage reports.
3. **SonarQube Analysis**: Runs static code analysis and checks quality gate criteria.
4. **Build & Package**: Builds final Spring Boot executable `.jar` files and packages them into Docker images using individual microservice `Dockerfile`s.
5. **ECR Authentication & Image Push**:
   * Logs into AWS Elastic Container Registry (ECR).
   * Tags images with the Git commit SHA (e.g., `469935552565.dkr.ecr.ap-south-1.amazonaws.com/cryptovault-auth-service:git-abc1234`).
   * Pushes images to their respective ECR repositories.
6. **Terraform Apply**:
   * Initialises Terraform with the environment backend.
   * Feeds the newly built ECR image tag into the environment variables.
   * Runs `terraform apply -auto-approve` to update target definitions.
7. **ECS Dynamic Update**:
   * AWS ECS registers the new task definition version.
   * Initiates rolling updates using the blue-green style Fargate task placement.

---

## 3. Jenkinsfile Blueprint Snippet

```groovy
pipeline {
    agent any
    environment {
        AWS_DEFAULT_REGION = 'ap-south-1'
        AWS_CREDENTIALS_ID = 'aws-credentials-id'
        ECR_REGISTRY       = '469935552565.dkr.ecr.ap-south-1.amazonaws.com'
        IMAGE_TAG          = "git-${env.GIT_COMMIT[0..7]}"
    }
    stages {
        stage('Checkout') {
            steps { checkout scm }
        }
        stage('Maven Test & Build') {
            steps {
                sh 'mvn clean verify jacoco:report'
            }
        }
        stage('Docker Build & Push') {
            steps {
                withCredentials([[credentialsId: env.AWS_CREDENTIALS_ID, type: 'AmazonWebServicesCredentials']]) {
                    sh 'aws ecr get-login-password --region $AWS_DEFAULT_REGION | docker login --username AWS --password-stdin $ECR_REGISTRY'
                    
                    // Example auth-service build & push
                    sh "docker build -t $ECR_REGISTRY/cryptovault-auth-service:$IMAGE_TAG ./backend/auth-service"
                    sh "docker push $ECR_REGISTRY/cryptovault-auth-service:$IMAGE_TAG"
                }
            }
        }
        stage('Terraform Deploy') {
            steps {
                dir('terraform/environments/prod') {
                    withCredentials([[credentialsId: env.AWS_CREDENTIALS_ID, type: 'AmazonWebServicesCredentials']]) {
                        sh 'terraform init'
                        sh "terraform apply -var='image_tag=${IMAGE_TAG}' -auto-approve"
                    }
                }
            }
        }
    }
}
```

---

## 4. Zero-Downtime Rolling Updates

AWS ECS performs zero-downtime rolling updates by controlling the task replacement thresholds during a deployment:

* **Minimum Healthy Percent**: Configured to `100%`. ECS will keep all existing tasks running while starting new containers.
* **Maximum Percent**: Configured to `200%`. ECS will double the task instances to start the new containers, then shut down the old versions once health checks pass.
* **Connection Draining**: The ALB is configured to drain connections from deregistered targets for 30 seconds to avoid interrupting active transactions.

---

## 5. Rollback Strategy & Incident Response

If a defect escapes pre-production validation and gets deployed to production, DevOps engineers can choose between three rollback models depending on the severity:

### Model A: Revert Commit (Recommended)
This model maintains a clean Git history and ensures the code remains synchronized with the infrastructure.
1. Revert the commit on the main branch:
   ```bash
   git revert HEAD
   git push origin main
   ```
2. The CI/CD pipeline triggers automatically, packages the previous code version, pushes a new image tag, and applies it via Terraform.

### Model B: Manual Image Rollback (Fastest Hotfix)
If the CI/CD pipeline is broken, force ECS to run a previous known stable image using the AWS CLI:
1. Update the ECS service to run the previous stable task definition:
   ```bash
   aws ecs update-service \
     --cluster cryptovault-prod-cluster \
     --service cryptovault-prod-auth-service \
     --task-definition cryptovault-prod-auth-service:3
   ```
   *(Assuming version 3 was the last stable definition)*
2. ECS will start new containers using the old stable version and drain connections from the broken containers.

### Model C: Infrastructure State Rollback
If the problem is caused by a bad Terraform configuration modification rather than code (e.g. database routing error):
1. Locate the commit SHA of the last stable Terraform release.
2. Revert the files to that commit:
   ```bash
   git checkout <stable-commit-sha> -- terraform/
   ```
3. Execute local/manual deployment plan validation:
   ```bash
   cd terraform/environments/prod
   terraform plan -out=rollback.plan
   terraform apply rollback.plan
   ```
4. Verify the database connectivity, route tables, and firewalls are restored.

---

## 6. Local Development Sandbox Deployment Automation

For rapid provisioning of the `dev` environment from your local workstation, run the automated deployment script located at the root of the workspace:

```powershell
.\deploy-dev.ps1
```

This script automates the following actions:
1. **Verification**: Verifies that your local AWS CLI is authenticated.
2. **ECR Login**: Authenticates Docker with your ECR registry in `ap-south-1`.
3. **Repository Bootstrapping**: Automatically creates missing ECR repositories (`cryptovault-api-gateway`, `cryptovault-auth-service`, etc.).
4. **Compile & Push**: Compiles the Spring Boot services, builds container layers, and pushes them to your ECR registry.
5. **Orchestrator Application**: Initializes and applies the Terraform configuration to provision your VPC, database, ALB, and ECS Fargate services.
6. **Frontend Assets Sync**: Bundles the React static app and synchronizes it directly with S3.

