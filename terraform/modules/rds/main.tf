variable "vpc_id" {
  type        = string
  description = "Target VPC ID"
}

variable "private_subnet_ids" {
  type        = list(string)
  description = "Private subnets for database placement"
}

variable "environment" {
  type        = string
  description = "Deployment environment name (dev/prod)"
}

variable "db_instance_class" {
  type        = string
  default     = "db.t4g.micro"
  description = "RDS database instance size"
}

variable "db_allocated_storage" {
  type        = number
  default     = 20
  description = "Storage allocated in GB"
}

variable "multi_az" {
  type        = bool
  default     = false
  description = "Flag to enable Multi-AZ deployment"
}

variable "ecs_security_group_id" {
  type        = string
  description = "Security Group ID of the ECS microservices to grant DB access"
}

# 1. DB Subnet Group (associates RDS to private subnets)
resource "aws_db_subnet_group" "main" {
  name       = "cryptovault-${var.environment}-db-subnet-group"
  subnet_ids = var.private_subnet_ids

  tags = {
    Name        = "cryptovault-${var.environment}-db-subnet-group"
    Environment = var.environment
  }
}

# 2. Database Security Group
resource "aws_security_group" "db_sg" {
  name        = "cryptovault-${var.environment}-db-sg"
  description = "Restricts PostgreSQL traffic to ECS microservices"
  vpc_id      = var.vpc_id

  ingress {
    description     = "Allow port 5432 ingress from ECS microservices"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [var.ecs_security_group_id]
  }

  egress {
    from_port        = 0
    to_port          = 0
    protocol         = "-1"
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }

  tags = {
    Name        = "cryptovault-${var.environment}-db-sg"
    Environment = var.environment
  }
}

# 3. Database Parameter Group
resource "aws_db_parameter_group" "pg" {
  name   = "cryptovault-${var.environment}-db-pg"
  family = "postgres15"

  parameter {
    name  = "log_connections"
    value = "1"
  }

  parameter {
    name  = "log_disconnections"
    value = "1"
  }
}

# 4. PostgreSQL DB Instance
resource "aws_db_instance" "postgres" {
  identifier             = "cryptovault-${var.environment}-db"
  engine                 = "postgres"
  engine_version         = "15"
  instance_class         = var.db_instance_class
  allocated_storage      = var.db_allocated_storage
  max_allocated_storage  = 100
  storage_type           = "gp3"
  
  db_name                = "cryptovault"
  username               = "postgres"
  password               = "dummy_db_password" # In production, set as variable or injected via Secrets Manager
  
  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.db_sg.id]
  parameter_group_name   = aws_db_parameter_group.pg.name
  
  multi_az               = var.multi_az
  backup_retention_period = var.environment == "prod" ? 7 : 0
  backup_window          = "03:00-04:00"
  maintenance_window     = "Mon:04:00-Mon:05:00"
  
  skip_final_snapshot    = var.environment == "dev"
  final_snapshot_identifier = "cryptovault-${var.environment}-db-final-snapshot"
  
  tags = {
    Environment = var.environment
  }
}

# Outputs
output "db_endpoint" {
  value       = aws_db_instance.postgres.endpoint
  description = "Database connection endpoint host:port"
}

output "db_address" {
  value       = aws_db_instance.postgres.address
  description = "Database connection DNS address"
}

output "db_security_group_id" {
  value = aws_security_group.db_sg.id
}
