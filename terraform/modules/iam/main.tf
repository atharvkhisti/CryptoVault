variable "environment" {
  type        = string
  description = "Deployment environment name (dev/prod)"
}

# 1. ECS Task Execution Role (Required by ECS agent to pull image & push logs)
resource "aws_iam_role" "ecs_execution_role" {
  name = "cryptovault-${var.environment}-ecs-execution-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action    = "sts:AssumeRole"
        Effect    = "Allow"
        Principal = { Service = "ecs-tasks.amazonaws.com" }
      }
    ]
  })

  tags = {
    Environment = var.environment
  }
}

# Attach standard AWS policy for ECS task execution (ECR, CloudWatch logs)
resource "aws_iam_role_policy_attachment" "ecs_execution" {
  role       = aws_iam_role.ecs_execution_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# Add policy for Secrets Manager access (so ECS agent can inject secrets into tasks)
resource "aws_iam_policy" "ecs_secrets_access" {
  name        = "cryptovault-${var.environment}-ecs-secrets-policy"
  description = "Allows ECS agent to retrieve database and JWT secrets from Secrets Manager"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = [
          "secretsmanager:GetSecretValue",
          "ssm:GetParameters"
        ]
        Resource = "*"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "ecs_secrets" {
  role       = aws_iam_role.ecs_execution_role.name
  policy_arn = aws_iam_policy.ecs_secrets_access.arn
}

# 2. ECS Task Role (Runtime permissions for applications running inside containers)
resource "aws_iam_role" "ecs_task_role" {
  name = "cryptovault-${var.environment}-ecs-task-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action    = "sts:AssumeRole"
        Effect    = "Allow"
        Principal = { Service = "ecs-tasks.amazonaws.com" }
      }
    ]
  })

  tags = {
    Environment = var.environment
  }
}

# 3. Deployment Role for Terraform
resource "aws_iam_role" "terraform_deployment_role" {
  name = "cryptovault-${var.environment}-terraform-deploy-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action    = "sts:AssumeRole"
        Effect    = "Allow"
        Principal = { Service = "ec2.amazonaws.com" } # Or OIDC provider for GitHub Actions / Jenkins
      }
    ]
  })

  tags = {
    Environment = var.environment
  }
}

# Outputs
output "ecs_execution_role_arn" {
  value = aws_iam_role.ecs_execution_role.arn
}

output "ecs_task_role_arn" {
  value = aws_iam_role.ecs_task_role.arn
}

output "terraform_deployment_role_arn" {
  value = aws_iam_role.terraform_deployment_role.arn
}
