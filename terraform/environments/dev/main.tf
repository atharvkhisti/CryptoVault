terraform {
  required_version = ">= 1.5.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # For local state, or configured with an S3 backend in production
  backend "local" {
    path = "terraform.tfstate"
  }
}

provider "aws" {
  region = "ap-south-1"
}

module "vpc" {
  source      = "../../modules/vpc"
  environment = "dev"
}

module "iam" {
  source      = "../../modules/iam"
  environment = "dev"
}

module "alb" {
  source            = "../../modules/alb"
  vpc_id            = module.vpc.vpc_id
  public_subnet_ids = module.vpc.public_subnets
  environment       = "dev"
}

module "rds" {
  source                = "../../modules/rds"
  vpc_id                = module.vpc.vpc_id
  private_subnet_ids    = module.vpc.private_subnets
  environment           = "dev"
  db_instance_class     = "db.t4g.micro"
  db_allocated_storage  = 20
  multi_az              = false
  ecs_security_group_id = module.ecs.ecs_security_group_id
}

module "ecs" {
  source                 = "../../modules/ecs"
  vpc_id                 = module.vpc.vpc_id
  private_subnet_ids     = module.vpc.private_subnets
  environment            = "dev"
  ecs_execution_role_arn = module.iam.ecs_execution_role_arn
  ecs_task_role_arn      = module.iam.ecs_task_role_arn
  alb_security_group_id  = module.alb.alb_security_group_id
  db_endpoint            = module.rds.db_endpoint
  db_password            = "dummy_db_password"

  # Dev services mapping (single task configuration, low cpu/ram, latest ECR placeholders)
  services = {
    api-gateway = {
      name             = "api-gateway"
      port             = 8080
      cpu              = 256
      memory           = 512
      image            = "469935552565.dkr.ecr.ap-south-1.amazonaws.com/cryptovault-api-gateway:latest"
      target_group_arn = module.alb.target_group_gateway_arn
    }
    auth = {
      name             = "auth-service"
      port             = 8083
      cpu              = 256
      memory           = 512
      image            = "469935552565.dkr.ecr.ap-south-1.amazonaws.com/cryptovault-auth-service:latest"
      target_group_arn = ""
    }
    wallet = {
      name             = "wallet-service"
      port             = 8081
      cpu              = 256
      memory           = 512
      image            = "469935552565.dkr.ecr.ap-south-1.amazonaws.com/cryptovault-wallet-service:latest"
      target_group_arn = ""
    }
    transaction = {
      name             = "transaction-service"
      port             = 8082
      cpu              = 256
      memory           = 512
      image            = "469935552565.dkr.ecr.ap-south-1.amazonaws.com/cryptovault-transaction-service:latest"
      target_group_arn = ""
    }
    notification = {
      name             = "notification-service"
      port             = 8084
      cpu              = 256
      memory           = 512
      image            = "469935552565.dkr.ecr.ap-south-1.amazonaws.com/cryptovault-notification-service:latest"
      target_group_arn = ""
    }
    risk = {
      name             = "risk-service"
      port             = 8085
      cpu              = 256
      memory           = 512
      image            = "469935552565.dkr.ecr.ap-south-1.amazonaws.com/cryptovault-risk-service:latest"
      target_group_arn = ""
    }
    audit = {
      name             = "audit-service"
      port             = 8086
      cpu              = 256
      memory           = 512
      image            = "469935552565.dkr.ecr.ap-south-1.amazonaws.com/cryptovault-audit-service:latest"
      target_group_arn = ""
    }
    kyc = {
      name             = "kyc-service"
      port             = 8087
      cpu              = 256
      memory           = 512
      image            = "469935552565.dkr.ecr.ap-south-1.amazonaws.com/cryptovault-kyc-service:latest"
      target_group_arn = ""
    }
  }
}

module "s3" {
  source       = "../../modules/s3"
  environment  = "dev"
  alb_dns_name = module.alb.alb_dns_name
}

module "cloudwatch" {
  source      = "../../modules/cloudwatch"
  environment = "dev"
}

# Outputs for local orchestration
output "cloudfront_endpoint" {
  value       = module.s3.cloudfront_domain_name
  description = "CloudFront web entrypoint"
}

output "load_balancer_endpoint" {
  value       = module.alb.alb_dns_name
  description = "Load balancer DNS endpoint"
}

output "s3_bucket_name" {
  value       = module.s3.s3_bucket_name
  description = "S3 bucket hosting frontend static assets"
}
