package com.fiap.billing_service.application.port.in;

import com.fiap.billing_service.domain.entity.Payment;
import com.fiap.billing_service.infrastructure.adapter.in.messaging.dto.PaymentRequestDto;

/**
 * Input port for payment processing use case
 */
public interface ProcessPaymentUseCase {
    Payment processPayment(PaymentRequestDto paymentRequest);
}
