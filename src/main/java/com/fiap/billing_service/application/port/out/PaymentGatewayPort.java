package com.fiap.billing_service.application.port.out;

import com.fiap.billing_service.domain.dto.PaymentResponse;
import java.math.BigDecimal;

/** Output port for payment gateway (Mercado Pago with PIX) */
public interface PaymentGatewayPort {
<<<<<<< HEAD
  PaymentResponse processPixPayment(
      BigDecimal amount, String email, String description, String firstName);
=======
  PaymentResponse processPixPayment(BigDecimal amount, String email, String description);
>>>>>>> 874da5d659f8f0227b13a5ef37e537fd54c18408
}
