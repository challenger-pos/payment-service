package com.fiap.billing_service.domain.entity;

import com.fiap.billing_service.domain.valueobject.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class Payment {
  private final UUID id;
  private final UUID workOrderId;
  private final UUID customerId;
  private final BigDecimal amount;
  private PaymentStatus status;
  private String externalPaymentId; // Mercado Pago payment ID
  private String orderPaymentId; // Internal order payment ID
  private String paymentMethod;
  private String qrCode;
  private String qrCodeBase64;
  private final LocalDateTime createdAt;
  private LocalDateTime processedAt;
  private String errorMessage;

  public Payment(UUID id, UUID workOrderId, UUID customerId, BigDecimal amount) {
    this.id = id;
    this.workOrderId = workOrderId;
    this.customerId = customerId;
    this.amount = amount;
    this.status = PaymentStatus.PENDING;
    this.createdAt = LocalDateTime.now();
  }

  public UUID getId() {
    return id;
  }

  public UUID getWorkOrderId() {
    return workOrderId;
  }

  public UUID getCustomerId() {
    return customerId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public PaymentStatus getStatus() {
    return status;
  }

  public String getExternalPaymentId() {
    return externalPaymentId;
  }

  public String getOrderPaymentId() {
    return orderPaymentId;
  }

  public String getPaymentMethod() {
    return paymentMethod;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getProcessedAt() {
    return processedAt;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public String getQrCode() {
    return qrCode;
  }

  public String getQrCodeBase64() {
    return qrCodeBase64;
  }

  public void markAsProcessing(
      String externalPaymentId,
      String orderPaymentId,
      String paymentMethod,
      String qrCode,
      String qrCodeBase64) {
    this.status = PaymentStatus.PROCESSING;
    this.externalPaymentId = externalPaymentId;
    this.orderPaymentId = orderPaymentId;
    this.paymentMethod = paymentMethod;
    this.qrCode = qrCode;
    this.qrCodeBase64 = qrCodeBase64;
  }

  public void markAsApproved() {
    this.status = PaymentStatus.APPROVED;
    this.processedAt = LocalDateTime.now();
  }

  public void markAsRejected(String errorMessage) {
    this.status = PaymentStatus.REJECTED;
    this.errorMessage = errorMessage;
    this.processedAt = LocalDateTime.now();
  }

  public void markAsFailed(String errorMessage) {
    this.status = PaymentStatus.FAILED;
    this.errorMessage = errorMessage;
    this.processedAt = LocalDateTime.now();
  }
}
