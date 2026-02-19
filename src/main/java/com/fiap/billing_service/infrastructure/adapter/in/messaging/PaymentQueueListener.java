package com.fiap.billing_service.infrastructure.adapter.in.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.billing_service.application.port.in.ProcessPaymentUseCase;
import com.fiap.billing_service.infrastructure.adapter.in.messaging.dto.PaymentRequestDto;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Adapter for listening to payment requests from SQS queue */
@Component
public class PaymentQueueListener {

  private static final Logger log = LoggerFactory.getLogger(PaymentQueueListener.class);

  private final ProcessPaymentUseCase processPaymentUseCase;
  private final ObjectMapper objectMapper;

  public PaymentQueueListener(
      ProcessPaymentUseCase processPaymentUseCase, ObjectMapper objectMapper) {
    this.processPaymentUseCase = processPaymentUseCase;
    this.objectMapper = objectMapper;
  }

  /**
   * Listens to payment requests from the queue
   *
   * <p>With ON_SUCCESS acknowledgment mode: - Success: Message is removed from queue - Exception:
   * Message returns to queue (becomes visible after timeout) - Idempotency: Duplicate messages are
   * handled safely by ProcessPaymentService
   *
   * @param message JSON message containing payment request data
   */
  @SqsListener("${aws.sqs.payment-request-queue}")
  public void receivePaymentRequest(String message) {
    log.info("Received payment request message from queue");

    try {
      // Parse the message
      PaymentRequestDto paymentRequest = objectMapper.readValue(message, PaymentRequestDto.class);
      log.info("Parsed payment request: {}", paymentRequest);

      log.info(
          "Processing payment request - workOrderId: {}, clientId: {}",
          paymentRequest.getWorkOrderId(),
          paymentRequest.getCustomerId());

      // Process the payment (idempotency handled by ProcessPaymentService)
      processPaymentUseCase.processPayment(paymentRequest);

      log.info(
          "Payment processed successfully for workOrderId: {}", paymentRequest.getWorkOrderId());

    } catch (Exception e) {
      log.error("Error processing payment request message: {}", message, e);
      // Re-throw to prevent message acknowledgment (with ON_SUCCESS mode)
      // Message will return to queue after visibility timeout
      // After maxReceiveCount attempts, it will go to DLQ
      throw new RuntimeException("Failed to process payment request", e);
    }
  }
}
