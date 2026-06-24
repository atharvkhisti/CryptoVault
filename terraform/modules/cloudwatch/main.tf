variable "environment" {
  type        = string
  description = "Deployment environment name (dev/prod)"
}

variable "service_names" {
  type        = list(string)
  default     = ["api-gateway", "auth-service", "wallet-service", "transaction-service", "notification-service", "risk-service", "audit-service", "kyc-service"]
  description = "List of microservice names to create log groups for"
}

# 1. Log Groups for all Microservices
resource "aws_cloudwatch_log_group" "ecs" {
  count             = length(var.service_names)
  name              = "/ecs/cryptovault-${var.environment}-${var.service_names[count.index]}"
  retention_in_days = var.environment == "prod" ? 30 : 7

  tags = {
    Environment = var.environment
  }
}

# 2. CloudWatch Dashboard for CPU, Memory, and ALB metrics
resource "aws_cloudwatch_dashboard" "main" {
  dashboard_name = "cryptovault-${var.environment}-dashboard"

  dashboard_body = jsonencode({
    widgets = [
      {
        type   = "metric"
        x      = 0
        y      = 0
        width  = 12
        height = 6
        properties = {
          metrics = [
            for s in var.service_names : [
              "AWS/ECS", "CPUUtilization", "ServiceName", "cryptovault-${var.environment}-${s}", "ClusterName", "cryptovault-${var.environment}-cluster"
            ]
          ]
          period = 300
          stat   = "Average"
          region = "ap-south-1"
          title  = "Average CPU Utilization per Service (%)"
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 0
        width  = 12
        height = 6
        properties = {
          metrics = [
            for s in var.service_names : [
              "AWS/ECS", "MemoryUtilization", "ServiceName", "cryptovault-${var.environment}-${s}", "ClusterName", "cryptovault-${var.environment}-cluster"
            ]
          ]
          period = 300
          stat   = "Average"
          region = "ap-south-1"
          title  = "Average Memory Utilization per Service (%)"
        }
      },
      {
        type   = "metric"
        x      = 0
        y      = 6
        width  = 24
        height = 6
        properties = {
          metrics = [
            ["AWS/ApplicationELB", "RequestCount", "LoadBalancer", "app/cryptovault-${var.environment}-alb/*"],
            ["AWS/ApplicationELB", "HTTPCode_Target_5XX_Count", "LoadBalancer", "app/cryptovault-${var.environment}-alb/*", { "color" = "#d62728", "stat" = "Sum" }],
            ["AWS/ApplicationELB", "TargetResponseTime", "LoadBalancer", "app/cryptovault-${var.environment}-alb/*", { "stat" = "Average" }]
          ]
          period = 300
          region = "ap-south-1"
          title  = "Application Load Balancer (Request count, HTTP 5XX Errors, Avg Response Time)"
        }
      }
    ]
  })
}
