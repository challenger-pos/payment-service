package com.fiap.billing_service.infrastructure.adapter.out.payment;

import com.fiap.billing_service.application.port.out.PaymentGatewayPort;
import com.fiap.billing_service.domain.dto.PaymentResponse;
import com.fiap.billing_service.domain.valueobject.PaymentStatus;
import com.fiap.billing_service.infrastructure.adapter.out.payment.dto.MercadoPagoOrderRequest;
import com.fiap.billing_service.infrastructure.adapter.out.payment.dto.MercadoPagoOrderResponse;
import io.opentracing.Span;
import io.opentracing.util.GlobalTracer;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/** Mercado Pago payment gateway adapter using Orders API with PIX */
@Component
public class MercadoPagoAdapter implements PaymentGatewayPort {

  private static final Logger log = LoggerFactory.getLogger(MercadoPagoAdapter.class);
  private static final String ORDERS_API_URL = "https://api.mercadopago.com/v1/orders";

  @Value("${mercadopago.access-token}")
  private String accessToken;

  private final RestTemplate restTemplate;

  public MercadoPagoAdapter(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }
<<<<<<< HEAD

  @PostConstruct
  public void init() {
    log.info("Mercado Pago adapter initialized with Orders API");
  }

  @Override
  public PaymentResponse processPixPayment(BigDecimal amount, String email, String description, String firstName) {
    Span span = GlobalTracer.get().activeSpan();
    if (span != null) {
      span.setTag("operation.type", "processPixPayment");
      span.setTag("payment.amount", amount != null ? amount.toString() : "0");
      span.setTag("payment.provider", "mercadopago");
    }

    log.info(
        "Processing PIX payment through Mercado Pago Orders API: amount={}, email={}",
        amount,
        email);

    try {
      // Generate unique reference ID
      String externalReference = "order_ref_" + UUID.randomUUID();

      // Use provided email or default
      String payerEmail = email != null && !email.isEmpty() ? email : "test@testuser.com";

      // Create order request
      MercadoPagoOrderRequest orderRequest =
          new MercadoPagoOrderRequest(externalReference, amount, payerEmail, firstName);
      log.info("Created Mercado Pago order request: {}", orderRequest);

      // Set up headers with Authorization and X-Idempotency-Key
      HttpHeaders headers = new HttpHeaders();
      headers.set("Authorization", "Bearer " + accessToken);
      headers.set("X-Idempotency-Key", UUID.randomUUID().toString());
      headers.set("Content-Type", "application/json");

      HttpEntity<MercadoPagoOrderRequest> requestEntity = new HttpEntity<>(orderRequest, headers);

      // Make API call
      log.info("Calling Mercado Pago Orders API with idempotency key");
      ResponseEntity<MercadoPagoOrderResponse> responseEntity =
          restTemplate.exchange(
              ORDERS_API_URL, HttpMethod.POST, requestEntity, MercadoPagoOrderResponse.class);

=======

  @PostConstruct
  public void init() {
    log.info("Mercado Pago adapter initialized with Orders API");
  }

  @Override
  public PaymentResponse processPixPayment(BigDecimal amount, String email, String description) {
    log.info(
        "Processing PIX payment through Mercado Pago Orders API: amount={}, email={}",
        amount,
        email);

    try {
      // Generate unique reference ID
      String externalReference = "order_ref_" + UUID.randomUUID();

      // Use provided email or default
      String payerEmail = email != null && !email.isEmpty() ? email : "test@testuser.com";

      // Create order request
      MercadoPagoOrderRequest orderRequest =
          new MercadoPagoOrderRequest(externalReference, amount, payerEmail);

      // Set up headers with Authorization and X-Idempotency-Key
      HttpHeaders headers = new HttpHeaders();
      headers.set("Authorization", "Bearer " + accessToken);
      headers.set("X-Idempotency-Key", UUID.randomUUID().toString());
      headers.set("Content-Type", "application/json");

      HttpEntity<MercadoPagoOrderRequest> requestEntity = new HttpEntity<>(orderRequest, headers);

      // Make API call
      log.info("Calling Mercado Pago Orders API with idempotency key");
      ResponseEntity<MercadoPagoOrderResponse> responseEntity =
          restTemplate.exchange(
              ORDERS_API_URL, HttpMethod.POST, requestEntity, MercadoPagoOrderResponse.class);

>>>>>>> 874da5d659f8f0227b13a5ef37e537fd54c18408
      MercadoPagoOrderResponse orderResponse = responseEntity.getBody();

      if (orderResponse == null) {
        throw new RuntimeException("Empty response from Mercado Pago Orders API");
      }

      log.info(
          "Mercado Pago order created: id={}, status={}",
          orderResponse.getId(),
          orderResponse.getStatus());

      // Extract payment data
      String orderId = orderResponse.getId();
      String qrCode = null;
      String qrCodeBase64 = null;
      String paymentId = orderId;
      String paymentStatus = orderResponse.getStatus();

      // Extract QR code from first payment transaction
      if (orderResponse.getTransactions() != null
          && orderResponse.getTransactions().getPayments() != null
          && orderResponse.getTransactions().getPayments().length > 0) {

        MercadoPagoOrderResponse.Payment payment = orderResponse.getTransactions().getPayments()[0];
        paymentId = payment.getId();
        paymentStatus = payment.getStatus();

        if (payment.getPointOfInteraction() != null
            && payment.getPointOfInteraction().getTransactionData() != null) {
          qrCode = payment.getPointOfInteraction().getTransactionData().getQrCode();
          qrCodeBase64 = payment.getPointOfInteraction().getTransactionData().getQrCodeBase64();
        }
      }

      // Determine payment status
      PaymentStatus status = mapStatus(paymentStatus);
      String errorMessage = null;

      if (status == PaymentStatus.REJECTED
          && orderResponse.getTransactions() != null
          && orderResponse.getTransactions().getPayments() != null
          && orderResponse.getTransactions().getPayments().length > 0) {
        errorMessage = orderResponse.getTransactions().getPayments()[0].getStatusDetail();
      }

<<<<<<< HEAD
      if (span != null) {
        span.setTag("payment.order_id", orderId);
        span.setTag("payment.payment_id", paymentId);
        span.setTag("payment.status", status.name());
      }

=======
>>>>>>> 874da5d659f8f0227b13a5ef37e537fd54c18408
      log.info(
          "PIX payment processed: orderId={}, paymentId={}, status={}", orderId, paymentId, status);

      return new PaymentResponse(
          paymentId, orderResponse.getId(), "pix", status, qrCode, qrCodeBase64, errorMessage);

    } catch (Exception e) {
      log.error("Error processing payment through Mercado Pago Orders API", e);
      throw new RuntimeException("Mercado Pago API error: " + e.getMessage(), e);
    }
  }

  private PaymentStatus mapStatus(String mpStatus) {
    if (mpStatus == null) {
<<<<<<< HEAD
      return PaymentStatus.REJECTED;
    }

    return switch (mpStatus.toLowerCase()) {
      case "approved", "processed", "accredited" -> PaymentStatus.APPROVED;
      case "waiting_transfer", "cancelled" -> PaymentStatus.REJECTED;
      case "pending", "processing" -> PaymentStatus.PROCESSING;
      default -> PaymentStatus.REJECTED;
=======
      return PaymentStatus.PROCESSING;
    }

    return switch (mpStatus.toLowerCase()) {
      case "approved", "processed" -> PaymentStatus.APPROVED;
      case "rejected", "cancelled" -> PaymentStatus.REJECTED;
      case "pending", "processing" -> PaymentStatus.PROCESSING;
      default -> PaymentStatus.PROCESSING;
>>>>>>> 874da5d659f8f0227b13a5ef37e537fd54c18408
    };
  }
}
