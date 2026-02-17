# Billing Service - Payment Processing with Mercado Pago PIX

**Payment microservice built with Hexagonal Architecture, Queue-based processing, and Mercado Pago integration**

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Technology Stack](#technology-stack)
4. [Quick Start](#quick-start)
5. [Installation & Setup](#installation--setup)
6. [Docker & Deployment](#docker--deployment)
7. [Queue-Based Processing](#queue-based-processing)
8. [Mercado Pago Integration](#mercado-pago-integration)
9. [Payment Processing Flow](#payment-processing-flow)
10. [Payment Entity & States](#payment-entity--states)
11. [Configuration](#configuration)
12. [Running the Application](#running-the-application)
13. [Development](#development)
14. [Troubleshooting](#troubleshooting)
15. [Enterprise Features](#enterprise-features)

---

## Overview

The Billing Service handles payment processing for order payments using:

- **Hexagonal Architecture** (Ports & Adapters pattern) for clean separation of concerns
- **Queue-based asynchronous processing** via AWS SQS
- **Mercado Pago PIX integration** for secure payment processing
- **Idempotency guarantees** (application + database + constraints)
- **Dead Letter Queues** for handling failed payment attempts
- **PostgreSQL 17** for persistent storage

### Key Responsibilities

- ✅ Listen to payment request messages from AWS SQS queue
- ✅ Process payments through Mercado Pago API using PIX payment method
- ✅ Persist payment information with order and client IDs
- ✅ Publish payment response messages to notify other services
- ✅ Generate QR codes for customer payment action
- ✅ Track payment states and handle failures gracefully

---

## Architecture

### Hexagonal Architecture (Ports & Adapters)

The application is organized into three concentric layers:

```
DOMAIN (Center) - Pure Business Logic
    ↓
APPLICATION - Use Cases & Ports (Interfaces)
    ↓
INFRASTRUCTURE - Adapters & External Systems (Periphery)
```

### Project Structure

```
billing-service/
├── src/main/java/com/fiap/billing_service/
│   ├── domain/                          # Core Business Logic
│   │   ├── entity/
│   │   │   └── Payment.java            # Domain payment entity
│   │   ├── valueobject/
│   │   │   └── PaymentStatus.java      # PENDING, PROCESSING, APPROVED, REJECTED, FAILED
│   │   ├── dto/
│   │   │   └── PaymentResponse.java    # Domain response DTO
│   │   └── exception/
│   │       └── PaymentProcessingException.java
│   │
│   ├── application/                     # Use Cases & Port Definitions
│   │   ├── port/
│   │   │   ├── in/
│   │   │   │   └── ProcessPaymentUseCase.java     # Input port interface
│   │   │   └── out/
│   │   │       ├── PaymentGatewayPort.java       # Output port for payment gateway
│   │   │       ├── PaymentRepositoryPort.java    # Output port for persistence
│   │   │       └── PaymentResponseMessagePort.java # Output port for messaging
│   │   └── service/
│   │       └── ProcessPaymentService.java         # Use case implementation
│   │
│   └── infrastructure/                  # Adapters & External Integrations
│       ├── adapter/
│       │   ├── in/                      # Input adapters
│       │   │   ├── messaging/
│       │   │   │   ├── PaymentQueueListener.java
│       │   │   │   └── dto/PaymentRequestDto.java
│       │   │   └── web/
│       │   │       ├── controller/HealthController.java
│       │   │       └── GlobalExceptionHandler.java
│       │   └── out/                     # Output adapters
│       │       ├── messaging/
│       │       │   ├── PaymentResponseMessageAdapter.java
│       │       │   └── SqsMessageSender.java
│       │       ├── payment/
│       │       │   └── MercadoPagoAdapter.java
│       │       └── persistence/
│       │           ├── entity/PaymentEntity.java
│       │           ├── repository/SpringDataPaymentRepository.java
│       │           ├── mapper/PaymentMapper.java
│       │           └── PaymentRepositoryAdapter.java
│       └── config/
│           ├── AwsConfig.java
│           ├── DatabaseConfig.java
│           ├── JacksonConfig.java
│           └── WebConfig.java
│
└── src/main/resources/
    ├── application.yml
    ├── application-development.yml
    ├── application-homologation.yml
    ├── application-production.yml
    └── db/migration/                    # Flyway migrations
        └── V1__init_payments_table.sql
```

### Ports & Adapters Pattern

| Component                      | Type           | Purpose                             |
| ------------------------------ | -------------- | ----------------------------------- |
| **ProcessPaymentUseCase**      | Input Port     | Defines payment processing contract |
| **PaymentQueueListener**       | Input Adapter  | Listens to SQS queue for requests   |
| **PaymentGatewayPort**         | Output Port    | Abstracts payment gateway           |
| **MercadoPagoAdapter**         | Output Adapter | Implements Mercado Pago integration |
| **PaymentRepositoryPort**      | Output Port    | Abstracts database persistence      |
| **PaymentRepositoryAdapter**   | Output Adapter | Implements JPA-based persistence    |
| **PaymentResponseMessagePort** | Output Port    | Abstracts response messaging        |
| **SqsMessageSender**           | Output Adapter | Implements SQS messaging            |

### Architecture Benefits

- 🔓 **Technology Independence**: Business logic isolated from framework details
- 🧪 **High Testability**: Easy to test with mocks
- 🔌 **Flexibility**: Swap implementations (e.g., different payment gateway)
- 📦 **Maintainability**: Clear separation of concerns
- 🚀 **Scalability**: Easy to add new adapters

---

## Technology Stack

| Component        | Version | Purpose                    |
| ---------------- | ------- | -------------------------- |
| Java             | 21      | Language                   |
| Spring Boot      | 4.0.2   | Web framework              |
| Spring Data JPA  | -       | ORM & database abstraction |
| Spring Cloud AWS | 4.0.0   | AWS SQS integration        |
| PostgreSQL       | 17      | Production database        |
| H2 Database      | -       | Development/testing        |
| Mercado Pago SDK | 2.1.4   | Payment gateway            |
| Maven            | 3.9+    | Build tool                 |
| Docker           | 20.10+  | Containerization           |
| Docker Compose   | 2.0+    | Container orchestration    |
| Flyway           | -       | Database migration         |

### Key Dependencies

```xml
<!-- Web & Data -->
<spring-boot-starter-web>                          <!-- REST endpoints -->
<spring-boot-starter-data-jpa>                     <!-- Database ORM -->
<spring-boot-starter-validation>                   <!-- Bean validation -->

<!-- Database -->
<postgresql>                                       <!-- Production DB driver -->
<h2>                                               <!-- Development/test DB -->

<!-- AWS Integration -->
<spring-cloud-aws-starter-sqs>                     <!-- SQS messaging -->

<!-- Payment Gateway -->
<sdk-java>2.1.4</sdk-java>                         <!-- Mercado Pago SDK -->

<!-- Utilities -->
<jackson-datatype-jsr310>                          <!-- JSON date/time handling -->
<spring-dotenv>                                    <!-- .env file support -->
<lombok>                                           <!-- Code generation -->

<!-- Testing -->
<spring-boot-starter-test>                         <!-- Unit & integration tests -->
```

---

## Quick Start

### Pre-requisites

- Docker & Docker Compose installed
- `.env` file with credentials (copy from `.env.example`)
- Ports 8080, 5433, 5050 available

### Option 1: Quick Start Script (Recommended)

```bash
# Make script executable
chmod +x start.sh

# Run interactive menu
./start.sh
```

**Menu options:**

1. Start full stack (App + Database)
2. Start database only (for local development)
3. Start with PgAdmin (for DB management)
4. Stop all services
5. View logs in real-time
6. Clean everything (removes containers & volumes)
7. Setup SQS queues (creates DLQ for development)
8. Show help

### Option 2: Using Makefile

```bash
# View all available commands
make help

# Start everything
make up

# Start database only
make db-only

# View logs
make logs

# Stop services
make down

# Run tests
make test

# Clean up
make clean
```

### Option 3: Docker Compose Direct

```bash
# Full stack
docker-compose up -d --build

# Database only
docker-compose -f docker-compose.dev.yml up -d

# With PgAdmin (dev profile)
docker-compose --profile dev up -d

# View logs
docker-compose logs -f

# Stop
docker-compose down
```

### Verify Installation

```bash
# Check services running
docker-compose ps

# Health check
curl http://localhost:8080/actuator/health

# View logs
docker-compose logs app
```

---

## Installation & Setup

### Prerequisites

- **Java 21+** (for local development)
- **Maven 3.8+** (for building)
- **PostgreSQL 12+** (if not using Docker)
- **AWS credentials** (for SQS access)
- **Mercado Pago credentials**

### Environment Configuration

#### 1. Create `.env` file

```bash
cp .env.example .env
```

#### 2. Configure AWS

```properties
# AWS SQS Configuration
AWS_REGION=us-east-2
AWS_ACCESS_KEY=your-access-key-id
AWS_SECRET_KEY=your-secret-access-key
```

#### 3. Configure Mercado Pago

```properties
# Mercado Pago Sandbox Credentials
MERCADOPAGO_ACCESS_TOKEN=APP_USR-your-token-here
MERCADOPAGO_PUBLIC_KEY=your-public-key-here
```

#### 4. Database Configuration (Docker)

```properties
DB_USERNAME=postgres
DB_PASSWORD=postgres123
```

#### 5. Optional: Email Service (AWS SES)

```properties
AWS_SES_FROM_EMAIL=noreply@billing-service.com
AWS_SES_CONFIGURATION_SET=email-tracking
```

### Build Locally

```bash
# Clean and build
mvn clean install

# Run tests
mvn test

# Package
mvn package

# Run Spring Boot
mvn spring-boot:run
```

### Database Setup

#### With Flyway (Automatic)

Flyway automatically runs migrations on application startup:

```
V1__init_payments_table.sql  → Creates payments table
```

#### Manual Setup

```bash
# Connect to PostgreSQL
psql -h localhost -U postgres -d billing_db

# Run init script (only needed for LocalStack)
psql -h localhost -U postgres -d billing_db < init-scripts/01-init.sql
```

---

## Docker & Deployment

### Dockerfile Overview

**Multi-stage build for optimization:**

- **Stage 1 - Build**: Maven compilation with Java 21
- **Stage 2 - Runtime**: Optimized JRE with Alpine Linux

**Security features:**

- ✅ Non-root user execution
- ✅ Health checks enabled
- ✅ Minimal image size (Alpine)
- ✅ Read-only filesystem compatible

**Healthchecks:**

- Interval: 30 seconds
- Timeout: 3 seconds
- Retries: 3
- Start period: 60 seconds

### Docker Compose Services

#### PostgreSQL 17 Service

```yaml
- Container: billing-service-postgres
- Image: postgres:17-alpine
- Port: 5433:5432
- Database: billing_db
- Credentials: postgres/postgres123
- Health Check: pg_isready -U postgres
- Volume: postgres_data (persistent)
- Network: billing-network
```

#### Application Service

```yaml
- Container: billing-service-app
- Port: 8080:8080
- Depends On: postgres (healthy)
- Environment:
    - SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-development}
    - AWS_REGION: us-east-2
    - All .env variables loaded
- Memory: 512MB minimum, 1GB recommended
- Network: billing-network
```

#### PgAdmin Service (Optional - `--profile dev`)

```yaml
- Container: billing-service-pgadmin
- Port: 5050:80
- Credentials: admin@billing-service.com / admin123
- Web URL: http://localhost:5050
```

### Docker Build Commands

```bash
# Build image
docker build -t billing-service:latest .

# Build with custom tag
docker build -t billing-service:v1.0.0 .

# View image details
docker image ls
docker inspect billing-service:latest
```

### Docker Compose Commands

```bash
# Start services
docker-compose up -d --build

# Start specific service
docker-compose up -d postgres

# Logs
docker-compose logs -f                    # All services
docker-compose logs -f app                # Application only
docker-compose logs -f postgres           # Database only

# Shell access
docker-compose exec app bash              # App container shell
docker-compose exec postgres psql -U postgres  # Database shell

# Stop
docker-compose down                       # Keep volumes
docker-compose down -v                    # Remove volumes

# Resource usage
docker-compose stats
docker stats
```

### Deployment Profiles

#### Development Profile

```yaml
# application-development.yml
spring.jpa.hibernate.ddl-auto: update # Auto create/alter schema
spring.jpa.show-sql: true # Log SQL statements
logging.level: DEBUG # Detailed logging
aws.sqs.auto-create-queue: true # Auto-create SQS queues
```

#### Homologation Profile

```yaml
# application-homologation.yml
spring.datasource.url: ${SPRING_DATASOURCE_URL} # From environment
spring.jpa.hibernate.ddl-auto: validate # Validate only
logging.level: INFO # Information logging
aws.sqs.auto-create-queue: false # Use existing queues
```

#### Production Profile

```yaml
# application-production.yml
spring.datasource.url: ${SPRING_DATASOURCE_URL} # From secrets manager
spring.jpa.hibernate.ddl-auto: validate # No schema changes
logging.level: WARN # Warnings only
aws.sqs.auto-create-queue: false # Strict queue validation
```

---

## Queue-Based Processing

### Architecture Overview

```
Payment Request → SQS Queue → PaymentQueueListener → ProcessPaymentService
                                                            ↓
                                                    MercadoPagoAdapter
                                                            ↓
                                    SuccessQueue OR FailureQueue
```

### Payment Request Queue

**Queue Configuration:**

- **Name**: `payment-request-queue`
- **Visibility Timeout**: 300 seconds (5 minutes)
- **Max Receive Count**: 3 (before moving to DLQ)
- **Acknowledgment**: ON_SUCCESS (removed after processing)
- **Purpose**: Receives payment requests from order service

**Message Format:**

```json
{
  "order_id": "550e8400-e29b-41d4-a716-446655440000",
  "client_id": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
  "amount": 150.5,
  "customer_email": "customer@example.com",
  "customer_document": "12345678900",
  "customer_name": "John Doe",
  "description": "Payment for order #ORD-2024-001"
}
```

### Payment Response Queues

**Success Queue:**

- **Name**: `payment-response-success-queue`
- **Triggers**: When payment status is APPROVED or PROCESSING
- **Purpose**: Notifies order service of successful payment

**Failure Queue:**

- **Name**: `payment-response-failure-queue`
- **Triggers**: When payment status is REJECTED or FAILED
- **Purpose**: Notifies order service of payment rejection

**Response Message Format:**

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "orderId": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
  "clientId": "550e8400-e29b-41d4-a716-446655440111",
  "amount": 150.5,
  "status": "APPROVED",
  "externalPaymentId": "1234567890",
  "paymentMethod": "pix",
  "qrCode": "00020126580014br.gov.bcb.brcode...",
  "qrCodeBase64": "iVBORw0KGgoAAAANSUhEUgAAAA...",
  "createdAt": "2024-02-16T10:00:00",
  "processedAt": "2024-02-16T10:00:30"
}
```

### Dead Letter Queue (DLQ)

**Configuration:**

- **Name**: `payment-request-dlq`
- **Max Receive Count**: 3
- **Purpose**: Stores permanently failed messages
- **Monitoring**: DLQ messages indicate processing failures

**Setup (for LocalStack development):**

```bash
./start.sh
# Select option: 7. Setup SQS queues
```

Or manually:

```bash
chmod +x scripts/setup-sqs-dlq.sh
./scripts/setup-sqs-dlq.sh
```

**What the script does:**

1. Creates main queue: `payment-request-queue`
2. Creates DLQ: `payment-request-dlq`
3. Configures redrive policy with maxReceiveCount=3
4. Sets visibility timeout to 300 seconds

### Message Idempotency

**Three-layer defense against duplicate processing:**

#### 1. Application-Level Check

Before processing, the service checks:

```java
Optional<Payment> existing = paymentRepository.findByOrderId(orderId);
if (existing.isPresent() && !existing.get().getStatus().isPending()) {
  return existing.get();  // Return existing payment
}
```

#### 2. Database Unique Constraint

```sql
ALTER TABLE payments
ADD CONSTRAINT uk_payments_order_id UNIQUE (order_id);
```

#### 3. SQS Visibility Timeout

- Message invisible for 5 minutes after receipt
- Prevents duplicate processing from concurrent instances
- Failed processing: message returns to queue after timeout, moves to DLQ after 3 attempts

**Why Idempotency Matters:**

| Scenario             | Without Idempotency | With Idempotency                         |
| -------------------- | ------------------- | ---------------------------------------- |
| Network timeout      | Duplicate payment   | Same payment returned                    |
| Service crash        | Reprocessing fails  | Existing payment recovered               |
| SQS timeout          | Multiple charges    | Single payment persisted                 |
| Concurrent instances | Race condition      | Database constraint prevents duplication |

**Idempotency Guarantees:**

✅ Duplicate detection logged with WARN level
✅ Race condition prevention with database constraint
✅ Safe retry after crash or network failure
✅ No duplicate charges to customer

---

## Mercado Pago Integration

### Overview

- **Gateway**: Mercado Pago (Production or Sandbox)
- **Payment Method**: PIX (Instant Payment System)
- **Integration Model**: Orders API (`/v1/orders`)
- **QR Code**: Generated for customer payment action
- **Testing**: Sandbox environment with test credentials

### Configuration

```yaml
# application.yml
mercadopago:
  access-token: ${MERCADOPAGO_ACCESS_TOKEN} # APP_USR-your-token
  public-key: ${MERCADOPAGO_PUBLIC_KEY} # PKG_TEST-your-key
```

### Orders API Endpoint

**Base URL:** `https://api.mercadopago.com/v1/orders`

**Authentication:**

- Method: Bearer Token
- Header: `Authorization: Bearer {access-token}`

**Idempotency:**

- Header: `X-Idempotency-Key: {UUID}`
- Prevents duplicate order creation

**Request Body:**

```json
{
  "type": "online",
  "external_reference": "order_ref_550e8400-e29b-41d4-a716",
  "payer": {
    "email": "customer@example.com",
    "first_name": "John",
    "last_name": "Doe",
    "identification": {
      "type": "CPF",
      "number": "12345678900"
    }
  },
  "transactions": [
    {
      "amount": 150.5,
      "description": "Payment for order #ORD-2024-001",
      "payment_method": {
        "id": "pix",
        "type": "bank_transfer"
      }
    }
  ]
}
```

**Response Body:**

```json
{
  "id": "1234567890",
  "status": "PENDING",
  "payer": {
    "email": "customer@example.com"
  },
  "transactions": [
    {
      "id": "9876543210",
      "status": "PENDING",
      "amount": 150.5,
      "payment_method": {
        "id": "pix",
        "type": "bank_transfer",
        "qr_code": "00020126580014br.gov.bcb.brcode...",
        "qr_code_image": "data:image/png;base64,iVBORw0KGgo..."
      }
    }
  ]
}
```

### Payment States

| Status     | Meaning                   | Next State        | Action                 |
| ---------- | ------------------------- | ----------------- | ---------------------- |
| PENDING    | Created, awaiting payment | PROCESSING        | Customer pays PIX      |
| PROCESSING | Transaction in progress   | APPROVED/REJECTED | Mercado Pago processes |
| APPROVED   | Payment successful        | (Final)           | Order fulfilled        |
| REJECTED   | Payment declined          | (Final)           | Notify customer        |
| FAILED     | Technical error           | PENDING           | Retry available        |

### QR Code Handling

**String Format:**

```
00020126580014br.gov.bcb.brcode0136...
```

**Base64 Image Format:**

```
data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA...
```

**Client Usage:**

```javascript
// Display QR code in UI
<img src={qrCodeBase64} alt="PIX QR Code" />;

// Or use string format
QRCode.toCanvas(document.getElementById("qrcode"), qrCode, function (error) {
  if (error) console.error(error);
});
```

### Production Considerations

When moving to production (non-sandbox):

1. **Replace credentials:**

   ```properties
   MERCADOPAGO_ACCESS_TOKEN=APP_USR-production-token
   MERCADOPAGO_PUBLIC_KEY=PKG_PROD-production-key
   ```

2. **Update queue names:**

   ```yaml
   aws.sqs:
     payment-request-queue: production-payment-request
     payment-response-success-queue: production-payment-success
     payment-response-failure-queue: production-payment-failure
   ```

3. **Update API endpoints** if integrating additional services

4. **Enable API authentication** for external queue access

---

## Payment Processing Flow

### Detailed Sequence

```
1. Order Service
   ↓
   Publishes {orderId, clientId, amount, ...} to payment-request-queue
   ↓
2. PaymentQueueListener (@SqsListener)
   ├─ Receives message from SQS
   ├─ Deserializes PaymentRequestDto
   └─ Calls ProcessPaymentService.process()
   ↓
3. ProcessPaymentService
   ├─ Checks for duplicate (idempotency)
   ├─ Creates Payment entity with PENDING status
   ├─ Updates status to PROCESSING
   └─ Calls MercadoPagoAdapter
   ↓
4. MercadoPagoAdapter
   ├─ Prepares request with idempotency UUID
   ├─ Calls Mercado Pago Orders API
   ├─ Parses response with QR code
   └─ Returns PaymentResponse
   ↓
5. ProcessPaymentService
   ├─ Updates Payment with external ID
   ├─ Stores QR code (string + base64)
   ├─ Updates status based on API response
   ├─ Saves to database
   └─ Calls PaymentResponseMessageAdapter
   ↓
6. PaymentResponseMessageAdapter
   ├─ Routes based on payment status:
   │  ├─ SUCCESS (APPROVED/PROCESSING) → payment-response-success-queue
   │  └─ FAILURE (REJECTED/FAILED) → payment-response-failure-queue
   └─ Publishes PaymentResponse message
   ↓
7. Order Service
   ├─ Receives response from appropriate queue
   ├─ Updates order status
   └─ Notifies customer

ERROR HANDLING:
├─ Message deserialization error → DLQ
├─ Duplicate payment detected → Return existing
├─ API timeout/failure → Retry on visibility timeout
├─ After 3 retries → Move to DLQ
└─ Monitor DLQ for manual intervention
```

### Code Sequence

**1. Listen for Payment Request:**

```java
@Component
public class PaymentQueueListener {
    @SqsListener("${aws.sqs.payment-request-queue}")
    public void handlePaymentRequest(PaymentRequestDto request) {
        processPaymentService.process(request);
    }
}
```

**2. Process Payment:**

```java
@Service
public class ProcessPaymentService implements ProcessPaymentUseCase {
    public PaymentResponse process(PaymentRequestDto request) {
        // Check idempotency
        Optional<Payment> existing = paymentRepository.findByOrderId(request.getOrderId());
        if (existing.isPresent()) return mapToResponse(existing.get());

        // Create new payment
        Payment payment = new Payment(
            request.getOrderId(),
            request.getClientId(),
            request.getAmount()
        );
        payment.setStatus(PaymentStatus.PENDING);
        paymentRepository.save(payment);

        // Process via Mercado Pago
        payment.setStatus(PaymentStatus.PROCESSING);
        PaymentResponse mpResponse = paymentGateway.createOrder(request);

        // Update with response
        payment.setExternalPaymentId(mpResponse.getId());
        payment.setQrCode(mpResponse.getQrCode());
        payment.setQrCodeBase64(mpResponse.getQrCodeBase64());
        payment.setStatus(mpResponse.getStatus());
        payment.setProcessedAt(Instant.now());
        paymentRepository.save(payment);

        // Publish response
        messagePort.publishPaymentResponse(payment);

        return mapToResponse(payment);
    }
}
```

**3. Publish Response:**

```java
@Component
public class SqsMessageSender implements PaymentResponseMessagePort {
    public void publishPaymentResponse(Payment payment) {
        String queueUrl = payment.getStatus().isSuccess()
            ? successQueueUrl
            : failureQueueUrl;

        sqsTemplate.convertAndSend(queueUrl, mapToDto(payment));
    }
}
```

---

## Payment Entity & States

### Payment Entity Fields

| Field               | Type          | Description                                     |
| ------------------- | ------------- | ----------------------------------------------- |
| `id`                | UUID          | Primary key, unique identifier                  |
| `orderId`           | UUID          | Foreign key to order service                    |
| `clientId`          | UUID          | Client identifier                               |
| `amount`            | BigDecimal    | Payment amount (precision 10,2)                 |
| `status`            | PaymentStatus | PENDING, PROCESSING, APPROVED, REJECTED, FAILED |
| `externalPaymentId` | String        | Mercado Pago payment ID                         |
| `paymentMethod`     | String        | "pix" (current) or future methods               |
| `qrCode`            | Text          | PIX QR code string                              |
| `qrCodeBase64`      | Text          | PIX QR code as base64 image                     |
| `createdAt`         | Instant       | Payment creation timestamp                      |
| `processedAt`       | Instant       | Payment processing completion                   |
| `errorMessage`      | Text          | Error details if status = FAILED                |

### Database Schema

```sql
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL UNIQUE,
    client_id UUID NOT NULL,
    amount NUMERIC(10, 2) NOT NULL CHECK (amount > 0),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    external_payment_id VARCHAR(255),
    payment_method VARCHAR(50) DEFAULT 'pix',
    qr_code TEXT,
    qr_code_base64 TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    error_message TEXT,

    -- Indexes for query performance
    INDEX idx_order_id (order_id),
    INDEX idx_client_id (client_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_external_payment_id (external_payment_id)
);
```

### Payment Status Enum

```java
public enum PaymentStatus {
    PENDING("PENDING", "Waiting for processing"),
    PROCESSING("PROCESSING", "Being processed by gateway"),
    APPROVED("APPROVED", "Successfully approved"),
    REJECTED("REJECTED", "Rejected by gateway"),
    FAILED("FAILED", "Technical error during processing");

    public boolean isSuccess() {
        return this == APPROVED || this == PROCESSING;
    }

    public boolean isTerminal() {
        return this == APPROVED || this == REJECTED || this == FAILED;
    }
}
```

---

## Configuration

### Environment Variables

**Required:**

```properties
# AWS Configuration
AWS_REGION=us-east-2
AWS_ACCESS_KEY=your-access-key-id
AWS_SECRET_KEY=your-secret-access-key

# Mercado Pago Configuration
MERCADOPAGO_ACCESS_TOKEN=APP_USR-your-token
MERCADOPAGO_PUBLIC_KEY=PKG_TEST-your-key

# Spring Profile
SPRING_PROFILES_ACTIVE=development|homologation|production
```

**Optional:**

```properties
# Database (defaults to Docker values)
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres123

# Email Service (AWS SES)
AWS_SES_FROM_EMAIL=noreply@billing-service.com
AWS_SES_CONFIGURATION_SET=email-tracking
```

### Spring Profiles

#### development (Default)

```yaml
spring:
  profiles:
    active: development
  datasource:
    url: jdbc:postgresql://localhost:5433/billing_db
    username: postgres
    password: postgres123
  jpa:
    hibernate.ddl-auto: update
    show-sql: true
    properties:
      hibernate.format_sql: true

aws:
  region: us-east-2
  sqs:
    payment-request-queue: payment-request-queue
    payment-response-success-queue: payment-response-success-queue
    payment-response-failure-queue: payment-response-failure-queue
    auto-create-queue: true
    visibility-timeout: 300
    max-receive-count: 3

logging:
  level:
    com.fiap.billing_service: DEBUG
    org.springframework.web: DEBUG
    software.amazon.awssdk: DEBUG
```

#### homologation

```yaml
spring:
  profiles:
    active: homologation
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
  jpa:
    hibernate.ddl-auto: validate

aws:
  region: ${AWS_REGION}
  sqs:
    auto-create-queue: false

logging:
  level:
    com.fiap.billing_service: INFO
```

#### production

```yaml
spring:
  profiles:
    active: production
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
  jpa:
    hibernate.ddl-auto: validate

aws:
  region: ${AWS_REGION}
  sqs:
    auto-create-queue: false

logging:
  level:
    com.fiap.billing_service: WARN
```

---

## Running the Application

### Option 1: Docker Compose (Recommended)

```bash
# Full stack (App + DB)
docker-compose up -d --build

# View logs
docker-compose logs -f app

# Stop
docker-compose down
```

### Option 2: Interactive Script

```bash
chmod +x start.sh
./start.sh

# Menu options:
# 1 - Start full stack
# 2 - Start database only
# 3 - Start with PgAdmin
# 4 - Stop all services
# 5 - View logs
# 6 - Clean up
# 7 - Setup SQS queues
# 8 - Help
```

### Option 3: Makefile

```bash
make help        # Show all targets
make up          # Start everything
make down        # Stop everything
make logs        # View logs
make test        # Run tests
make db-only     # Database only
```

### Option 4: Local Development

```bash
# 1. Start database (Docker)
docker-compose -f docker-compose.dev.yml up -d

# 2. Configure .env
export SPRING_PROFILES_ACTIVE=development

# 3. Run from IDE or:
mvn spring-boot:run

# Application available at http://localhost:8080
```

### Service Access

| Service      | URL/Port                              | Credentials                          |
| ------------ | ------------------------------------- | ------------------------------------ |
| Application  | http://localhost:8080                 | -                                    |
| Health Check | http://localhost:8080/actuator/health | -                                    |
| Database     | localhost:5433                        | postgres / postgres123               |
| PgAdmin      | http://localhost:5050                 | admin@billing-service.com / admin123 |

---

## Development

### Building

```bash
# Clean build
mvn clean install

# Package JAR
mvn package

# Skip tests
mvn package -DskipTests
```

### Testing

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ProcessPaymentServiceTest

# Run with coverage
mvn test jacoco:report

# View coverage
open target/site/jacoco/index.html
```

### Code Quality

```bash
# Check style
mvn checkstyle:check

# Static analysis
mvn sonar:sonar

# FindBugs
mvn findbugs:check
```

### IDE Setup

#### VS Code

```bash
# Install extensions
code --install-extension redhat.java
code --install-extension vscjava.vscode-maven
code --install-extension vscjava.vscode-spring-boot
```

#### IntelliJ IDEA

1. Open project
2. Maven → Download Sources & Javadocs
3. Run → Run 'BillingServiceApplication'
4. File → Project Structure → Project SDK → Java 21

### Database Debugging

```bash
# Connect to database
docker-compose exec postgres psql -U postgres -d billing_db

# List tables
\dt

# Query payments
SELECT * FROM payments;

# View payment details
SELECT id, order_id, amount, status, created_at FROM payments WHERE order_id = 'xxx';
```

### Logging

**Correlation ID Tracking:**

```java
@RestController
public class HealthController {
    @GetMapping("/actuator/health")
    public ResponseEntity<?> health() {
        String correlationId = MDC.get("correlationId");
        // All logs include this ID for tracing
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
```

**Log Levels:**

- **DEBUG**: Development (verbose logging)
- **INFO**: Staging (important events)
- **WARN**: Duplicate detection, timeouts
- **ERROR**: Exceptions, failures
- **FATAL**: System-critical failures

```bash
# View logs with correlation ID
docker-compose logs | grep "correlationId"
```

---

## Troubleshooting

### Common Issues

#### 1. Port Already in Use

**Error:** `Error starting ApplicationContext. Port 8080 is already in use`

**Solution:**

```bash
# Kill process on port
lsof -i :8080              # Find process
kill -9 <PID>              # Kill it

# Or use different port
export SERVER_PORT=8081
mvn spring-boot:run
```

#### 2. Database Connection Failed

**Error:** `Unable to obtain a new Connection from the Driver`

**Solution:**

```bash
# Check if PostgreSQL is running
docker-compose ps
# If not running, start it
docker-compose up -d postgres

# Check PostgreSQL logs
docker-compose logs postgres

# Test connection
psql -h localhost -U postgres -p 5433
```

#### 3. AWS Credentials Invalid

**Error:** `InvalidSignatureException`, `Unable to load or find cached SigV4Credentials`

**Solution:**

```bash
# Verify .env file
cat .env | grep AWS

# Check AWS CLI credentials
aws sts get-caller-identity

# Refresh credentials and restart
docker-compose restart app
```

#### 4. Mercado Pago API Error

**Error:** `401 Unauthorized` from Mercado Pago API

**Solution:**

```bash
# Verify token in .env
echo $MERCADOPAGO_ACCESS_TOKEN

# Test API directly
curl -H "Authorization: Bearer $MERCADOPAGO_ACCESS_TOKEN" \
     https://api.mercadopago.com/v1/users/me

# Check if using APP_USR- token (not TEST)
```

#### 5. SQS Queue Not Found

**Error:** `AWS service threw an exception: Queue does not exist`

**Solution:**

```bash
# Setup queues for development
./start.sh
# Select option 7: Setup SQS queues

# Or manually
chmod +x scripts/setup-sqs-dlq.sh
./scripts/setup-sqs-dlq.sh

# Verify queue exists
aws sqs list-queues --endpoint-url http://localhost:4566
```

#### 6. Migration Failed (Flyway)

**Error:** `Migration validation failed`

**Solution:**

```bash
# Check migration file syntax
cat src/main/resources/db/migration/V1__init_payments_table.sql

# Clean schema and retry
mvn flyway:clean
mvn clean install

# Check database logs
docker-compose logs postgres
```

### Debugging

#### Enable Debug Logging

```bash
# application.yml
logging:
  level:
    org.springframework: DEBUG
    com.fiap.billing_service: DEBUG
    software.amazon.awssdk: TRACE
```

#### JVM Remote Debugging

```bash
# application-development.yml
java.opts: -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
```

Connect debugger to `localhost:5005` in IDE.

#### Database Inspection

```bash
# View all payments
docker-compose exec postgres psql -U postgres -d billing_db \
  -c "SELECT id, order_id, amount, status, created_at FROM payments ORDER BY created_at DESC LIMIT 10;"

# Check for duplicates
docker-compose exec postgres psql -U postgres -d billing_db \
  -c "SELECT order_id, COUNT(*) FROM payments GROUP BY order_id HAVING COUNT(*) > 1;"

# View DLQ messages (in LocalStack)
aws sqs receive-message --endpoint-url http://localhost:4566 \
  --queue-url http://localhost:4566/123456789012/payment-request-dlq
```

#### Message Inspection (SQS)

```bash
# Receive message from queue (LocalStack)
aws sqs receive-message \
  --endpoint-url http://localhost:4566 \
  --queue-url http://localhost:4566/123456789012/payment-request-queue

# Send test message
aws sqs send-message \
  --endpoint-url http://localhost:4566 \
  --queue-url http://localhost:4566/123456789012/payment-request-queue \
  --message-body '{"order_id":"test","client_id":"test","amount":100}'
```

### Performance Tuning

```bash
# Increase JVM memory
export JAVA_OPTS="-Xms1G -Xmx2G"
docker-compose up -d

# Database connection pool
spring.datasource.hikari.maximum-pool-size: 20
spring.datasource.hikari.minimum-idle: 5

# SQS polling
aws.sqs.wait-time-seconds: 20
aws.sqs.batch-size: 10
```

### Health Checks

```bash
# Application health
curl http://localhost:8080/actuator/health

# Database health
curl http://localhost:8080/actuator/health/db

# Readiness check
curl http://localhost:8080/actuator/health/readiness

# Liveness check
curl http://localhost:8080/actuator/health/liveness
```

---

## Enterprise Features

### Idempotency Guarantees

**Triple-layer protection:**

1. **Application Level**: Check for existing payment before creating new one
2. **Database Level**: Unique constraint on order_id prevents duplicates
3. **SQS Level**: Visibility timeout + max receive count prevents reprocessing

**Implementation:**

```java
// Application level check
Payment existing = paymentRepository.findByOrderId(orderId)
    .filter(p -> !p.getStatus().equals(PENDING))
    .orElse(null);

if (existing != null) {
    return mapToResponse(existing);  // Return existing instead of creating duplicate
}
```

### Distributed Tracing

**Correlation ID propagation:**

```java
@Component
public class CorrelationIdFilter implements OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) {
        String correlationId = request.getHeader("X-Correlation-Id");
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put("correlationId", correlationId);
        // ... logging and response
    }
}
```

### Dead Letter Queue Monitoring

**Automatic DLQ handling:**

- Messages that fail 3 times automatically move to DLQ
- DLQ monitored for operational alerts
- Manual reprocessing available

```bash
# Check DLQ for failed messages
aws sqs receive-message \
  --endpoint-url http://localhost:4566 \
  --queue-url http://localhost:4566/123456789012/payment-request-dlq \
  --max-number-of-messages 10
```

### Payment Retry Strategy

**On payment failure:**

1. **First Attempt**: Message immediately reprocessed (visibility timeout expires)
2. **Second Attempt**: Message visible again after 5 minutes
3. **Third Attempt**: Last chance before moving to DLQ

**Configuration:**

```yaml
aws:
  sqs:
    visibility-timeout: 300 # 5 minutes
    max-receive-count: 3 # 3 attempts
    message-retention: 1209600 # 14 days
```

### Audit Logging

**All payment events logged:**

```
[2024-02-16T10:00:00] INFO  [ord-123] Payment process started for orderId=ord-123, amount=150.50
[2024-02-16T10:00:01] DEBUG [ord-123] Duplicate check: no existing payment found
[2024-02-16T10:00:02] INFO  [ord-123] Payment status updated to PROCESSING
[2024-02-16T10:00:05] INFO  [ord-123] Mercado Pago API response received: APPROVED
[2024-02-16T10:00:06] INFO  [ord-123] Payment stored with externalId=mp-12345
[2024-02-16T10:00:07] INFO  [ord-123] Response published to success queue
```

### Security Considerations

1. **Credentials Management**:
   - AWS keys loaded from environment variables
   - Never commit `.env` file to repository
   - Rotate keys regularly

2. **Data Protection**:
   - Payment data encrypted at rest (PostgreSQL)
   - TLS for API communication
   - QR codes are non-sensitive (can be regenerated)

3. **Access Control**:
   - SQS queues have IAM policies
   - Database credentials in secrets manager
   - API endpoints protected by API Gateway (in production)

4. **Audit Trail**:
   - All payment operations logged with correlation IDs
   - Audit table for compliance (optional)
   - DLQ messages preserved for investigation

---

## Support & Contributions

For issues or questions:

1. Check [Troubleshooting](#troubleshooting) section
2. Review application logs: `docker-compose logs -f app`
3. Check payment status in database: `SELECT * FROM payments WHERE order_id = 'xxx';`
4. Inspect SQS messages in queue (for LocalStack)

---

**Last Updated**: February 2024  
**Version**: 1.0.0  
**Maintained by**: FIAP Billing Service Team
