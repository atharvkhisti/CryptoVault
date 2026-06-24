variable "vpc_id" {
  type        = string
  description = "Target VPC ID"
}

variable "private_subnet_ids" {
  type        = list(string)
  description = "Private subnets for task execution"
}

variable "environment" {
  type        = string
  description = "Deployment environment name (dev/prod)"
}

variable "ecs_execution_role_arn" {
  type        = string
  description = "Execution role ARN for pulling images and logging"
}

variable "ecs_task_role_arn" {
  type        = string
  description = "Task role ARN for application execution"
}

variable "alb_security_group_id" {
  type        = string
  description = "ALB security group ID to allow ingress traffic"
}

variable "db_endpoint" {
  type        = string
  description = "RDS connection endpoint"
}

variable "db_password" {
  type        = string
  description = "RDS master password"
  sensitive   = true
}

# 1. ECS Security Group (Tasks run in private subnets, egress to anywhere, ingress only from ALB & self)
resource "aws_security_group" "ecs_sg" {
  name        = "cryptovault-${var.environment}-ecs-sg"
  description = "Security group for ECS tasks"
  vpc_id      = var.vpc_id

  ingress {
    description     = "Allow traffic from ALB"
    from_port       = 0
    to_port         = 0
    protocol        = "-1"
    security_groups = [var.alb_security_group_id]
  }

  ingress {
    description = "Allow internal traffic between ECS tasks"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    self        = true
  }

  egress {
    from_port        = 0
    to_port          = 0
    protocol         = "-1"
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }

  tags = {
    Name        = "cryptovault-${var.environment}-ecs-sg"
    Environment = var.environment
  }
}

# 2. ECS Cluster
resource "aws_ecs_cluster" "main" {
  name = "cryptovault-${var.environment}-cluster"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }
}

# 2.1 Private DNS Namespace for Service Discovery
resource "aws_service_discovery_private_dns_namespace" "main" {
  name        = "cryptovault.local"
  description = "Private DNS namespace for CryptoVault ECS services"
  vpc         = var.vpc_id
}

# 2.2 Service Discovery Services
resource "aws_service_discovery_service" "app" {
  for_each = var.services

  name = each.value.name

  dns_config {
    namespace_id = aws_service_discovery_private_dns_namespace.main.id

    dns_records {
      ttl  = 10
      type = "A"
    }

    routing_policy = "MULTIVALUE"
  }

  health_check_custom_config {
    failure_threshold = 1
  }
}

# Define microservices configuration structure
variable "services" {
  type = map(object({
    name             = string
    port             = number
    cpu              = number
    memory           = number
    image            = string
    target_group_arn = string
  }))
}

# 3. ECS Task Definitions (Looped)
resource "aws_ecs_task_definition" "app" {
  for_each = var.services

  family                   = "cryptovault-${var.environment}-${each.value.name}"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = each.value.cpu
  memory                   = each.value.memory
  execution_role_arn       = var.ecs_execution_role_arn
  task_role_arn            = var.ecs_task_role_arn

  container_definitions = jsonencode([
    {
      name      = each.value.name
      image     = each.value.image
      essential = true
      portMappings = [
        {
          containerPort = each.value.port
          hostPort      = each.value.port
        }
      ]
      environment = [
        { name = "DB_HOST",     value = split(":", var.db_endpoint)[0] },
        { name = "DB_PORT",     value = "5432" },
        { name = "DB_USER",     value = "postgres" },
        { name = "DB_PASSWORD", value = var.db_password },
        { name = "DB_NAME",     value = "cryptovault" },
        { name = "SPRING_PROFILES_ACTIVE", value = var.environment },
        { name = "JWT_SECRET_KEY", value = "Y29uZmlndXJhYmxlc2VjcmV0a2V5Zm9yY3J5cHRvdmF1bHRhdXRoc2VydmljZWJhc2U2NA==" },
        { name = "AUTH_SERVICE_HOST",         value = "auth-service.cryptovault.local" },
        { name = "WALLET_SERVICE_HOST",        value = "wallet-service.cryptovault.local" },
        { name = "TRANSACTION_SERVICE_HOST",   value = "transaction-service.cryptovault.local" },
        { name = "NOTIFICATION_SERVICE_HOST",  value = "notification-service.cryptovault.local" },
        { name = "RISK_SERVICE_HOST",          value = "risk-service.cryptovault.local" },
        { name = "AUDIT_SERVICE_HOST",         value = "audit-service.cryptovault.local" },
        { name = "KYC_SERVICE_HOST",           value = "kyc-service.cryptovault.local" }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = "/ecs/cryptovault-${var.environment}-${each.value.name}"
          "awslogs-region"        = "ap-south-1"
          "awslogs-stream-prefix" = "ecs"
        }
      }
    }
  ])
}

# 4. ECS Services (Looped)
resource "aws_ecs_service" "app" {
  for_each = var.services

  name            = "cryptovault-${var.environment}-${each.value.name}"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.app[each.key].arn
  desired_count   = var.environment == "prod" ? 2 : 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = var.private_subnet_ids
    security_groups  = [aws_security_group.ecs_sg.id]
    assign_public_ip = false
  }

  dynamic "load_balancer" {
    for_each = each.value.target_group_arn != null && each.value.target_group_arn != "" ? [1] : []
    content {
      target_group_arn = each.value.target_group_arn
      container_name   = each.value.name
      container_port   = each.value.port
    }
  }

  service_registries {
    registry_arn = aws_service_discovery_service.app[each.key].arn
  }

  # Allow target group healthcheck to settle before starting deployment validation
  health_check_grace_period_seconds = each.value.target_group_arn != null && each.value.target_group_arn != "" ? 300 : null

  depends_on = [aws_ecs_task_definition.app, aws_service_discovery_service.app]
}

# 5. Auto Scaling (Looped for Production environments)
resource "aws_appautoscaling_target" "ecs_target" {
  for_each = var.environment == "prod" ? var.services : {}

  max_capacity       = 5
  min_capacity       = 2
  resource_id        = "service/${aws_ecs_cluster.main.name}/${aws_ecs_service.app[each.key].name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}

resource "aws_appautoscaling_policy" "ecs_policy_cpu" {
  for_each = var.environment == "prod" ? var.services : {}

  name               = "cryptovault-${var.environment}-${each.value.name}-cpu-autoscaling"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.ecs_target[each.key].resource_id
  scalable_dimension = aws_appautoscaling_target.ecs_target[each.key].scalable_dimension
  service_namespace  = aws_appautoscaling_target.ecs_target[each.key].service_namespace

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }
    target_value       = 70.0
    scale_in_cooldown  = 60
    scale_out_cooldown = 60
  }
}

# Outputs
output "cluster_name" {
  value = aws_ecs_cluster.main.name
}

output "ecs_security_group_id" {
  value = aws_security_group.ecs_sg.id
}
