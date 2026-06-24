variable "vpc_id" {
  type        = string
  description = "Target VPC ID"
}

variable "public_subnet_ids" {
  type        = list(string)
  description = "Public subnets for load balancer placement"
}

variable "environment" {
  type        = string
  description = "Deployment environment name (dev/prod)"
}

# 1. ALB Security Group
resource "aws_security_group" "alb_sg" {
  name        = "cryptovault-${var.environment}-alb-sg"
  description = "Controls HTTP/HTTPS ingress to the load balancer"
  vpc_id      = var.vpc_id

  ingress {
    description      = "Allow HTTP from internet"
    from_port        = 80
    to_port          = 80
    protocol         = "tcp"
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }

  ingress {
    description      = "Allow HTTPS from internet"
    from_port        = 443
    to_port          = 443
    protocol         = "tcp"
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }

  egress {
    from_port        = 0
    to_port          = 0
    protocol         = "-1"
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }

  tags = {
    Name        = "cryptovault-${var.environment}-alb-sg"
    Environment = var.environment
  }
}

# 2. Application Load Balancer
resource "aws_lb" "main" {
  name               = "cryptovault-${var.environment}-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb_sg.id]
  subnets            = var.public_subnet_ids

  tags = {
    Environment = var.environment
  }
}

# 3. Target Groups for Services
resource "aws_lb_target_group" "api_gateway" {
  name        = "cryptovault-${var.environment}-tg-gateway"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "ip"

  health_check {
    path                = "/actuator/health"
    matcher             = "200"
    interval            = 30
    timeout             = 10
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }
}

resource "aws_lb_target_group" "services" {
  for_each = {
    auth        = { port = 8083, path = "/api/auth/v3/api-docs" }
    wallet      = { port = 8081, path = "/api/wallets/v3/api-docs" }
    transaction = { port = 8082, path = "/api/transactions/v3/api-docs" }
    notification= { port = 8084, path = "/api/notifications/v3/api-docs" }
    risk        = { port = 8085, path = "/api/risk/v3/api-docs" }
    audit       = { port = 8086, path = "/api/audit/v3/api-docs" }
    kyc         = { port = 8087, path = "/api/kyc/v3/api-docs" }
  }

  name        = "cryptovault-${var.environment}-tg-${each.key}"
  port        = each.value.port
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "ip"

  health_check {
    path                = "/actuator/health"
    matcher             = "200"
    interval            = 30
    timeout             = 10
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }
}

# 4. ALB Listeners
resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.main.arn
  port              = "80"
  protocol          = "HTTP"

  # Default action redirects to API Gateway target group (or HTTPS in production)
  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.api_gateway.arn
  }
}
# Outputs
output "alb_dns_name" {
  value = aws_lb.main.dns_name
}

output "alb_security_group_id" {
  value = aws_security_group.alb_sg.id
}

output "target_group_gateway_arn" {
  value = aws_lb_target_group.api_gateway.arn
}

output "target_group_arns" {
  value = { for k, v in aws_lb_target_group.services : k => v.arn }
}
