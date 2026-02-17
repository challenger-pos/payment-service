package com.fiap.billing_service.domain.dto;

import com.fiap.billing_service.domain.valueobject.PaymentStatus;

/** DTO for payment processing response from payment gateway */
public class PaymentResponse {
  private final String externalPaymentId;
  private final String orderPaymentId;
  private final String paymentMethod;
  private final PaymentStatus status;
  private final String qrCode;
  private final String qrCodeBase64;
  private final String errorMessage;

  public PaymentResponse(
      String externalPaymentId,
      String orderPaymentId,
      String paymentMethod,
      PaymentStatus status,
      String qrCode,
      String qrCodeBase64,
      String errorMessage) {
    this.externalPaymentId = externalPaymentId;
    this.orderPaymentId = orderPaymentId;
    this.paymentMethod = paymentMethod;
    this.status = status;
    this.qrCode = qrCode;
    this.qrCodeBase64 = qrCodeBase64;
    this.errorMessage = errorMessage;
  }

  public String getExternalPaymentId() {
    return externalPaymentId;
  }

  public String getPaymentMethod() {
    return paymentMethod;
  }

  public PaymentStatus getStatus() {
    return status;
  }

  public String getQrCode() {
    return qrCode;
  }

  public String getQrCodeBase64() {
    return qrCodeBase64;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public String getOrderPaymentId() {
    return orderPaymentId;
  }
}
