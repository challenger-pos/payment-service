package com.fiap.billing_service.infrastructure.adapter.out.persistence.entity;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * PaymentEntity - Represents a payment record in DynamoDB.
 * 
 * Partition Key: workOrderId (provides uniqueness and distribution across partitions)
 * Sort Key: createdAtEpoch (timestamp for ordering and range queries)
 */
@DynamoDbBean
public class PaymentEntity {

  // Unique identifier for the payment
  private UUID id;

  // Budget reference (logical reference to another service)
  private UUID budgetId;

  // Partition Key: Work order ID (ensures uniqueness and enables efficient queries)
  private UUID workOrderId;

  // Sort Key: Creation timestamp (enables range queries and temporal ordering)
  private Long createdAtEpoch; // Stored as epoch milliseconds for DynamoDB compatibility

  // Client reference (logical reference to another service)
  private UUID clientId;

  // Payment amount in currency (e.g., BRL)
  private BigDecimal amount;

  // Payment status: PENDING, PROCESSING, APPROVED, REJECTED, FAILED
  private String status;

  // Mercado Pago external payment ID (for lookups via GSI)
  private String externalPaymentId;

  // Internal payment order ID
  private String orderPaymentId;

  // Payment method (default: pix)
  private String paymentMethod = "pix";

  // QR code string representation
  private String qrCode;

  // QR code in Base64 format (for direct rendering)
  private String qrCodeBase64;

  // Timestamp when payment was processed
  private Long processedAtEpoch; // Stored as epoch milliseconds

  // Error message if payment failed
  private String errorMessage;

  // Transient fields for convenience (not stored in DynamoDB)
  private LocalDateTime createdAt;
  private LocalDateTime processedAt;

  public PaymentEntity() {}

  // Getters and Setters

  @DynamoDbAttribute("id")
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  @DynamoDbAttribute("budgetId")
  public UUID getBudgetId() {
    return budgetId;
  }

  public void setBudgetId(UUID budgetId) {
    this.budgetId = budgetId;
  }

  @DynamoDbPartitionKey
  @DynamoDbAttribute("workOrderId")
  public UUID getWorkOrderId() {
    return workOrderId;
  }

  public void setWorkOrderId(UUID workOrderId) {
    this.workOrderId = workOrderId;
  }

  @DynamoDbAttribute("createdAtEpoch")
  public Long getCreatedAtEpoch() {
    return createdAtEpoch;
  }

  public void setCreatedAtEpoch(Long createdAtEpoch) {
    this.createdAtEpoch = createdAtEpoch;
    // Auto-convert from epoch milliseconds for convenience
    if (createdAtEpoch != null) {
      this.createdAt = java.time.LocalDateTime.ofInstant(
          java.time.Instant.ofEpochMilli(createdAtEpoch),
          java.time.ZoneId.systemDefault()
      );
    }
  }

  @DynamoDbAttribute("clientId")
  public UUID getClientId() {
    return clientId;
  }

  public void setClientId(UUID clientId) {
    this.clientId = clientId;
  }

  @DynamoDbAttribute("amount")
  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  @DynamoDbAttribute("status")
  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  @DynamoDbAttribute("externalPaymentId")
  public String getExternalPaymentId() {
    return externalPaymentId;
  }

  public void setExternalPaymentId(String externalPaymentId) {
    this.externalPaymentId = externalPaymentId;
  }

  @DynamoDbAttribute("orderPaymentId")
  public String getOrderPaymentId() {
    return orderPaymentId;
  }

  public void setOrderPaymentId(String orderPaymentId) {
    this.orderPaymentId = orderPaymentId;
  }

  @DynamoDbAttribute("paymentMethod")
  public String getPaymentMethod() {
    return paymentMethod;
  }

  public void setPaymentMethod(String paymentMethod) {
    this.paymentMethod = paymentMethod;
  }

  @DynamoDbAttribute("qrCode")
  public String getQrCode() {
    return qrCode;
  }

  public void setQrCode(String qrCode) {
    this.qrCode = qrCode;
  }

  @DynamoDbAttribute("qrCodeBase64")
  public String getQrCodeBase64() {
    return qrCodeBase64;
  }

  public void setQrCodeBase64(String qrCodeBase64) {
    this.qrCodeBase64 = qrCodeBase64;
  }

  @DynamoDbAttribute("processedAtEpoch")
  public Long getProcessedAtEpoch() {
    return processedAtEpoch;
  }

  public void setProcessedAtEpoch(Long processedAtEpoch) {
    this.processedAtEpoch = processedAtEpoch;
    // Auto-convert from epoch milliseconds for convenience
    if (processedAtEpoch != null) {
      this.processedAt = java.time.LocalDateTime.ofInstant(
          java.time.Instant.ofEpochMilli(processedAtEpoch),
          java.time.ZoneId.systemDefault()
      );
    }
  }

  @DynamoDbAttribute("errorMessage")
  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
    // Auto-convert to epoch milliseconds for DynamoDB storage
    if (createdAt != null) {
      this.createdAtEpoch = java.time.Instant.from(
          createdAt.atZone(java.time.ZoneId.systemDefault())
      ).toEpochMilli();
    }
  }

  public LocalDateTime getProcessedAt() {
    return processedAt;
  }

  public void setProcessedAt(LocalDateTime processedAt) {
    this.processedAt = processedAt;
    // Auto-convert to epoch milliseconds for DynamoDB storage
    if (processedAt != null) {
      this.processedAtEpoch = java.time.Instant.from(
          processedAt.atZone(java.time.ZoneId.systemDefault())
      ).toEpochMilli();
    }
  }
}
