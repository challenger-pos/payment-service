# Billing Service - Payment Processing with Mercado Pago PIX

**Payment microservice built with Hexagonal Architecture, Queue-based processing, and Mercado Pago integration**

> 🚀 Complete documentation consolidating 7 markdown files into a single comprehensive guide

---

## 📋 Table of Contents

### I. Getting Started

1. [Overview](#overview)
2. [Quick Start](#quick-start) ⭐ **START HERE**
3. [Architecture](#architecture-overview)
4. [Technology Stack](#technology-stack)

### II. Development Setup

5. [Installation & Prerequisites](#installation--prerequisites)
6. [Environment Configuration](#environment-configuration)
7. [Running the Application](#running-the-application)
8. [Development Workflow](#development-workflow)

### III. Core Concepts

9. [Hexagonal Architecture Details](#hexagonal-architecture-ports--adapters)
10. [Queue-Based Processing](#queue-based-processing)
11. [Payment Processing Flow](#payment-processing-flow)
12. [Payment Entity & States](#payment-entity--states)
13. [Mercado Pago Integration](#mercado-pago-integration)

### IV. Testing & Mock Data

14. [Unit & Integration Tests](#development)
15. [Testing Mock Payment Requests](#testing-mock-payment-requests)
    - [Quick Reference](#quick-reference-sending-mock-payments)
    - [Method 1: Java Tests](#method-1-using-java-tests)
    - [Method 2: AWS CLI](#method-2-using-aws-cli)
    - [Method 3: LocalStack](#method-3-using-localstack)
    - [Method 4: Docker Commands](#method-4-using-docker-commands)
    - [Advanced Mock Data Scenarios](#advanced-mock-data-scenarios)

### V. CI/CD Pipeline 🚀

16. [CI/CD Pipeline - Overview & Implementation](#cicd-pipeline-complete-guide)
    - [Implementation Status](#pipeline-implementation-status-)
    - [8-Stage Pipeline Architecture](#8-stage-pipeline-architecture)
    - [Quality Gates & Metrics](#quality-gates-and-metrics)
17. [CI/CD Complete Setup Guide](#cicd-setup-complete-instructions)
    - [GitHub Secrets Configuration](#github-secrets-setup-required)
    - [SonarCloud Integration](#sonarcloud-setup-guide)
    - [Docker Hub Setup](#docker-hub-integration-setup)
    - [AWS Configuration](#aws-infrastructure-setup)
    - [GitHub Environments & Branch Protection](#github-environment-and-branch-protection-setup)
    - [Monitoring & Troubleshooting](#cicd-monitoring-execution)
18. [CI/CD Implementation Checklist](#cicd-implementation-checklist)
    - [Phase 1: Local Validation](#phase-1-local-validation)
    - [Phase 2-10: Configuration Phases](#phase-2-sonarcloud-setup)
    - [Final Verification](#final-verification)

### VI. Infrastructure & Deployment

19. [Docker & Containers](#docker--deployment)
20. [Kubernetes Deployment](#kubernetes-deployment)
21. [Terraform Configuration](#terraform-configuration-complete-guide)
    - [File Structure](#file-structure-1)
    - [Deployment by Environment](#deployment-examples)
    - [Variables & Defaults](#variables-and-configuration)
    - [Outputs & Monitoring](#outputs)

### VII. Database & Migrations

22. [DynamoDB Configuration](#database--migrations)
23. [PostgreSQL → DynamoDB Migration Guide](#dynamodb-migration-guide)
    - [Dependency Changes](#dependency-changes-pomxml)
    - [Entity Transformation](#entity-transformation)
    - [Repository Refactoring](#repository-refactoring)
    - [Configuration Updates](#configuration-updates-)
    - [Deployment Steps](#deployment-steps)

### VIII. Configuration & Operations

24. [Configuration Reference](#configuration)
25. [Spring Profiles](#spring-profiles)
26. [Monitoring & Observability](#monitoring--observability)
27. [Troubleshooting](#troubleshooting)

### IX. Advanced Topics

28. [Enterprise Features](#enterprise-features)
29. [Performance Tuning](#performance-tuning)
30. [Support & Contributing](#support--contributions)

---

# Part I: Getting Started

## Overview

The Billing Service handles payment processing for order payments using:

- **Hexagonal Architecture** (Ports & Adapters pattern) for clean separation of concerns
- **Queue-based asynchronous processing** via AWS SQS
- **Mercado Pago PIX integration** for secure payment processing
- **Idempotency guarantees** (application-level + DynamoDB constraints)
- **Dead Letter Queues** for handling failed payment attempts
- **Amazon DynamoDB** for NoSQL persistent storage (pay-per-request capacity mode)

### Key Responsibilities

- ✅ Listen to payment request messages from AWS SQS queue
- ✅ Process payments through Mercado Pago API using PIX payment method
- ✅ Persist payment information with order and client IDs
- ✅ Publish payment response messages to notify other services
- ✅ Generate QR codes for customer payment action
- ✅ Track payment states and handle failures gracefully

---

## Quick Start

### Pre-requisites

- Docker & Docker Compose installed
- `.env` file with credentials (copy from `.env.example`)
- Ports 8080, 8000 (DynamoDB Local) available

### Option 1: Quick Start Script (Recommended)

```bash
# Make script executable
chmod +x start.sh

# Run interactive menu
./start.sh
```

**Menu options:**

1. Start full stack (App + DynamoDB Local)
2. Start DynamoDB Local only (for local development)
3. View logs in real-time
4. Stop all services
5. Clean everything (removes containers & volumes)
6. Setup SQS queues (creates DLQ for development)
7. Show help

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

# DynamoDB Local only
docker-compose up -d dynamodb-local

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

# Verify DynamoDB is running
curl http://localhost:8000/

# View logs
docker-compose logs app
```

---

## Architecture Overview

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
│           ├── DatabaseConfig.java          # DynamoDB configuration
│           ├── JacksonConfig.java
│           └── WebConfig.java
│
└── src/main/resources/
    ├── application.yml
    ├── application-development.yml
    ├── application-homologation.yml
    └── application-production.yml
```

### Ports & Adapters Pattern

| Component                      | Type           | Purpose                             |
| ------------------------------ | -------------- | ----------------------------------- |
| **ProcessPaymentUseCase**      | Input Port     | Defines payment processing contract |
| **PaymentQueueListener**       | Input Adapter  | Listens to SQS queue for requests   |
| **PaymentGatewayPort**         | Output Port    | Abstracts payment gateway           |
| **MercadoPagoAdapter**         | Output Adapter | Implements Mercado Pago integration |
| **PaymentRepositoryPort**      | Output Port    | Abstracts DynamoDB persistence      |
| **PaymentRepositoryAdapter**   | Output Adapter | Implements DynamoDB operations      |
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

| Component        | Version | Purpose                         |
| ---------------- | ------- | ------------------------------- |
| Java             | 21      | Language                        |
| Spring Boot      | 4.0.2   | Web framework                   |
| Spring Cloud AWS | 4.0.0   | AWS integration (SQS, DynamoDB) |
| DynamoDB         | Local   | NoSQL database (On-Demand)      |
| AWS SDK v2       | 2.24.9  | DynamoDB Enhanced client        |
| Mercado Pago SDK | 2.1.4   | Payment gateway                 |
| Maven            | 3.9+    | Build tool                      |
| Docker           | 20.10+  | Containerization                |
| Docker Compose   | 2.0+    | Container orchestration         |
| TestContainers   | 1.19.7  | DynamoDB testing container      |

### Key Dependencies

```xml
<!-- Web & Data -->
<spring-boot-starter-web>                          <!-- REST endpoints -->
<spring-boot-starter-validation>                   <!-- Bean validation -->

<!-- AWS DynamoDB -->
<software.amazon.awssdk:dynamodb-enhanced>         <!-- DynamoDB Enhanced Client -->
<software.amazon.awssdk:dynamodb>                  <!-- DynamoDB Core -->

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
<testcontainers>1.19.7</testcontainers>           <!-- DynamoDB Local for tests -->
<testcontainers-dynamodb>1.19.7</testcontainers>  <!-- DynamoDB container -->
```

---

# Part II: Development Setup

## Installation & Prerequisites

### Prerequisites

- **Java 21+** (for local development)
- **Maven 3.8+** (for building)
- **Docker** (for DynamoDB Local - if not using AWS-managed DynamoDB)
- **AWS credentials** (for SQS and DynamoDB access)
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

---

## Environment Configuration

### Required Variables

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

### Optional Variables

```properties
# Database (defaults to Docker values)
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres123

# Email Service (AWS SES)
AWS_SES_FROM_EMAIL=noreply@billing-service.com
AWS_SES_CONFIGURATION_SET=email-tracking
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
# 3 - View logs
# 4 - Stop services
# 5 - Clean up
# 6 - Setup SQS queues
# 7 - Help
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

## Development Workflow

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

# Part III: Core Concepts

## Hexagonal Architecture - Ports & Adapters

### Domain Layer (Core Business Logic)

```java
@DynamoDbBean
public class Payment {
    private String paymentId;
    private String orderId;
    private String clientId;
    private BigDecimal amount;
    private PaymentStatus status;
    private String externalPaymentId;
    private String qrCode;
    private Instant createdAt;
    private Instant processedAt;
}

public enum PaymentStatus {
    PENDING, PROCESSING, APPROVED, REJECTED, FAILED
}
```

### Application Layer (Use Cases)

```java
// Input Port - Defines contract
public interface ProcessPaymentUseCase {
    PaymentResponse process(PaymentRequestDto request);
}

// Output Port - Abstracts payment gateway
public interface PaymentGatewayPort {
    PaymentResponse createOrder(PaymentRequestDto request);
}

// Output Port - Abstracts persistence
public interface PaymentRepositoryPort {
    Payment save(Payment payment);
    Optional<Payment> findByOrderId(String orderId);
}

// Output Port - Abstracts messaging
public interface PaymentResponseMessagePort {
    void publishPaymentResponse(Payment payment);
}
```

### Infrastructure Layer (Adapters)

```java
// Input Adapter - SQS Listener
@Component
public class PaymentQueueListener {
    @SqsListener("${aws.sqs.payment-request-queue}")
    public void handlePaymentRequest(PaymentRequestDto request) {
        processPaymentService.process(request);
    }
}

// Output Adapter - Mercado Pago
@Component
public class MercadoPagoAdapter implements PaymentGatewayPort {
    public PaymentResponse createOrder(PaymentRequestDto request) {
        // Implementation details
    }
}

// Output Adapter - DynamoDB
@Component
public class PaymentRepositoryAdapter implements PaymentRepositoryPort {
    public Payment save(Payment payment) {
        // Implementation details
    }
}

// Output Adapter - SQS Sender
@Component
public class SqsMessageSender implements PaymentResponseMessagePort {
    public void publishPaymentResponse(Payment payment) {
        // Implementation details
    }
}
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

# Part IV: Testing & Mock Data

## Testing Mock Payment Requests

### Quick Reference: Sending Mock Payments

#### Option 1: Using Java Tests (Fastest)

```bash
# Send 1 message
mvn test -Dtest=SqsMockDataSenderTest#testSendSingleMockPaymentRequest

# Send 5 messages
mvn test -Dtest=SqsMockDataSenderTest#testSendBatchMockPaymentRequests

# Send with custom amount
mvn test -Dtest=SqsMockDataSenderTest#testSendCustomPaymentRequest
```

#### Option 2: Using Bash Script

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

#### Option 3: Using Python Script

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

#### Option 4: Using AWS CLI

```bash
# Single message
aws sqs send-message \
  --queue-url https://sqs.us-east-2.amazonaws.com/ACCOUNT_ID/payment-request-queue \
  --message-body '{"work_order_id":"550e8400-e29b-41d4-a716-446655440000","client_id":"6ba7b810-9dad-11d1-80b4-00c04fd430c8","budget_id":"6ba7b811-9dad-11d1-80b4-00c04fd430c8","order_request":{"type":"online","external_reference":"ORD-001","total_amount":"150.50","payer":{"email":"customer@example.com","first_name":"John"},"transactions":{"payments":[{"amount":"150.50","payment_method":{"id":"pix","type":"bank_transfer"}}]}}}' \
  --region us-east-2
```

### Tool Comparison

| Tool              | Best For                  | Command                                     |
| ----------------- | ------------------------- | ------------------------------------------- |
| **Java Tests**    | Development, CI/CD        | `mvn test -Dtest=SqsMockDataSenderTest#...` |
| **Bash Script**   | Quick testing, automation | `./send-mock-payment.sh`                    |
| **Python Script** | More control, flexibility | `python send_mock_payment.py`               |
| **AWS CLI**       | Production, direct SQS    | `aws sqs send-message`                      |
| **LocalStack**    | Local development         | `./init-localstack.sh`                      |

### Method 1: Using Java Tests

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

### Method 3: Using LocalStack

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
  billing-service:latest \
  mvn test -Dtest=SqsMockDataSenderTest#testSendSingleMockPaymentRequest
```

### Advanced Mock Data Scenarios

#### Scenario 1: Successful Payment

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
      "payments": [
        {
          "amount": "100.00",
          "payment_method": { "id": "pix", "type": "bank_transfer" }
        }
      ]
    }
  }
}
```

#### Scenario 2: Failure Simulation

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
      "payments": [
        {
          "amount": "invalid",
          "payment_method": { "id": "pix", "type": "bank_transfer" }
        }
      ]
    }
  }
}
```

#### Scenario 3: Large Amount (stress test)

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
      "payments": [
        {
          "amount": "99999.99",
          "payment_method": { "id": "pix", "type": "bank_transfer" }
        }
      ]
    }
  }
}
```

---

# Part V: CI/CD Pipeline

## CI/CD Pipeline: Complete Guide

### Pipeline Implementation Status ✅

The GitHub Actions CI/CD pipeline has been **fully implemented** with all required stages:

**Build → Test → Quality Check → Docker Build → Security Scan → Deploy (Dev/Homolog/Prod)**

### What Was Implemented

#### 1. GitHub Actions Workflow

- **File**: `.github/workflows/ci-cd.yml`
- **Triggers**: Push to main/develop, Pull requests, Manual workflow dispatch
- **8 Sequential Stages**:
  1. Build Application (Maven compile + package)
  2. Automated Tests (JUnit + Spring integration tests)
  3. SonarQube Quality Gate (Code analysis + metrics)
  4. Docker Build & Push (Docker Hub, multi-stage)
  5. Security Scan (Trivy vulnerability scanning)
  6. Deploy Development (AWS EKS + Terraform)
  7. Deploy Homologation (5 min after Dev)
  8. Deploy Production (Manual approval required)

#### 2. Maven Configuration

- **Plugins Added**:
  - `maven-surefire-plugin`: Run unit tests, generate reports
  - `maven-failsafe-plugin`: Run integration tests
  - `jacoco-maven-plugin`: Code coverage analysis (80% threshold)
  - `sonar-maven-plugin`: SonarQube integration
  - Surefire & Failsafe report plugins

- **File**: `pom.xml`
- **Coverage Target**: >= 80% (enforced)

#### 3. Terraform Configuration

- **Enhanced Variables**:
  - `image_repository`: Docker registry source
  - `image_tag`: Dynamic image tag (commit SHA)
- **New Outputs**:
  - `deployed_image_tag`: For rollback capability
  - `deployed_image`: Full image reference
- **Files**: `terraform/variables.tf`, `terraform/outputs.tf`

### 8-Stage Pipeline Architecture

```
PR to main:
  ├─ Build ✓
  ├─ Test ✓
  ├─ Quality Check ✓
  └─ Report in PR comment

Push to main:
  ├─ Build ✓
  ├─ Test ✓
  ├─ Quality Check ✓
  ├─ Docker Build & Push ✓
  ├─ Security Scan ✓
  ├─ Deploy Dev ✓
  ├─ Wait 5 minutes
  ├─ Deploy Homolog ✓
  ├─ Deploy Prod (MANUAL APPROVAL)
  └─ Auto-rollback if health check fails
```

**Total Pipeline Time**: ~20-25 minutes (Dev + Homolog + approval wait)

### Quality Gates and Metrics

#### Code Quality (SonarQube)

- Coverage: >= 80% ❌ Will fail deployment
- Bugs: 0
- Vulnerabilities: 0
- Code Smells: < threshold
- Duplicated Lines: < 5%

#### Security

- Trivy scans all Docker images
- CVE database in GitHub Security tab
- Critical/High severity items logged (non-blocking)

#### Testing

- JaCoCo enforces 80% code coverage
- Unit tests + integration tests required
- Test reports published to GitHub Actions

### Required Infrastructure (Pre-Setup)

#### Cloud Resources (AWS)

- ✅ 3 EKS clusters: `billing-service-dev`, `billing-service-homolog`, `billing-service-prod`
- ✅ S3 bucket for Terraform state (with versioning)
- ✅ DynamoDB table for Terraform locks
- ✅ IAM role: `github-actions-billing-service` (OIDC trust)

#### External Services

- ✅ SonarCloud account (free tier available)
- ✅ Docker Hub account (free public repositories)
- ✅ GitHub repository with branch protection

#### Credentials Required (11 secrets)

```
Docker Hub:
  ├─ DOCKER_HUB_USERNAME
  └─ DOCKER_HUB_TOKEN

SonarCloud:
  ├─ SONAR_TOKEN
  ├─ SONAR_ORGANIZATION
  └─ SONAR_HOST_URL

AWS:
  ├─ AWS_ROLE_TO_ASSUME
  ├─ AWS_REGION
  └─ TF_BACKEND_BUCKET

Optional (if not using OIDC):
  ├─ AWS_ACCESS_KEY_ID
  └─ AWS_SECRET_ACCESS_KEY
```

---

## CI/CD Setup: Complete Instructions

### GitHub Secrets: Setup Required

All secrets must be configured in: **Settings > Secrets and variables > Actions**

#### Docker Hub Integration

```
DOCKER_HUB_USERNAME
├─ Descrição: Seu usuário no Docker Hub
└─ Valor: seu_usuario_dockerhub

DOCKER_HUB_TOKEN
├─ Descrição: Token de autenticação do Docker Hub
├─ Como gerar: Docker Hub → Account Settings → Security → New Access Token
└─ Permissões: Read & Write
```

#### AWS Integration (OIDC Recomendado)

**Opção 1: Using GitHub OpenID Connect (RECOMENDADO - mais seguro)**

```
AWS_ROLE_TO_ASSUME
├─ Descrição: ARN da role IAM para assumir
├─ Formato: arn:aws:iam::123456789012:role/github-actions-billing-service
└─ Permissões: EKS, Terraform backend access
```

**Setup AWS OIDC (uma única vez):**

```bash
# 1. Criar o OIDC provider no AWS
aws iam create-open-id-connect-provider \
  --url https://token.actions.githubusercontent.com \
  --client-id-list sts.amazonaws.com \
  --thumbprint-list 1b511abead59c6ce207077c0ef0cae8f148d8e93 \
  --region us-east-1

# 2. Criar IAM Role: github-actions-billing-service
# Trust Policy:
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::123456789012:oidc-provider/token.actions.githubusercontent.com"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "token.actions.githubusercontent.com:aud": "sts.amazonaws.com",
          "token.actions.githubusercontent.com:sub": "repo:YOUR_GITHUB_ORG/organization:ref:refs/heads/main"
        }
      }
    }
  ]
}

# 3. Attach permissions: AmazonEKSClusterPolicy, AmazonECSTaskExecutionRolePolicy
# 4. Adicionar inline policy para Terraform backend:
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:DeleteObject",
        "s3:GetObjectVersion",
        "dynamodb:PutItem",
        "dynamodb:GetItem",
        "dynamodb:DeleteItem",
        "dynamodb:DescribeTable"
      ],
      "Resource": [
        "arn:aws:s3:::your-terraform-bucket/*",
        "arn:aws:dynamodb:*:123456789012:table/terraform-locks"
      ]
    }
  ]
}
```

**Opção 2: Access Keys diretas (menos seguro)**

```
AWS_ACCESS_KEY_ID
└─ Descrição: Access Key ID da sua conta AWS

AWS_SECRET_ACCESS_KEY
└─ Descrição: Secret Access Key da sua conta AWS
```

#### SonarQube Cloud Integration

```
SONAR_TOKEN
├─ Descrição: Token de autenticação do SonarQube Cloud
├─ Como gerar: SonarCloud → My Account → Security → Generate Token
└─ Permissões: Scan and Analyze

SONAR_ORGANIZATION
├─ Descrição: Organization key no SonarCloud
└─ Formato: seu-org-key

SONAR_HOST_URL
├─ Descrição: URL do servidor SonarQube
├─ Para SonarCloud: https://sonarcloud.io
└─ Para self-hosted: https://seu-sonarqube.com
```

#### AWS Terraform Backend

```
TF_BACKEND_BUCKET
├─ Descrição: Nome do bucket S3 para estado Terraform
├─ Formato: billing-service-terraform-state-prod
└─ Nota: Bucket deve ter versionamento habilitado

AWS_REGION
├─ Descrição: Região AWS para os deployments
└─ Valor: us-east-1 (ou a região desejada)
```

### SonarCloud Setup Guide

#### 1. Criar Organização no SonarCloud

```bash
# Acesse https://sonarcloud.io
# 1. Sign up com GitHub
# 2. Create Organization → Link GitHub Organization
# 3. Selecionar repositório "organization"
```

#### 2. Criar Project

```bash
# No SonarCloud:
# 1. My Projects → Create Project
# 2. Select GitHub organization
# 3. Select "billing-service" repository
# 4. Project Key será: github_YOUR_ORG_billing-service
```

#### 3. Configurar Quality Gate

```bash
# No SonarCloud → Settings → Quality Gates

Quality Gate Padrão (Recomendado):
├─ Coverage: >= 80%
├─ Duplicated Lines: < 5%
├─ Maintainability Rating: A
├─ Reliability Rating: A
├─ Security Rating: A
├─ Security Review Rating: A
└─ Blocker Issues: 0
```

#### 4. Configurar Branch Analysis

```bash
# SonarCloud → Project Settings → Branches and Pull Requests
├─ Main branch: main
├─ Branches: Include all branches
├─ Pull Requests: Auto-provision
└─ Issues: Keep open
```

### Docker Hub Integration Setup

- [ ] **Create Docker Hub account**
  - [ ] Visit https://hub.docker.com
  - [ ] Sign up or login

- [ ] **Create Access Token**
  - [ ] Account → Professional Settings → Security
  - [ ] New Access Token
  - [ ] Token description: `github actions-billing-service`
  - [ ] Permissions: Read & Write
  - [ ] Copy token (will not show again)

- [ ] **Create public repository** (optional)
  - [ ] Create → Repository
  - [ ] Repository name: `billing-service`
  - [ ] Visibility: Public
  - [ ] Short description: "Billing microservice with Mercado Pago integration"

### AWS Infrastructure Setup

#### EKS Clusters

Ensure você tem 3 clusters EKS:

```
Development:
├─ Cluster name: billing-service-dev
├─ Namespace: billing-development
└─ Instance type: t3.medium (auto-scaling 2-5)

Homologation:
├─ Cluster name: billing-service-homolog
├─ Namespace: billing-homologation
└─ Instance type: t3.large (auto-scaling 2-5)

Production:
├─ Cluster name: billing-service-prod
├─ Namespace: billing-production
└─ Instance type: t3.xlarge (auto-scaling 3-10)
```

#### S3 Backend Terraform

```bash
# Create bucket and configure:
aws s3api create-bucket \
  --bucket billing-service-terraform-state-prod \
  --region us-east-1

# Enable versioning:
aws s3api put-bucket-versioning \
  --bucket billing-service-terraform-state-prod \
  --versioning-configuration Status=Enabled

# Enable encryption:
aws s3api put-bucket-encryption \
  --bucket billing-service-terraform-state-prod \
  --server-side-encryption-configuration '{
    "Rules": [{
      "ApplyServerSideEncryptionByDefault": {
        "SSEAlgorithm": "AES256"
      }
    }]
  }'

# Block public access:
aws s3api put-public-access-block \
  --bucket billing-service-terraform-state-prod \
  --public-access-block-configuration \
    "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true"
```

#### DynamoDB para Terraform Locks

```bash
aws dynamodb create-table \
  --table-name terraform-locks \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region us-east-1
```

### GitHub Environment and Branch Protection Setup

#### Branch Protection Rules

Configure em: **Settings > Branches > Add rule**

```
Branch name pattern: main

Requirements:
├─ Require a pull request before merging
│  ├─ Dismiss stale pull request approvals: ✓
│  ├─ Require code owner approval: ✓
│  └─ Require status checks to pass: ✓
│
├─ Require status checks to pass before merging:
│  ├─ Build Application: ✓
│  ├─ Automated Tests: ✓
│  ├─ SonarQube Quality Gate: ✓
│  └─ Build and Push Docker Image: ✓
│
├─ Require dismissal of pull request reviews: ✓
│  └─ Dismiss pull request review restrictions: ✓
│
├─ Restrict who can push to matching branches: ❌
│
└─ Include administrators: ✓
```

#### GitHub Environments

Configure em: **Settings > Environments**

**Development**

```
Environment name: development

Protection rules:
├─ Deployment branches: All branches
└─ Reviewers: None (optional)

Secrets (Inherit from repo level):
├─ AWS_ROLE_TO_ASSUME
└─ AWS_REGION
```

**Homologation**

```
Environment name: homologation

Protection rules:
├─ Deployment branches: Selected branches
│  └─ main
└─ Reviewers: 1 required

Secrets (Inherit from repo level):
├─ AWS_ROLE_TO_ASSUME
└─ AWS_REGION
```

**Production**

```
Environment name: production

Protection rules:
├─ Deployment branches: Selected branches
│  └─ main
├─ Reviewers: 2 required
├─ Deployment history: Only allow GitHub Actions
└─ Wait timer: 15 minutes before granting access

Secrets (Inherit from repo level):
├─ AWS_ROLE_TO_ASSUME
└─ AWS_REGION
```

### CI/CD Monitoring Execution

#### 1. GitHub Actions

```
Repository → Actions → CI/CD Pipeline

View details:
├─ Build logs
├─ Test reports
├─ Coverage reports
├─ SonarQube results (comment PR)
└─ Deployment status
```

#### 2. SonarQube

```
SonarCloud → Projects → billing-service

Monitor:
├─ Coverage: Deve aumentar com cada commit
├─ Quality Gate: Status de passou/falhou
├─ Pull Requests: Análise em cada PR
└─ Code hotspots: Areas problemáticas
```

#### 3. AWS CloudWatch

```
EKS Deployment Logs:

# View deployment status
kubectl rollout status deployment/billing-service -n billing-development

# View pod logs
kubectl logs -n billing-development -l app=billing-service -f

# View metrics
kubectl top nodes
kubectl top pods -n billing-development
```

### Running the Pipeline Manually

#### Trigger workflow via GitHub CLI

```bash
# Trigger pipeline para uma branch específica
gh workflow run ci-cd.yml -f environment=

# Trigger com uma ref específica
gh workflow run ci-cd.yml -r main

# Trigger deploy produção especificamente
gh workflow run ci-cd.yml -f environment=production -r main
```

#### Ou via web interface

1. Go to: **Actions > CI/CD Pipeline**
2. Click: **Run workflow**
3. Select branch: **main**
4. Click: **Run workflow**

---

## CI/CD Implementation Checklist

### Phase 1: Local Validation

- [ ] **Build locally**
  ```bash
  cd billing-service
  mvn clean package
  ```
- [ ] **Run unit tests locally**
  ```bash
  mvn test
  ```
- [ ] **Check test coverage locally**

  ```bash
  mvn jacoco:report
  ```

  - [ ] Coverage >= 80%

- [ ] **Build Docker image locally**

  ```bash
  docker build -t billing-service:test .
  ```

- [ ] **Test Docker image**
  ```bash
  docker run -p 8080:8080 billing-service:test
  curl http://localhost:8080/actuator/health
  ```

### Phase 2: SonarCloud Setup

- [ ] **Create SonarCloud account**
- [ ] **Create SonarCloud organization**
- [ ] **Create billing-service project**
- [ ] **Configure Quality Gate** (80% coverage min)
- [ ] **Generate SONAR_TOKEN**
- [ ] **Verify project key**

### Phase 3: Docker Hub Setup

- [ ] **Create Docker Hub account**
- [ ] **Create Access Token**
- [ ] **Create public repository** (optional)

### Phase 4: AWS Configuration

- [ ] **Create OIDC Provider**
- [ ] **Create IAM Role: `github-actions-billing-service`**
- [ ] **Attach permissions to role**
- [ ] **Note the Role ARN**
- [ ] **Create S3 bucket for Terraform state**
- [ ] **Enable versioning**
- [ ] **Enable encryption**
- [ ] **Create DynamoDB table for locks**
- [ ] **Verify EKS Clusters** (3 clusters: dev, homolog, prod)

### Phase 5: GitHub Configuration

This documentation and the consolidated README have provided all necessary information. Continue with:

- [ ] Configure Secrets (11 total)
- [ ] Configure Branch Protection Rules
- [ ] Create Environments (dev, homolog, prod)

### Phase 6: Git & Push

- [ ] Commit changes to feature branch
- [ ] Push to feature branch
- [ ] Create Pull Request

### Phase 7: First Pipeline Run

- [ ] Create Pull Request
- [ ] Review GitHub Actions run
- [ ] Review test reports
- [ ] Review SonarQube analysis
- [ ] Fix any issues found
- [ ] Merge Pull Request

### Phase 8: Validate Production Pipeline

- [ ] Monitor Actions run after merge
- [ ] Approve Production Deployment (if desired)
- [ ] Monitor Production Deployment

### Phase 9: Validation & Testing

- [ ] Verify Docker image in Docker Hub
- [ ] Verify EKS deployment
- [ ] Test application endpoints
- [ ] Monitor SonarQube metrics
- [ ] Check GitHub Actions metrics

### Phase 10: Documentation & Training

- [ ] Review documentation
- [ ] Update team documentation
- [ ] Update main README.md

### Phase 11: Ongoing Maintenance

- [ ] Monitor metrics weekly
- [ ] Update documentation quarterly
- [ ] Review security regularly
- [ ] Optimize performance

### Final Verification

After completing all phases, verify:

```bash
# 1. Check all files exist
ls -la .github/workflows/ci-cd.yml
ls -la terraform/variables.tf
ls -la pom.xml

# 2. Verify pom.xml has plugins
grep "maven-surefire-plugin" pom.xml
grep "jacoco-maven-plugin" pom.xml

# 3. Check Git status
git status

# 4. Verify no secrets in code
grep -r "token" --include="*.yml" --include="*.yaml" .
```

### Success Criteria

Pipeline is ready when:

✅ All phases 1-5 completed without errors
✅ First PR run shows Build, Test, Quality stages passing
✅ First main branch merge triggers full pipeline
✅ Docker image pushed to Docker Hub
✅ All environments deployed successfully
✅ Health checks passing in all environments
✅ SonarCloud shows quality metrics
✅ No secrets exposed in code or logs
✅ Team trained on deployment process

---

# Part VI: Infrastructure & Deployment

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

## Kubernetes Deployment

### Deployment Strategy

This service deploys to EKS via Terraform with three environments (dev, homolog, prod).

### HPA Configuration

- Minimum replicas: 2
- Maximum replicas: 4
- CPU threshold: 70%

### Service & Ingress

- Service Type: ClusterIP (internal)
- Port: 8080
- Health endpoint: `/actuator/health`

---

## Terraform Configuration: Complete Guide

### File Structure

```
terraform/
├── backend.tf                      # S3 backend (sem hardcoded key)
├── deployment.tf                   # Kubernetes deployment
├── secret.tf                       # Kubernetes secret para credenciais
├── configmap.tf                    # Kubernetes configmap
├── service.tf                      # Kubernetes service
├── hpa.tf                          # Horizontal Pod Autoscaler
├── namespace.tf                    # Kubernetes namespace
├── providers.tf                    # Provider configuration
├── variables.tf                    # Variáveis com defaults
├── outputs.tf                      # Outputs
├── terraform.tfvars.dev            # Dev environment (environment only)
├── terraform.tfvars.homologation   # Homolog environment (environment only)
├── terraform.tfvars.production     # Production environment (environment only)
├── secret.tfvars                   # SECRETS (não fazer commit!)
├── secret.tfvars.template          # Exemplo de variáveis secretas
└── terraform.tfvars.example        # Exemplo completo (legado)
```

### Deployment Examples

#### Pré-requisitos

- Terraform v1.0+
- AWS credentials configurados
- kubeconfig acessível (EKS cluster)
- Arquivo `secret.tfvars` com credenciais

#### Exemplo: Deploy em Dev

```bash
# Initiate with backend config
terraform init \
  -backend-config=key=v4/service-billing/dev/terraform.tfstate

# Plan
terraform plan \
  -var-file=terraform.tfvars.dev \
  -var-file=secret.tfvars \
  -out=dev.tfplan

# Apply
terraform apply dev.tfplan
```

#### Exemplo: Deploy em Homologation

```bash
terraform init \
  -backend-config=key=v4/service-billing/homologation/terraform.tfstate

terraform apply \
  -var-file=terraform.tfvars.homologation \
  -var-file=secret.tfvars \
  -auto-approve
```

#### Exemplo: Deploy em Production

```bash
terraform init \
  -backend-config=key=v4/service-billing/production/terraform.tfstate

terraform apply \
  -var-file=terraform.tfvars.production \
  -var-file=secret.tfvars \
  -auto-approve
```

### Variables and Configuration

#### Obrigatórias (sem defaults)

- `environment` - dev | homologation | production

#### Sensíveis (em secret.tfvars)

- `aws_access_key`
- `aws_secret_key`
- `mercadopago_access_token`

#### Com Defaults

- `region` = "us-east-2"
- `project_name` = "challengeone"
- `service_name` = "billing"
- `app_replicas` = 2
- `app_image` = "thiagotierre/billing-service:latest"
- `app_port` = 8080
- `cpu_request` = "250m"
- `cpu_limit` = "500m"
- `memory_request` = "512Mi"
- `memory_limit` = "1Gi"
- `mercadopago_public_key` = "APP_USR-test-public-key"
- `sqs_queue_url` = ""
- `hpa_min_replicas` = 2
- `hpa_max_replicas` = 4
- `hpa_cpu_threshold` = 70
- `eks_state_key` = "v4/kubernetes/dev/terraform.tfstate"
- `dynamodb_state_key` = "v4/dynamodb-billing/dev/terraform.tfstate"

#### Customização

Para alterar valores padrão (ex: aumentar replicas), edite `variables.tf` e aumente o `default` correspondente. Não altere `terraform.tfvars.*` - esses arquivos devem conter apenas `environment`.

### Outputs

```bash
terraform output

namespace              - Kubernetes namespace (payment-service)
service_name           - Kubernetes service name (billing)
service_cluster_ip     - ClusterIP interno
loadbalancer_hostname  - DNS externo (se aplicável)
deployment_name        - Kubernetes deployment name
```

---

# Part VII: Database & Migrations

## Database & Migrations

### DynamoDB Configuration

**Overview:**

- **Table**: Challenge One Payment Service
- **Partition Key**: `paymentId` (UUID)
- **Sort Key**: `createdAt` (ISO-8601 timestamp)
- **Global Secondary Indexes** (GSI):
  - `OrderIdIndex`: `orderId` (for queries by order)
  - `StatusIndex`: `status` (for filtering by payment status)
- **Capacity Mode**: On-Demand (pay-per-request)

---

## DynamoDB Migration Guide

### Ação Necessária: Atualizar Código Java

A infraestrutura Terraform foi atualizada para usar **DynamoDB** ao invés de **PostgreSQL RDS**.

**Status:**

- ✅ **Terraform**: Atualizado e pronto para deploy
- ⏳ **Código Java**: Requer atualização manual

### Alterações de Terraform Já Realizadas

#### Arquivos Modificados

1. **terraform/providers.tf**
   - Remote state: `rds_billing` → `dynamodb_billing`

2. **terraform/configmap.tf**
   - Removido: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`
   - Adicionado: `AWS_DYNAMODB_TABLE_NAME`

3. **terraform/secret.tf**
   - Removido: `SPRING_DATASOURCE_PASSWORD`

4. **terraform/variables.tf**
   - Removido: variável `db_password`

5. **terraform/secret.tfvars**
   - Removido: valor `db_password`

### Dependency Changes (pom.xml)

#### ❌ REMOVER (PostgreSQL/JPA):

```xml
<!-- PostgreSQL -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>

<!-- Spring Data JPA -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- Flyway Migrations -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

#### ✅ ADICIONAR (DynamoDB):

```xml
<!-- AWS SDK v2 - DynamoDB Enhanced Client -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>dynamodb-enhanced</artifactId>
    <version>2.20.26</version>
</dependency>

<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>dynamodb</artifactId>
    <version>2.20.26</version>
</dependency>
```

### Entity Transformation

#### ❌ ANTES (JPA):

```java
import javax.persistence.*;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // ... getters e setters
}
```

#### ✅ DEPOIS (DynamoDB):

```java
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;
import java.time.Instant;

@DynamoDbBean
public class Payment {

    private String paymentId;      // UUID
    private String createdAt;      // ISO-8601 timestamp (Sort Key)
    private String orderId;
    private BigDecimal amount;
    private String currency;
    private String status;         // PENDING, APPROVED, FAILED
    private String paymentMethod;
    private String merchantOrderId;
    private String externalId;
    private Map<String, String> metadata;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("paymentId")
    public String getPaymentId() {
        return paymentId;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute("createdAt")
    public String getCreatedAt() {
        return createdAt;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "OrderIdIndex")
    @DynamoDbAttribute("orderId")
    public String getOrderId() {
        return orderId;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "StatusIndex")
    @DynamoDbAttribute("status")
    public String getStatus() {
        return status;
    }

    // ... outros getters e setters

    @PrePersist
    public void prePersist() {
        if (paymentId == null) {
            paymentId = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = Instant.now().toString();
        }
    }
}
```

### Repository Refactoring

#### ❌ REMOVER (JPA Repository):

```java
@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {
    Optional<Payment> findByOrderId(String orderId);
    List<Payment> findByStatus(PaymentStatus status);
}
```

#### ✅ CRIAR (DynamoDB Repository):

```java
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.*;

@Repository
public class PaymentRepository {

    private final DynamoDbTable<Payment> paymentTable;

    public PaymentRepository(
            DynamoDbEnhancedClient enhancedClient,
            @Value("${aws.dynamodb.table-name}") String tableName) {

        this.paymentTable = enhancedClient.table(
            tableName,
            TableSchema.fromBean(Payment.class)
        );
    }

    /**
     * Salvar/Atualizar pagamento
     */
    public Payment save(Payment payment) {
        payment.prePersist();  // Garantir IDs
        paymentTable.putItem(payment);
        return payment;
    }

    /**
     * Buscar por paymentId (PK) e createdAt (SK)
     */
    public Optional<Payment> findById(String paymentId, String createdAt) {
        Key key = Key.builder()
            .partitionValue(paymentId)
            .sortValue(createdAt)
            .build();

        return Optional.ofNullable(paymentTable.getItem(key));
    }

    /**
     * Buscar por orderId usando GSI OrderIdIndex
     */
    public List<Payment> findByOrderId(String orderId) {
        QueryConditional queryConditional = QueryConditional
            .keyEqualTo(Key.builder()
                .partitionValue(orderId)
                .build());

        return paymentTable
            .index("OrderIdIndex")
            .query(queryConditional)
            .items()
            .stream()
            .toList();
    }

    /**
     * Buscar por status usando GSI StatusIndex
     */
    public List<Payment> findByStatus(String status) {
        QueryConditional queryConditional = QueryConditional
            .keyEqualTo(Key.builder()
                .partitionValue(status)
                .build());

        return paymentTable
            .index("StatusIndex")
            .query(queryConditional)
            .items()
            .stream()
            .toList();
    }

    /**
     * Deletar pagamento
     */
    public void delete(String paymentId, String createdAt) {
        Key key = Key.builder()
            .partitionValue(paymentId)
            .sortValue(createdAt)
            .build();

        paymentTable.deleteItem(key);
    }
}
```

### Configuration Updates

#### ✅ CRIAR: DynamoDbConfig.java

```java
package com.fiap.billing_service.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Configuration
public class DynamoDbConfig {

    @Value("${aws.region:us-east-2}")
    private String awsRegion;

    @Value("${aws.access-key-id:#{null}}")
    private String awsAccessKey;

    @Value("${aws.secret-access-key:#{null}}")
    private String awsSecretKey;

    @Bean
    public DynamoDbClient dynamoDbClient() {
        var builder = DynamoDbClient.builder()
            .region(Region.of(awsRegion));

        // Se credenciais fornecidas (dev/homolog), usa-as
        // Em produção, usar IAM Roles (IRSA)
        if (awsAccessKey != null && !awsAccessKey.isEmpty()
            && awsSecretKey != null && !awsSecretKey.isEmpty()) {

            builder.credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(awsAccessKey, awsSecretKey)
                )
            );
        } else {
            // Fallback para default credentials chain (IAM Role, env vars, etc)
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        return builder.build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
            .dynamoDbClient(dynamoDbClient)
            .build();
    }
}
```

#### Update Application Properties

##### ❌ REMOVER:

```properties
# PostgreSQL
spring.datasource.url=${SPRING_DATASOURCE_URL}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA
spring.jpa.hibernate.ddl-auto=none
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# Flyway
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
```

##### ✅ ADICIONAR:

```properties
# DynamoDB
aws.dynamodb.table-name=${AWS_DYNAMODB_TABLE_NAME:challengeone-billing-dev}

# AWS Credentials (opcional - fallback para IRSA)
aws.access-key-id=${AWS_ACCESS_KEY:}
aws.secret-access-key=${AWS_SECRET_KEY:}
aws.region=${AWS_REGION:us-east-2}
```

### Deployment Steps

#### 1. Deploy Tabela DynamoDB (PRIMEIRO)

```bash
cd infra-database/envs/dev
./deploy.sh
```

#### 2. Atualizar Código Java

- [ ] Atualizar pom.xml (dependências)
- [ ] Atualizar Payment entity (anotações DynamoDB)
- [ ] Criar PaymentRepository (DynamoDB)
- [ ] Criar DynamoDbConfig
- [ ] Atualizar application.properties
- [ ] Adaptar services se necessário
- [ ] Executar testes locais

#### 3. Build Nova Imagem Docker

```bash
cd billing-service

# Build
docker build -t thiagotierre/billing-service:latest .

# Push
docker push thiagotierre/billing-service:latest
```

#### 4. Deploy Kubernetes (DEPOIS)

```bash
cd terraform

# Reinicializar terraform (remote state mudou)
terraform init -reconfigure

# Aplicar mudanças
terraform apply -var-file=terraform.tfvars -var-file=secret.tfvars
```

#### 5. Validar

```bash
# Verificar pods
kubectl get pods -n challengeone-billing

# Logs
kubectl logs -n challengeone-billing -l app=billing --tail=100 -f

# Testar endpoint
kubectl port-forward -n challengeone-billing svc/billing 8080:8080
curl http://localhost:8080/actuator/health
```

### Migration Reference

| Aspecto          | PostgreSQL                          | DynamoDB                                 |
| ---------------- | ----------------------------------- | ---------------------------------------- |
| **Dependências** | spring-data-jpa, postgresql, flyway | aws-sdk dynamodb-enhanced                |
| **Entity**       | `@Entity`, `@Table`, `@Id`          | `@DynamoDbBean`, `@DynamoDbPartitionKey` |
| **Repository**   | `extends JpaRepository`             | Classe manual com DynamoDbTable          |
| **Queries**      | JPQL/SQL                            | Key-based + GSI                          |
| **Migrations**   | Flyway/Liquibase                    | Schema-less (não precisa)                |

### Important Warnings

1. **Queries Complexas**: DynamoDB não suporta JOINs nem queries SQL avançadas
2. **GSI Limits**: Máximo 20 GSIs por tabela
3. **Item Size**: Máximo 400KB por item
4. **Transactions**: Limitadas a 25 items por transação
5. **Custos**: Pay-per-request cobra por read/write (estime custos antes)

---

# Part VIII: Configuration & Operations

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

## Spring Profiles

### development (Default)

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

### homologation

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

### production

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

## Monitoring & Observability

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

---

# Part IX: Advanced Topics

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

### Security Considerations

1. **Credentials Management**:
   - AWS keys loaded from environment variables
   - Never commit `.env` file to repository
   - Rotate keys regularly

2. **Data Protection**:
   - Payment data encrypted at rest (DynamoDB)
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

## Performance Tuning

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

---

## Support & Contributions

For issues or questions:

1. Check [Troubleshooting](#troubleshooting) section
2. Review application logs: `docker-compose logs -f app`
3. Check payment status in database
4. Inspect SQS messages in queue

---

**Last Updated**: February 2026  
**Version**: 2.0  
**Status**: ✅ Complete Consolidation

This comprehensive README consolidates all 7 markdown files into a single, well-organized source of truth for the Billing Service project.
