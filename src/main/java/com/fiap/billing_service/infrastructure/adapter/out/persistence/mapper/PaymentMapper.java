package com.fiap.billing_service.infrastructure.adapter.out.persistence.mapper;

import com.fiap.billing_service.domain.entity.Payment;
import com.fiap.billing_service.domain.valueobject.PaymentStatus;
import com.fiap.billing_service.infrastructure.adapter.out.persistence.entity.PaymentEntity;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

  public PaymentEntity toEntity(Payment payment) {
    PaymentEntity entity = new PaymentEntity();
    entity.setId(payment.getId());
    entity.setWorkOrderId(payment.getWorkOrderId());
    entity.setCustomerId(payment.getCustomerId());
    entity.setAmount(payment.getAmount());
    entity.setStatus(payment.getStatus().name());
    entity.setExternalPaymentId(payment.getExternalPaymentId());
    entity.setOrderPaymentId(payment.getOrderPaymentId());
    entity.setPaymentMethod(payment.getPaymentMethod());
    entity.setQrCode(payment.getQrCode());
    entity.setQrCodeBase64(payment.getQrCodeBase64());

    // Ensure createdAt is set (required for DynamoDB sort key)
    if (payment.getCreatedAt() != null) {
      entity.setCreatedAt(payment.getCreatedAt());
    } else {
      // Default to now if not set
      entity.setCreatedAt(java.time.LocalDateTime.now());
    }

    entity.setProcessedAt(payment.getProcessedAt());
    entity.setErrorMessage(payment.getErrorMessage());
    return entity;
  }

  public Payment toDomain(PaymentEntity entity) {
    Payment payment =
        new Payment(
            entity.getId(), entity.getWorkOrderId(), entity.getCustomerId(), entity.getAmount());

    // Restore payment status and other fields
    if (entity.getStatus() != null) {
      PaymentStatus status = PaymentStatus.valueOf(entity.getStatus());
      if (status == PaymentStatus.APPROVED) {
        payment.markAsProcessing(
            entity.getExternalPaymentId(),
            entity.getOrderPaymentId(),
            entity.getPaymentMethod(),
            entity.getQrCode(),
            entity.getQrCodeBase64());
        payment.markAsApproved();
      } else if (status == PaymentStatus.REJECTED) {
        payment.markAsRejected(entity.getErrorMessage());
      } else if (status == PaymentStatus.FAILED) {
        payment.markAsFailed(entity.getErrorMessage());
      } else if (status == PaymentStatus.PROCESSING) {
        payment.markAsProcessing(
            entity.getExternalPaymentId(),
            entity.getOrderPaymentId(),
            entity.getPaymentMethod(),
            entity.getQrCode(),
            entity.getQrCodeBase64());
      }
    }

    return payment;
  }
}
