# Terraform Infrastructure as Code (IaC) Guide — CryptoVault

This guide describes how to manage, validate, and deploy the CryptoVault cloud infrastructure using Terraform.

---

## 1. Modular Directory Structure

The Terraform configurations are structured to separate reusable core infrastructure templates (modules) from environment-specific configurations (environments).

```
terraform/
├── modules/
│   ├── vpc/          # Custom virtual private network topology (Subnets, IGW, NAT, RTs)
│   ├── iam/          # AWS Identity & Access Management Roles and Policy bindings
│   ├── rds/          # Relational Database Service (PostgreSQL database instance & parameter groups)
│   ├── alb/          # Application Load Balancer & path routing rules for microservices
│   ├── ecs/          # Elastic Container Service Fargate configs & target autoscaling
│   ├── s3/           # S3 bucket for React static files & CloudFront CDN distribution
│   └── cloudwatch/   # Logging groups per microservice & unified metrics dashboard
└── environments/
    ├── dev/          # Local state sandbox (Minimal cost footprint, single instance tasks)
    └── prod/         # Remote S3 state backend (DynamoDB state locking, high availability)
```

---

## 2. Environment Configurations

CryptoVault implements environment isolation to control costs and support CI/CD staging pipelines.

### Staging (Dev) vs Production (Prod) Configurations

| Architectural Feature | Dev Environment | Prod Environment |
| :--- | :--- | :--- |
| **State Storage** | `local` file (`terraform.tfstate`) | Remote S3 (`cryptovault-prod-terraform-state`) |
| **State Locking** | None (Single developer workflow) | DynamoDB Table (`cryptovault-prod-tflocks`) |
| **RDS DB Instance** | `db.t4g.micro` (Burst-capable, Arm64) | `db.m6g.large` (Production-grade memory/CPU) |
| **RDS Availability** | Single Availability Zone (Low cost) | Multi-AZ (Active-Standby replication, automated failover) |
| **ECS Task Desired Tasks** | `1` task per microservice | `2` tasks minimum (Multi-AZ load distribution) |
| **Auto Scaling** | Disabled | Target Tracking CPU Autoscaling Enabled (2 to 5 tasks) |
| **Log Retention** | 7 Days (Minimizes CloudWatch storage) | 30 Days (Compliant with audit standards) |

---

## 3. Terraform Backend & State Management

### Dev State Backend
The developer sandbox uses a local state backend (`terraform.tfstate`) for rapid testing:
```hcl
terraform {
  backend "local" {
    path = "terraform.tfstate"
  }
}
```

### Prod State Backend
Production uses a secure, distributed backend to support team-based deployments and protect against concurrency conflicts:
```hcl
terraform {
  backend "s3" {
    bucket         = "cryptovault-prod-terraform-state"
    key            = "global/s3/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "cryptovault-prod-tflocks"
    encrypt        = true
  }
}
```
* **Encryption**: Enforced at rest using standard AWS managed keys (`encrypt = true`).
* **Concurrency Locking**: Managed via **DynamoDB**. Any write operations automatically obtain a lock to prevent concurrent modifications from corrupting the state file.

---

## 4. Operational Workflow

To run commands, change directories directly into the target environment folder:

```bash
cd terraform/environments/dev
# OR
cd terraform/environments/prod
```

### Step 1: Initialize Terraform
Downloads the required provider plugins (AWS provider `~> 5.0`) and configures the backend state.
```bash
terraform init
```

### Step 2: Validate Configurations
Runs syntax checking and static validation on all module inputs and resources.
```bash
terraform validate
```

### Step 3: Plan Changes
Generates an execution plan displaying actions (create, update, destroy) that Terraform will perform without applying changes.
```bash
terraform plan -out=tfplan.binary
```

### Step 4: Apply Changes
Applies the planned changes.
```bash
terraform apply tfplan.binary
```

### Step 5: Tearing Down (Development Sandbox Only)
Destroys all provisioned resources to stop billing.
```bash
terraform destroy
```

---

## 5. Staging-to-Production Promotion Path

To migrate changes safely from the development sandbox to production:
1. **Develop Modules**: Modify modular files inside `terraform/modules/`.
2. **Local Sandbox Test**: Run `terraform plan` and `terraform apply` inside `terraform/environments/dev`. Validate deployment endpoints.
3. **Commit & Pull Request**: Commit changes to Git. Submit a Pull Request.
4. **CI Validation**: Jenkins executes `terraform validate` and `terraform plan` on the proposed changes.
5. **Approval**: Obtain peer review approval.
6. **CD Execution**: Merge to the `main` branch. Jenkins executes `terraform apply` in `environments/prod` using credentials assigned to `cryptovault-prod-terraform-deploy-role`.
