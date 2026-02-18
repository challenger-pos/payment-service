# Quick Reference: Sending Mock SQS Messages

## 5-Minute Quick Start

### Option 1: Using Java Tests (Fastest)
```bash
# Send 1 message
mvn test -Dtest=SqsMockDataSenderTest#testSendSingleMockPaymentRequest

# Send 5 messages
mvn test -Dtest=SqsMockDataSenderTest#testSendBatchMockPaymentRequests

# Send with custom amount
mvn test -Dtest=SqsMockDataSenderTest#testSendCustomPaymentRequest
```

### Option 2: Using Bash Script
```bash
# Make executable
chmod +x send-mock-payment.sh

# Send 1 message with default amount
./send-mock-payment.sh

# Send 5 messages with $299.99
./send-mock-payment.sh 5 299.99

# Send with LocalStack
USE_LOCALSTACK=true ./send-mock-payment.sh 3 150.00
```

### Option 3: Using Python Script
```bash
# Install boto3 (first time only)
pip install boto3

# Make executable
chmod +x send_mock_payment.py

# Send 1 message
python send_mock_payment.py

# Send 10 messages with LocalStack
python send_mock_payment.py --count 10 --localstack

# Send with specific amount
python send_mock_payment.py --count 5 --amount 299.99
```

### Option 4: Using AWS CLI
```bash
# Single message
aws sqs send-message \
  --queue-url https://sqs.us-east-2.amazonaws.com/ACCOUNT_ID/payment-request-queue \
  --message-body '{"work_order_id":"550e8400-e29b-41d4-a716-446655440000","client_id":"6ba7b810-9dad-11d1-80b4-00c04fd430c8","budget_id":"6ba7b811-9dad-11d1-80b4-00c04fd430c8","order_request":{"type":"online","external_reference":"ORD-001","total_amount":"150.50","payer":{"email":"customer@example.com","first_name":"John"},"transactions":{"payments":[{"amount":"150.50","payment_method":{"id":"pix","type":"bank_transfer"}}]}}}' \
  --region us-east-2
```

---

## File Structure

```
billing-service/
├── src/
│   ├── main/java/
│   │   └── infrastructure/adapter/in/messaging/dto/
│   │       └── MockPaymentDataGenerator.java          # Mock data generation
│   └── test/java/
│       └── infrastructure/adapter/in/messaging/
│           └── SqsMockDataSenderTest.java            # Java test class
├── send-mock-payment.sh                             # Bash script
├── send_mock_payment.py                             # Python script
├── init-localstack.sh                               # LocalStack setup
└── MOCK_SQS_DATA.md                                 # Full documentation
```

---

## What Each Tool Does

| Tool | Best For | Command |
|------|----------|---------|
| **Java Tests** | Development, CI/CD | `mvn test -Dtest=SqsMockDataSenderTest#...` |
| **Bash Script** | Quick testing, automation | `./send-mock-payment.sh` |
| **Python Script** | More control, flexibility | `python send_mock_payment.py` |
| **AWS CLI** | Production, direct SQS | `aws sqs send-message` |
| **LocalStack** | Local development | `./init-localstack.sh` |

---

## Environment Setup

### Development (.env)
```bash
AWS_ACCESS_KEY=test
AWS_SECRET_KEY=test
AWS_REGION=us-east-2
PAYMENT_REQUEST_QUEUE=payment-request-queue
PAYMENT_REQUEST_DLQ=payment-request-dlq
PAYMENT_RESPONSE_SUCCESS_QUEUE=payment-response-success-queue
PAYMENT_RESPONSE_FAILURE_QUEUE=payment-response-failure-queue
```

### LocalStack Setup
```bash
# Start LocalStack
docker-compose up -d localstack

# Initialize queues
./init-localstack.sh

# Check it worked
docker-compose logs -f localstack
```

---

## Common Use Cases

### 1. Test Single Payment
```bash
./send-mock-payment.sh 1 150.00
```

### 2. Stress Test (100 messages)
```bash
./send-mock-payment.sh 100 50.00
```

### 3. Batch Different Amounts
```bash
for amount in 100 150 200 250 300; do
  ./send-mock-payment.sh 5 $amount
done
```

### 4. Continuous Testing (every 5 seconds)
```bash
while true; do
  ./send-mock-payment.sh 1 $(shuf -i 50-500 -n 1).00
  sleep 5
done
```

### 5. Python Batch with Progress
```python
python send_mock_payment.py --count 1000 --amount 99.99
```

---

## Mock Data Sample

### Simple (150.50)
```json
{
  "work_order_id": "550e8400-e29b-41d4-a716-446655440000",
  "client_id": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
  "budget_id": "6ba7b811-9dad-11d1-80b4-00c04fd430c8",
  "order_request": {
    "type": "online",
    "external_reference": "ORD-20260217-001",
    "total_amount": "150.50",
    "payer": {"email": "customer@example.com", "first_name": "John"},
    "transactions": {
      "payments": [{
        "amount": "150.50",
        "payment_method": {"id": "pix", "type": "bank_transfer"}
      }]
    }
  }
}
```

---

## Verification

### Check Messages in Queue
```bash
# AWS CLI
aws sqs receive-message \
  --queue-url https://sqs.us-east-2.amazonaws.com/ACCOUNT/payment-request-queue \
  --region us-east-2

# LocalStack
aws --endpoint-url http://localhost:4566 sqs receive-message \
  --queue-url http://localhost:4566/000000000000/payment-request-queue \
  --region us-east-2
```

### Check Application Logs
```bash
docker-compose logs -f billing-service-app
```

### Monitor Health
```bash
curl http://localhost:8080/actuator/health
```

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Queue not found | Create queue first or use LocalStack init |
| Invalid credentials | Check .env AWS_ACCESS_KEY and AWS_SECRET_KEY |
| Message not processed | Check application logs, verify queue name |
| LocalStack not responding | Restart: `docker-compose restart localstack` |
| Python boto3 not found | Install: `pip install boto3` |
| AWS CLI not found | Install: `awscli` or `awscliv2` |

---

## Next Steps

1. **Choose your method** - Pick one of the 4 options above
2. **Test it** - Send 1-5 messages
3. **Monitor** - Watch the logs and verify messages are processed
4. **Scale** - Use batch sending for load testing
5. **Automate** - Add to CI/CD pipeline if needed

See [MOCK_SQS_DATA.md](MOCK_SQS_DATA.md) for detailed documentation.
