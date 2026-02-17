package com.fiap.billing_service.application.port.out;

import com.fiap.billing_service.domain.entity.Payment;

/** Output port for querying payment order status from external payment gateway */
public interface PaymentOrderQueryPort {

  /**
   * Query the current status of an order from the payment gateway
   *
   * @param orderPaymentId the external order payment ID from the gateway
   * @return Payment entity with updated status and details
   * @throws com.fiap.billing_service.domain.exception.PaymentProcessingException if query fails
   */
  Payment getOrderStatus(String orderPaymentId);
}
