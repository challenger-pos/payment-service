#!/bin/bash

# Script to configure SQS Queues with Dead Letter Queue for local development (LocalStack)
# This script creates the main queue with a DLQ and configures the redrive policy

set -e

echo "========================================="
echo "Configuring SQS Queues with DLQ"
echo "========================================="

# LocalStack endpoint
ENDPOINT="http://localhost:4566"
REGION="us-east-2"

# Queue names
MAIN_QUEUE="payment-request-queue"
DLQ="payment-request-dlq"

echo ""
echo "1. Creating Dead Letter Queue: $DLQ"
DLQ_URL=$(aws sqs create-queue \
  --endpoint-url $ENDPOINT \
  --region $REGION \
  --queue-name $DLQ \
  --query 'QueueUrl' \
  --output text 2>/dev/null || aws sqs get-queue-url \
  --endpoint-url $ENDPOINT \
  --region $REGION \
  --queue-name $DLQ \
  --query 'QueueUrl' \
  --output text)

echo "   DLQ URL: $DLQ_URL"

# Get DLQ ARN
DLQ_ARN=$(aws sqs get-queue-attributes \
  --endpoint-url $ENDPOINT \
  --region $REGION \
  --queue-url $DLQ_URL \
  --attribute-names QueueArn \
  --query 'Attributes.QueueArn' \
  --output text)

echo "   DLQ ARN: $DLQ_ARN"

echo ""
echo "2. Creating Main Queue: $MAIN_QUEUE with DLQ redrive policy"

# Create redrive policy JSON
REDRIVE_POLICY=$(cat <<EOF
{
  "deadLetterTargetArn": "$DLQ_ARN",
  "maxReceiveCount": "3"
}
EOF
)

# Create main queue with redrive policy and visibility timeout
MAIN_QUEUE_URL=$(aws sqs create-queue \
  --endpoint-url $ENDPOINT \
  --region $REGION \
  --queue-name $MAIN_QUEUE \
  --attributes "{\"RedrivePolicy\":\"$(echo $REDRIVE_POLICY | sed 's/"/\\"/g')\",\"VisibilityTimeout\":\"300\"}" \
  --query 'QueueUrl' \
  --output text 2>/dev/null || \
  (aws sqs get-queue-url \
    --endpoint-url $ENDPOINT \
    --region $REGION \
    --queue-name $MAIN_QUEUE \
    --query 'QueueUrl' \
    --output text && \
   aws sqs set-queue-attributes \
    --endpoint-url $ENDPOINT \
    --region $REGION \
    --queue-url $(aws sqs get-queue-url --endpoint-url $ENDPOINT --region $REGION --queue-name $MAIN_QUEUE --query 'QueueUrl' --output text) \
    --attributes "{\"RedrivePolicy\":\"$(echo $REDRIVE_POLICY | sed 's/"/\\"/g')\",\"VisibilityTimeout\":\"300\"}" && \
   aws sqs get-queue-url \
    --endpoint-url $ENDPOINT \
    --region $REGION \
    --queue-name $MAIN_QUEUE \
    --query 'QueueUrl' \
    --output text))

echo "   Main Queue URL: $MAIN_QUEUE_URL"

echo ""
echo "3. Verifying Queue Attributes"
aws sqs get-queue-attributes \
  --endpoint-url $ENDPOINT \
  --region $REGION \
  --queue-url $MAIN_QUEUE_URL \
  --attribute-names All \
  --output table

echo ""
echo "========================================="
echo "✅ SQS Queues configured successfully!"
echo "========================================="
echo ""
echo "Configuration:"
echo "  Main Queue: $MAIN_QUEUE"
echo "  Dead Letter Queue: $DLQ"
echo "  Max Receive Count: 3"
echo "  Visibility Timeout: 300 seconds (5 minutes)"
echo ""
echo "Behavior:"
echo "  - Messages failing 3 times will move to DLQ"
echo "  - Visibility timeout allows 5 minutes for processing"
echo "  - Idempotency prevents duplicate payment creation"
echo ""
