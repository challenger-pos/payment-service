/**
 * DynamoDB Table for Billing Service Payments
 * 
 * This resource creates the main DynamoDB table for storing payment records.
 * 
 * Design:
 * - Partition Key: workOrderId (UUID) - Ensures uniqueness and even distribution
 * - Sort Key: createdAt (Number - epoch milliseconds) - Enables temporal ordering and range queries
 * - Billing Mode: PAY_PER_REQUEST (On-Demand) - Scales automatically based on usage
 * - Global Secondary Indexes: Support queries by clientId, status, and externalPaymentId
 * 
 * Data Flow:
 * 1. Single payment lookup: Query by workOrderId (partition key) - O(1)
 * 2. Payments by client: Query GSI1 (clientId + createdAt) - O(log n)
 * 3. Payments by status: Query GSI2 (status + createdAt) - O(log n)
 * 4. External payment lookup: Query GSI3 (externalPaymentId) - O(1)
 * 
 * Point-in-Time Recovery: Disabled (as per requirements)
 * TTL: Not configured
 * Encryption: AWS-managed keys (SSE_AWS)
 * Stream: Not enabled
 */
resource "aws_dynamodb_table" "payments" {
  name           = "${var.service_name}-${var.environment}"
  billing_mode   = "PAY_PER_REQUEST" # On-Demand: scales automatically
  hash_key       = "workOrderId"
  range_key      = "createdAt"
  stream_enabled = false

  # Partition Key: workOrderId (UUID string)
  attribute {
    name = "workOrderId"
    type = "S" # String
  }

  # Sort Key: createdAt (epoch milliseconds for efficient sorting)
  attribute {
    name = "createdAt"
    type = "N" # Number
  }

  # GSI1: For querying payments by client ID
  attribute {
    name = "clientId"
    type = "S" # String
  }

  # GSI2: For querying payments by status
  attribute {
    name = "status"
    type = "S" # String
  }

  # GSI3: For querying payments by external payment ID (Mercado Pago)
  attribute {
    name = "externalPaymentId"
    type = "S" # String
  }

  # Global Secondary Index 1: Query by Client ID
  # Use case: Find all payments for a specific client, ordered by creation time
  global_secondary_index {
    name            = "clientId-createdAt-index"
    hash_key        = "clientId"
    range_key       = "createdAt"
    projection_type = "ALL" # Return all attributes (not just keys) to avoid additional queries

    # Projection specifies which attributes are returned in query results
    # ALL: includes all attributes (useful for this service)
    # OTHER/KEYS_ONLY: more storage efficient but requires additional queries
  }

  # Global Secondary Index 2: Query by Status
  # Use case: Find all payments with a specific status (e.g., all PENDING), ordered by creation time
  global_secondary_index {
    name            = "status-createdAt-index"
    hash_key        = "status"
    range_key       = "createdAt"
    projection_type = "ALL"
  }

  # Global Secondary Index 3: Query by External Payment ID
  # Use case: Lookup payment by Mercado Pago payment ID (unique lookup)
  # Note: This is effectively a sparse index (not all items have externalPaymentId)
  global_secondary_index {
    name            = "externalPaymentId-index"
    hash_key        = "externalPaymentId"
    projection_type = "ALL"
  }

  # Server-Side Encryption
  server_side_encryption {
    enabled     = true
    kms_key_arn = null # Uses AWS-managed keys (no need to manage separate KMS keys)
  }

  # Point-in-Time Recovery: Disabled (as per requirements)
  # WARNING: Without PITR, accidental deletion is not recoverable
  # RECOMMENDATION: Consider enabling PITR in production environments
  point_in_time_recovery {
    enabled = false
  }

  # TTL: Not configured
  # Future enhancement: Could add TTL for archiving old payments

  # Tags for organization and cost tracking
  tags = {
    Name        = "billing-payments-table"
    Environment = var.environment
    Service     = var.service_name
    Project     = var.project_name
    ManagedBy   = "Terraform"
    CreatedBy   = "Infrastructure as Code"
  }

  depends_on = [aws_kms_key.dynamodb_key]
}

# KMS Key for DynamoDB Encryption (optional, using AWS-managed keys for now)
# Uncomment if you want to use customer-managed keys
# resource "aws_kms_key" "dynamodb_key" {
#   description             = "KMS key for DynamoDB encryption"
#   deletion_window_in_days = 10
#   enable_key_rotation     = true
# 
#   tags = {
#     Name    = "${var.service_name}-dynamodb-key"
#     Service = var.service_name
#   }
# }
# 
# resource "aws_kms_alias" "dynamodb_key_alias" {
#   name          = "alias/${var.service_name}-dynamodb"
#   target_key_id = aws_kms_key.dynamodb_key.key_id
# }

# For now, using AWS-managed encryption
resource "aws_kms_key" "dynamodb_key" {
  count = 0 # Disabled - using AWS-managed keys
}

# CloudWatch Alarms for DynamoDB Table Monitoring
resource "aws_cloudwatch_metric_alarm" "dynamodb_read_throttle" {
  alarm_name          = "${var.service_name}-${var.environment}-dynamodb-read-throttle"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "ReadThrottleEvents"
  namespace           = "AWS/DynamoDB"
  period              = "60"
  statistic           = "Sum"
  threshold           = "0"
  alarm_description   = "Alert when read throttling occurs on DynamoDB table"
  alarm_actions       = []

  dimensions = {
    TableName = aws_dynamodb_table.payments.name
  }
}

resource "aws_cloudwatch_metric_alarm" "dynamodb_write_throttle" {
  alarm_name          = "${var.service_name}-${var.environment}-dynamodb-write-throttle"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "WriteThrottleEvents"
  namespace           = "AWS/DynamoDB"
  period              = "60"
  statistic           = "Sum"
  threshold           = "0"
  alarm_description   = "Alert when write throttling occurs on DynamoDB table"
  alarm_actions       = []

  dimensions = {
    TableName = aws_dynamodb_table.payments.name
  }
}

resource "aws_cloudwatch_metric_alarm" "dynamodb_user_errors" {
  alarm_name          = "${var.service_name}-${var.environment}-dynamodb-user-errors"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  metric_name         = "UserErrors"
  namespace           = "AWS/DynamoDB"
  period              = "60"
  statistic           = "Sum"
  threshold           = "5"
  alarm_description   = "Alert when DynamoDB returns user errors (validation, etc.)"
  alarm_actions       = []

  dimensions = {
    TableName = aws_dynamodb_table.payments.name
  }
}

# Outputs
output "dynamodb_table_name" {
  description = "DynamoDB payments table name"
  value       = aws_dynamodb_table.payments.name
}

output "dynamodb_table_arn" {
  description = "ARN of the DynamoDB payments table"
  value       = aws_dynamodb_table.payments.arn
}

output "dynamodb_table_stream_arn" {
  description = "ARN of the DynamoDB table stream (if enabled)"
  value       = aws_dynamodb_table.payments.stream_arn
}
