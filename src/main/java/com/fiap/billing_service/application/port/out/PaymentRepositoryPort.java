package com.fiap.billing_service.application.port.out;

import com.fiap.billing_service.domain.entity.Payment;
import java.util.Optional;
import java.util.UUID;

/** Output port for payment repository */
public interface PaymentRepositoryPort {
  Payment save(Payment payment);

  /**
   * Find payment by work order ID
   *
   * @param workOrderId the work order ID
   * @return Optional containing the payment if found, empty otherwise
   */
  Optional<Payment> findByWorkOrderId(UUID workOrderId);
}
