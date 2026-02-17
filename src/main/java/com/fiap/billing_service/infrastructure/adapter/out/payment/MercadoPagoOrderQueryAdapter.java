package com.fiap.billing_service.infrastructure.adapter.out.payment;

import com.fiap.billing_service.application.port.out.PaymentOrderQueryPort;
import com.fiap.billing_service.domain.entity.Payment;
import com.fiap.billing_service.domain.exception.PaymentProcessingException;
import com.fiap.billing_service.domain.valueobject.PaymentStatus;
import com.fiap.billing_service.infrastructure.adapter.out.payment.dto.MercadoPagoOrderResponse;
import jakarta.annotation.PostConstruct;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/** Mercado Pago order query adapter for retrieving payment status from Orders API */
@Component
public class MercadoPagoOrderQueryAdapter implements PaymentOrderQueryPort {

  private static final Logger log =
      LoggerFactory.getLogger(MercadoPagoOrderQueryAdapter.class);
  private static final String ORDERS_API_URL = "https://api.mercadopago.com/v1/orders";

  @Value("${mercadopago.access-token}")
  private String accessToken;

  private final RestTemplate restTemplate;

  public MercadoPagoOrderQueryAdapter(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @PostConstruct
  public void init() {
    log.info("Mercado Pago Order Query adapter initialized");
  }

  @Override
  public Payment getOrderStatus(String orderPaymentId) {
    log.info("Querying order status from Mercado Pago: orderPaymentId={}", orderPaymentId);

    try {
      // Set up headers with Authorization
      HttpHeaders headers = new HttpHeaders();
      headers.set("Authorization", "Bearer " + accessToken);
      headers.set("Content-Type", "application/json");

      HttpEntity<?> requestEntity = new HttpEntity<>(headers);

      // Make GET API call
      String url = ORDERS_API_URL + "/" + orderPaymentId;
      log.debug("Calling Mercado Pago Orders API with GET: {}", url);

      ResponseEntity<MercadoPagoOrderResponse> responseEntity =
          restTemplate.exchange(url, HttpMethod.GET, requestEntity, MercadoPagoOrderResponse.class);

      MercadoPagoOrderResponse orderResponse = responseEntity.getBody();

      if (orderResponse == null) {
        throw new PaymentProcessingException(
            "Empty response from Mercado Pago Orders API for orderPaymentId: " + orderPaymentId);
      }

      log.info(
          "Order status retrieved from Mercado Pago: id={}, status={}",
          orderResponse.getId(),
          orderResponse.getStatus());

      return mapResponseToPayment(orderResponse);

    } catch (HttpClientErrorException.NotFound e) {
      log.error("Order not found in Mercado Pago: orderPaymentId={}", orderPaymentId, e);
      throw new PaymentProcessingException(
          "Order not found in Mercado Pago with ID: " + orderPaymentId, e);
    } catch (RestClientException e) {
      log.error("Error querying Mercado Pago Orders API: orderPaymentId={}", orderPaymentId, e);
      throw new PaymentProcessingException(
          "Failed to query order status from Mercado Pago for ID: " + orderPaymentId, e);
    } catch (Exception e) {
      log.error("Unexpected error querying Mercado Pago Orders API", e);
      throw new PaymentProcessingException(
          "Unexpected error querying Mercado Pago Orders API: " + e.getMessage(), e);
    }
  }

  /**
   * Maps Mercado Pago order response to domain Payment entity with updated status
   *
   * @param orderResponse the response from Mercado Pago API
   * @return Payment entity with updated status and payment details
   */
  private Payment mapResponseToPayment(MercadoPagoOrderResponse orderResponse) {
    String orderId = orderResponse.getId();
    String qrCode = null;
    String qrCodeBase64 = null;
    String externalPaymentId = orderId;
    String paymentStatus = orderResponse.getStatus();
    String errorMessage = null;

    // Extract payment data from first transaction
    if (orderResponse.getTransactions() != null
        && orderResponse.getTransactions().getPayments() != null
        && orderResponse.getTransactions().getPayments().length > 0) {

      MercadoPagoOrderResponse.Payment payment = orderResponse.getTransactions().getPayments()[0];
      externalPaymentId = payment.getId();
      paymentStatus = payment.getStatus();

      // Extract QR code if available
      if (payment.getPointOfInteraction() != null
          && payment.getPointOfInteraction().getTransactionData() != null) {
        qrCode = payment.getPointOfInteraction().getTransactionData().getQrCode();
        qrCodeBase64 = payment.getPointOfInteraction().getTransactionData().getQrCodeBase64();
      }

      // Extract error message if payment was rejected
      if (paymentStatus != null && paymentStatus.toLowerCase().contains("rejected")) {
        errorMessage = payment.getStatusDetail();
      }
    }

    // Map status
    PaymentStatus status = mapStatus(paymentStatus);

    // Create Payment entity with updated information
    Payment payment =
        new Payment(
            UUID.randomUUID(),
            null, // budgetId not available from API query
            null, // workOrderId not available from API query
            null, // clientId not available from API query
            null); // amount not available from API query

    // Update payment with gateway response
    payment.markAsProcessing(
        externalPaymentId, orderId, "pix", qrCode, qrCodeBase64);

    // Apply final status
    if (status == PaymentStatus.APPROVED) {
      payment.markAsApproved();
    } else if (status == PaymentStatus.REJECTED) {
      payment.markAsRejected(errorMessage);
    }

    log.debug(
        "Payment entity mapped from Mercado Pago response: status={}, externalPaymentId={}",
        status,
        externalPaymentId);

    return payment;
  }

  /**
   * Maps Mercado Pago payment status to domain PaymentStatus enum
   *
   * @param mpStatus the status from Mercado Pago API
   * @return corresponding PaymentStatus
   */
  private PaymentStatus mapStatus(String mpStatus) {
    if (mpStatus == null) {
      return PaymentStatus.PROCESSING;
    }

    return switch (mpStatus.toLowerCase()) {
      case "approved", "processed" -> PaymentStatus.APPROVED;
      case "rejected", "cancelled" -> PaymentStatus.REJECTED;
      case "pending", "processing" -> PaymentStatus.PROCESSING;
      default -> PaymentStatus.PROCESSING;
    };
  }
}
