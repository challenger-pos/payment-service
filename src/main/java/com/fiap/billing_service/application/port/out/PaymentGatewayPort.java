package com.fiap.billing_service.application.port.out;

import com.fiap.billing_service.domain.dto.PaymentResponse;

import java.math.BigDecimal;

/**
 * Output port for payment gateway (Mercado Pago with PIX)
 */
public interface PaymentGatewayPort {
    PaymentResponse processPixPayment(BigDecimal amount, String documentNumber, String email, String name, String description);
}
