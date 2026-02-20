#!/bin/sh

# DynamoDB Local Table Initialization Script
# Creates the payments table with workOrderId as the only key (partition key)
# workOrderId is unique per business requirements

set -e

DYNAMODB_ENDPOINT="${DYNAMODB_ENDPOINT:-http://localhost:8000}"
TABLE_NAME="${DYNAMODB_TABLE_NAME:-payments}"
REGION="${AWS_REGION:-us-east-1}"
MAX_RETRIES=30
RETRY_INTERVAL=2

echo "============================================"
echo "Initializing DynamoDB Local Table"
echo "============================================"
echo "Endpoint: $DYNAMODB_ENDPOINT"
echo "Table: $TABLE_NAME"
echo "Region: $REGION"
echo ""

# Wait for DynamoDB Local to be available
echo "Waiting for DynamoDB Local to be available..."
retry_count=0
while true; do
  if curl -s "$DYNAMODB_ENDPOINT" > /dev/null 2>&1; then
    break
  fi
  
  retry_count=$((retry_count + 1))
  if [ $retry_count -ge $MAX_RETRIES ]; then
    echo "ERROR: DynamoDB Local did not become available after $MAX_RETRIES attempts"
    exit 1
  fi
  
  echo "  Attempt $retry_count/$MAX_RETRIES - retrying in ${RETRY_INTERVAL}s..."
  sleep $RETRY_INTERVAL
done

echo "✓ DynamoDB Local is available"
echo ""

# Check if table already exists
echo "Checking if table '$TABLE_NAME' already exists..."
TABLE_EXISTS=$(aws dynamodb describe-table \
  --endpoint-url "$DYNAMODB_ENDPOINT" \
  --table-name "$TABLE_NAME" \
  --region "$REGION" \
  2>/dev/null || echo "")

if [ -n "$TABLE_EXISTS" ]; then
  echo "✓ Table '$TABLE_NAME' already exists, skipping creation"
  echo ""
  exit 0
fi

# Create payments table with workOrderId as partition key (unique identifier)
echo "Creating table '$TABLE_NAME'..."
aws dynamodb create-table \
  --endpoint-url "$DYNAMODB_ENDPOINT" \
  --table-name "$TABLE_NAME" \
  --attribute-definitions \
    AttributeName=workOrderId,AttributeType=S \
  --key-schema \
    AttributeName=workOrderId,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region "$REGION" \
  --tags Key=Environment,Value=local Key=Service,Value=billing-service 2>&1 || true

echo "✓ Table creation initiated"
echo ""

# Wait for table to be active
echo "Waiting for table to become ACTIVE..."
max_wait=30
wait_count=0
while true; do
  table_status=$(aws dynamodb describe-table \
    --endpoint-url "$DYNAMODB_ENDPOINT" \
    --table-name "$TABLE_NAME" \
    --region "$REGION" \
    --query 'Table.TableStatus' \
    --output text 2>/dev/null || echo "UNKNOWN")

  if [ "$table_status" = "ACTIVE" ]; then
    echo "✓ Table is now ACTIVE"
    break
  fi

  wait_count=$((wait_count + 1))
  if [ $wait_count -ge $max_wait ]; then
    echo "WARNING: Table status is $table_status after $max_wait seconds"
    break
  fi

  echo "  Current status: $table_status, waiting..."
  sleep 1
done

echo ""
echo "============================================"
echo "DynamoDB Local initialization complete!"
echo "============================================"
echo ""
echo "✓ Ready for operations"

