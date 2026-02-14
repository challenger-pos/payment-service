package com.fiap.billing_service.application.port.out;

import com.fiap.billing_service.domain.entity.Payment;

/**
 * Output port for payment response messaging
 */
public interface PaymentResponseMessagePort {
    void sendPaymentResponse(Payment payment);
}
