variable "region" {
  description = "AWS region"
  type        = string
  default     = "us-east-2"
}

variable "environment" {
  description = "Environment name"
  type        = string
}

variable "project_name" {
  description = "Project name"
  type        = string
  default     = "challengeone"
}

variable "service_name" {
  description = "Service name"
  type        = string
  default     = "billing"
}

# Application
variable "app_replicas" {
  description = "Number of pod replicas"
  type        = number
  default     = 2
}

variable "app_image" {
  description = "Docker image for billing service"
  type        = string
}

variable "app_port" {
  description = "Application port"
  type        = number
  default     = 8080
}

# Resources
variable "cpu_request" {
  description = "CPU request"
  type        = string
  default     = "250m"
}

variable "cpu_limit" {
  description = "CPU limit"
  type        = string
  default     = "500m"
}

variable "memory_request" {
  description = "Memory request"
  type        = string
  default     = "512Mi"
}

variable "memory_limit" {
  description = "Memory limit"
  type        = string
  default     = "1Gi"
}

# AWS Credentials (for DynamoDB and SQS)
variable "aws_access_key" {
  description = "AWS Access Key for SQS"
  type        = string
  sensitive   = true
}

variable "aws_secret_key" {
  description = "AWS Secret Key for SQS"
  type        = string
  sensitive   = true
}

# Mercado Pago
variable "mercadopago_access_token" {
  description = "Mercado Pago Access Token"
  type        = string
  sensitive   = true
}

variable "mercadopago_public_key" {
  description = "Mercado Pago Public Key"
  type        = string
  default     = "APP_USR-test-public-key"
}

# SQS Queue
variable "sqs_queue_url" {
  description = "SQS Queue URL for payment processing"
  type        = string
  default     = ""
}

# Remote States
variable "eks_state_key" {
  description = "EKS Terraform state key"
  type        = string
}

variable "dynamodb_state_key" {
  description = "DynamoDB Terraform state key"
  type        = string
}

# HPA
variable "hpa_min_replicas" {
  description = "HPA minimum replicas"
  type        = number
  default     = 2
}

variable "hpa_max_replicas" {
  description = "HPA maximum replicas"
  type        = number
  default     = 4
}

variable "hpa_cpu_threshold" {
  description = "HPA CPU threshold percentage"
  type        = number
  default     = 70
}

variable "hpa_cpu_target" {
  description = "HPA CPU target percentage (alias)"
  type        = number
  default     = 70
}

# DATADOG
variable "datadog_agent_host" {
  description = "Hostname do Datadog Agent no cluster"
  type        = string
  default     = "datadog-agent.default.svc.cluster.local"
}

variable "app_version" {
  description = "Versão da aplicação (DD_VERSION e labels Datadog)"
  type        = string
  default     = "1.0.0"
}
