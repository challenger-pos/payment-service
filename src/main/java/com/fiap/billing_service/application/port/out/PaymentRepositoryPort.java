package com.fiap.billing_service.application.port.out;

import com.fiap.billing_service.domain.entity.Payment;

import java.util.Optional;
import java.util.UUID;

/**
 * Output port for payment repository
 */
public interface PaymentRepositoryPort {
    Payment save(Payment payment);
    Optional<Payment> findById(UUID paymentId);
    Optional<Payment> findByWorkOrderId(UUID workOrderId);
}
