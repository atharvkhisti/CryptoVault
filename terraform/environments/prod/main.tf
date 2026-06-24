terraform {
  required_version = ">= 1.5.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # Production environment remote state storage (S3 backend with DynamoDB locking)
  backend "s3" {
    bucket         = "cryptovault-prod-terraform-state"
    key            = "global/s3/terraform.tfstate"
    region         = "ap-south-1"
    dynamodb_table = "cryptovault-prod-tflocks"
    encrypt        = true
  }
}

provider "aws" {
  region = "ap-south-1"
}

module "vpc" {
  source      = "../../modules/vpc"
  environment = "prod"
}

module "iam" {
  source      = "../../modules/iam"
  environment = "prod"
}

module "alb" {
  source            = "../../modules/alb"
  vpc_id            = module.vpc.vpc_id
  public_subnet_ids = module.vpc.public_subnets
  environment       = "prod"
}

module "rds" {
  source                = "../../modules/rds"
  vpc_id                = module.vpc.vpc_id
  private_subnet_ids    = module.vpc.private_subnets
  environment           = "prod"
  db_instance_class     = "db.m6g.large"
  db_allocated_storage  = 100
  multi_az              = true
  ecs_security_group_id = module.ecs.ecs_security_group_id
}

module "ecs" {
  source                 = "../../modules/ecs"
  vpc_id                 = module.vpc.vpc_id
  private_subnet_ids     = module.vpc.private_subnets
  environment            = "prod"
  ecs_execution_role_arn = module.iam.ecs_execution_role_arn
  ecs_task_role_arn      = module.iam.ecs_task_role_arn
  alb_security_group_id  = module.alb.alb_security_group_id
  db_endpoint            = module.rds.db_endpoint
  db_password            = "dummy_db_password"

  services = {
    api-gateway = {
      name             = "api-gateway"
      port             = 8080
      cpu              = 512
      memory           = 1024
      image            = "469935552565.dkr.ecr.ap-south-1.amazonaws.com/cryptovault-api-gateway:latest"
      target_group_arn = module.alb.target_group_gateway_arn
    }
    auth = {
      name             = "auth-service"
      port             = 8083
      cpu              = 512
      memory           = 1024
      image            = "469935552565.dkr.ecr.ap-south-1.amazonaws.com/cryptovault-auth-service:latest"
      target_group_arn = ""
    }
    wallet = {
      name             = "wallet-service"
      port             = 8081
      cpu              = 512
      memory           = 1024
      image            = "469935552565.dkr.ecr.ap-south-1.amazonaws.com/cryptovault-wallet-service:latest"
      target_group_arn = ""
    }
    transaction = {
      name             = "transaction-service"
      port             = 8082
      cpu              = 512
      memory           = 1024
      image            = "469935552565.dkr.ecr.ap-south-1.amazonaws.com/cryptovault-transaction-service:latest"
      target_group_arn = ""
    }
    notification = {
      name             = "notification-service"
      port             = 8084
      cpu              = 512
      memory           = 1024
      image            = "469935552565.dkr.ecr.ap-south-1.amazonaws.com/cryptovault-notification-service:latest"
      target_group_arn = ""
    }
    risk = {
      name             = "risk-service"
      port             = 8085
      cpu              = 512
      memory           = 1024
      image            = "469935552565.dkr.ecr.ap-south-1.amazonaws.com/cryptovault-risk-service:latest"
      target_group_arn = ""
    }
    audit = {
      name             = "audit-service"
      port             = 8086
      cpu              = 512
      memory           = 1024
      image            = "469935552565.dkr.ecr.ap-south-1.amazonaws.com/cryptovault-audit-service:latest"
      target_group_arn = ""
    }
    kyc = {
      name             = "kyc-service"
      port             = 8087
      cpu              = 512
      memory           = 1024
      image            = "469935552565.dkr.ecr.ap-south-1.amazonaws.com/cryptovault-kyc-service:latest"
      target_group_arn = ""
    }
  }
}

module "s3" {
  source       = "../../modules/s3"
  environment  = "prod"
  alb_dns_name = module.alb.alb_dns_name
}

module "cloudwatch" {
  source      = "../../modules/cloudwatch"
  environment = "prod"
}

# Outputs for prod orchestration
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
