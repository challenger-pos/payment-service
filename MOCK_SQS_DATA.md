# Mock SQS Payment Request Data Guide

This guide shows how to create and send mock payment request messages to the SQS queue for testing.

## Quick Start

### 1. Basic Mock Data (JSON Format)

```json
{
  "work_order_id": "550e8400-e29b-41d4-a716-446655440000",
  "client_id": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
  "budget_id": "6ba7b811-9dad-11d1-80b4-00c04fd430c8",
  "description": "Payment for consulting services",
  "order_request": {
    "type": "online",
    "external_reference": "ORD-20260217-001",
    "total_amount": "150.50",
    "payer": {
      "email": "customer@example.com",
      "first_name": "John Doe"
    },
    "transactions": {
      "payments": [
        {
          "amount": "150.50",
          "payment_method": {
            "id": "pix",
            "type": "bank_transfer"
          }
        }
      ]
    }
  }
}
```

## Methods to Send Mock Data

### Method 1: Using Java Tests (Recommended for Development)

#### Simple Test
```bash
cd billing-service
mvn test -Dtest=SqsMockDataSenderTest#testSendSingleMockPaymentRequest
```

#### Batch Test (Send 5 messages)
```bash
mvn test -Dtest=SqsMockDataSenderTest#testSendBatchMockPaymentRequests
```

#### Custom Amount Test
```bash
mvn test -Dtest=SqsMockDataSenderTest#testSendCustomPaymentRequest
```

### Method 2: Using AWS CLI

#### Prerequisites
```bash
# Install AWS CLI
brew install awscli  # macOS
apt-get install awscliv2  # Linux
choco install awscli  # Windows

# Configure credentials
aws configure
# Enter your AWS credentials
```

#### Send Single Message
```bash
# Save JSON to file
cat > payment_request.json << 'EOF'
{
  "work_order_id": "550e8400-e29b-41d4-a716-446655440000",
  "client_id": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
  "budget_id": "6ba7b811-9dad-11d1-80b4-00c04fd430c8",
  "description": "Payment for services",
  "order_request": {
    "type": "online",
    "external_reference": "ORD-20260217-001",
    "total_amount": "150.50",
    "payer": {
      "email": "customer@example.com",
      "first_name": "John Doe"
    },
    "transactions": {
      "payments": [
        {
          "amount": "150.50",
          "payment_method": {
            "id": "pix",
            "type": "bank_transfer"
          }
        }
      ]
    }
  }
}
EOF

# Send to SQS
aws sqs send-message \
  --queue-url https://sqs.us-east-2.amazonaws.com/YOUR_ACCOUNT_ID/payment-request-queue \
  --message-body file://payment_request.json \
  --region us-east-2
```

#### Send Multiple Messages
```bash
for i in {1..5}; do
  aws sqs send-message \
    --queue-url https://sqs.us-east-2.amazonaws.com/YOUR_ACCOUNT_ID/payment-request-queue \
    --message-body "{\"work_order_id\":\"550e8400-e29b-41d4-a716-446655440000\",\"client_id\":\"6ba7b810-9dad-11d1-80b4-00c04fd430c8\",\"budget_id\":\"6ba7b811-9dad-11d1-80b4-00c04fd430c8\",\"order_request\":{\"type\":\"online\",\"external_reference\":\"ORD-20260217-00$i\",\"total_amount\":\"100.00\",\"payer\":{\"email\":\"customer$i@example.com\",\"first_name\":\"Customer $i\"},\"transactions\":{\"payments\":[{\"amount\":\"100.00\",\"payment_method\":{\"id\":\"pix\",\"type\":\"bank_transfer\"}}]}}}" \
    --region us-east-2
done
```

### Method 3: Using LocalStack (Local Development)

#### Start LocalStack with Docker
```bash
# Using docker-compose.yml
cd billing-service

# Add to docker-compose.yml if not present:
version: '3.8'
services:
  localstack:
    image: localstack/localstack:latest
    ports:
      - "4566:4566"
    environment:
      - SERVICES=sqs
      - DEBUG=1
      - DATA_DIR=/tmp/localstack/data
      - DOCKER_HOST=unix:///var/run/docker.sock
    volumes:
      - ./init-local-stack.sh:/docker-entrypoint-initaws.d/init-localstack.sh

# Start LocalStack
docker-compose up -d localstack

# Check if it's running
curl http://localhost:4566/_localstack/health
```

#### Create Queue in LocalStack
```bash
# Create SQS queue
aws --endpoint-url http://localhost:4566 \
  sqs create-queue \
  --queue-name payment-request-queue \
  --region us-east-2

# List queues
aws --endpoint-url http://localhost:4566 \
  sqs list-queues \
  --region us-east-2

# Get queue URL
QUEUE_URL=$(aws --endpoint-url http://localhost:4566 \
  sqs get-queue-url \
  --queue-name payment-request-queue \
  --region us-east-2 \
  --query 'QueueUrl' \
  --output text)

echo $QUEUE_URL
```

#### Send Message to LocalStack
```bash
QUEUE_URL="http://localhost:4566/000000000000/payment-request-queue"

aws --endpoint-url http://localhost:4566 \
  sqs send-message \
  --queue-url $QUEUE_URL \
  --message-body '{"work_order_id":"550e8400-e29b-41d4-a716-446655440000","client_id":"6ba7b810-9dad-11d1-80b4-00c04fd430c8","budget_id":"6ba7b811-9dad-11d1-80b4-00c04fd430c8","order_request":{"type":"online","external_reference":"ORD-20260217-001","total_amount":"150.50","payer":{"email":"customer@example.com","first_name":"John Doe"},"transactions":{"payments":[{"amount":"150.50","payment_method":{"id":"pix","type":"bank_transfer"}}]}}}' \
  --region us-east-2
```

#### View Messages in Queue
```bash
QUEUE_URL="http://localhost:4566/000000000000/payment-request-queue"

# Receive messages
aws --endpoint-url http://localhost:4566 \
  sqs receive-message \
  --queue-url $QUEUE_URL \
  --region us-east-2
```

### Method 4: Using Docker Commands

#### Build and Run Container
```bash
# Build image
docker build -t billing-service:latest .

# Run tests in container
docker run --rm \
  --env AWS_ACCESS_KEY=test \
  --env AWS_SECRET_KEY=test \
  --env AWS_REGION=us-east-2 \
  --env SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/billing_db \
  --env SPRING_DATASOURCE_USERNAME=postgres \
  --env SPRING_DATASOURCE_PASSWORD=postgres123 \
  billing-service:latest \
  mvn test -Dtest=SqsMockDataSenderTest#testSendSingleMockPaymentRequest
```

### Method 5: Use Generated JSON via cURL to Lambda/API

If you expose an endpoint:

```bash
# Generate mock data and send to custom endpoint
curl -X POST http://localhost:8080/api/v1/payments/mock \
  -H "Content-Type: application/json" \
  -d '{
    "work_order_id": "550e8400-e29b-41d4-a716-446655440000",
    "client_id": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
    "budget_id": "6ba7b811-9dad-11d1-80b4-00c04fd430c8",
    "order_request": {
      "type": "online",
      "external_reference": "ORD-20260217-001",
      "total_amount": "150.50",
      "payer": {
        "email": "customer@example.com",
        "first_name": "John Doe"
      },
      "transactions": {
        "payments": [
          {
            "amount": "150.50",
            "payment_method": {
              "id": "pix",
              "type": "bank_transfer"
            }
          }
        ]
      }
    }
  }'
```

## Available Mock Data Generators

The `MockPaymentDataGenerator` class provides:

### Generate Random Payment Request
```java
PaymentRequestDto request = MockPaymentDataGenerator.generatePaymentRequest();
```

### Generate with Custom IDs and Amount
```java
PaymentRequestDto request = MockPaymentDataGenerator.generatePaymentRequest(
    UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
    UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8"),
    UUID.fromString("6ba7b811-9dad-11d1-80b4-00c04fd430c8"),
    BigDecimal.valueOf(299.99)
);
```

### Generate Test IDs
```java
PaymentRequestDto request = MockPaymentDataGenerator.generatePaymentRequestWithTestIds();
```

### Generate Batch (Multiple Requests)
```java
PaymentRequestDto[] requests = MockPaymentDataGenerator.generateBatchPaymentRequests(5);
```

### Generate JSON Strings
```java
String json = MockPaymentDataGenerator.generatePaymentRequestJson();
String json = MockPaymentDataGenerator.generatePaymentRequestJsonWithAmount(299.99);
```

## Test Scenarios

### Scenario 1: Successful Payment
```json
{
  "work_order_id": "550e8400-e29b-41d4-a716-446655440000",
  "client_id": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
  "budget_id": "6ba7b811-9dad-11d1-80b4-00c04fd430c8",
  "order_request": {
    "external_reference": "ORD-SUCCESS",
    "total_amount": "100.00",
    "payer": {
      "email": "success@test.com",
      "first_name": "Success Test"
    },
    "transactions": {
      "payments": [{
        "amount": "100.00",
        "payment_method": {"id": "pix", "type": "bank_transfer"}
      }]
    }
  }
}
```

### Scenario 2: Failure Simulation (for testing error handling)
```json
{
  "work_order_id": "550e8400-e29b-41d4-a716-446655440001",
  "client_id": "invalid-uuid",
  "budget_id": "6ba7b811-9dad-11d1-80b4-00c04fd430c8",
  "order_request": {
    "external_reference": "ORD-FAIL",
    "total_amount": "invalid",
    "payer": {
      "email": "fail@test.com",
      "first_name": "Fail Test"
    },
    "transactions": {
      "payments": [{
        "amount": "invalid",
        "payment_method": {"id": "pix", "type": "bank_transfer"}
      }]
    }
  }
}
```

### Scenario 3: Large Amount (stress test)
```json
{
  "work_order_id": "550e8400-e29b-41d4-a716-446655440002",
  "client_id": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
  "budget_id": "6ba7b811-9dad-11d1-80b4-00c04fd430c8",
  "order_request": {
    "external_reference": "ORD-LARGE",
    "total_amount": "99999.99",
    "payer": {
      "email": "large@test.com",
      "first_name": "Large Amount"
    },
    "transactions": {
      "payments": [{
        "amount": "99999.99",
        "payment_method": {"id": "pix", "type": "bank_transfer"}
      }]
    }
  }
}
```

## Monitoring Messages

### Check Queue Metrics
```bash
aws sqs get-queue-attributes \
  --queue-url https://sqs.us-east-2.amazonaws.com/YOUR_ACCOUNT_ID/payment-request-queue \
  --attribute-names All \
  --region us-east-2
```

### Check Dead Letter Queue
```bash
aws sqs receive-message \
  --queue-url https://sqs.us-east-2.amazonaws.com/YOUR_ACCOUNT_ID/payment-request-dlq \
  --region us-east-2
```

### Watch Logs in Docker
```bash
docker-compose logs -f billing-service-app
```

## Troubleshooting

### Issue: Queue not found
```bash
# List available queues
aws sqs list-queues --region us-east-2
```

### Issue: Invalid credentials
```bash
# Check AWS credentials
aws sts get-caller-identity

# Verify in .env file
cat .env | grep AWS
```

### Issue: Message not processed
```bash
# Check application logs
docker-compose logs billing-service-app

# Check if listener is active
curl http://localhost:8080/actuator/health
```

### Issue: LocalStack not working
```bash
# Check LocalStack status
docker-compose ps

# Check LocalStack logs
docker-compose logs localstack

# Restart LocalStack
docker-compose restart localstack
```
