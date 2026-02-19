package com.fiap.billing_service.application.service;

import com.fiap.billing_service.application.port.in.ProcessPaymentUseCase;
import com.fiap.billing_service.application.port.out.PaymentGatewayPort;
import com.fiap.billing_service.application.port.out.PaymentOrderQueryPort;
import com.fiap.billing_service.application.port.out.PaymentRepositoryPort;
import com.fiap.billing_service.application.port.out.PaymentResponseMessagePort;
import com.fiap.billing_service.domain.entity.Payment;
import com.fiap.billing_service.domain.exception.PaymentProcessingException;
import com.fiap.billing_service.domain.valueobject.PaymentStatus;
import com.fiap.billing_service.infrastructure.adapter.in.messaging.dto.PaymentRequestDto;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

@Service
public class ProcessPaymentService implements ProcessPaymentUseCase {

  private static final Logger log = LoggerFactory.getLogger(ProcessPaymentService.class);

  private final PaymentRepositoryPort paymentRepository;
  private final PaymentGatewayPort paymentGateway;
  private final PaymentOrderQueryPort paymentOrderQuery;
  private final PaymentResponseMessagePort paymentResponseMessage;

  public ProcessPaymentService(
      PaymentRepositoryPort paymentRepository,
      PaymentGatewayPort paymentGateway,
      PaymentOrderQueryPort paymentOrderQuery,
      PaymentResponseMessagePort paymentResponseMessage) {
    this.paymentRepository = paymentRepository;
    this.paymentGateway = paymentGateway;
    this.paymentOrderQuery = paymentOrderQuery;
    this.paymentResponseMessage = paymentResponseMessage;
  }

  @Override
  public Payment processPayment(PaymentRequestDto paymentRequest) {
    UUID workOrderId = paymentRequest.getWorkOrderId();

    // Check for existing payment (idempotency - first line of defense)
    var existingPayment = paymentRepository.findByWorkOrderId(workOrderId);
    if (existingPayment.isPresent()) {
      Payment payment = existingPayment.get();
      log.warn(
          "Duplicate payment request detected for workOrderId: {}, status: {}, paymentId: {}",
          workOrderId,
          payment.getStatus(),
          payment.getId());

      // If payment is still pending, it might be a legitimate retry after a crash
      // Otherwise, return existing payment (idempotency)
      if (payment.getStatus() != PaymentStatus.PENDING) {
        log.info("Returning existing payment due to idempotency: {}", payment.getId());
        return payment;
      }
      log.info("Payment is PENDING, continuing processing for workOrderId: {}", workOrderId);
    }

    // Create payment entity
    var payment =
        new Payment(
            UUID.randomUUID(),
            workOrderId,
            paymentRequest.getCustomerId(),
            paymentRequest.getAmount());
    log.info("Created payment entity: {}", payment);

    // Save initial payment (DynamoDB constraint validation - first persistence check)
    try {
      payment = paymentRepository.save(payment);
      log.info(
          "Payment created successfully: paymentId={}, workOrderId={}",
          payment.getId(),
          workOrderId);
    } catch (DynamoDbException e) {
      // Race condition or constraint violation
      log.info(
          "Concurrent duplicate or constraint violation detected for workOrderId: {}. Fetching existing payment.",
          workOrderId);
      return paymentRepository
          .findByWorkOrderId(workOrderId)
          .orElseThrow(
              () ->
                  new PaymentProcessingException(
                      "Payment saving failed but payment not found for workOrderId: "
                          + workOrderId));
    }

    try {
      // Process payment through Mercado Pago (PIX)
      var processedPayment =
          paymentGateway.processPixPayment(
              paymentRequest.getAmount(),
              null,
              paymentRequest.getDescription() != null
                  ? paymentRequest.getDescription()
                  : "Payment for order " + paymentRequest.getWorkOrderId(),
              paymentRequest.getFirstName());

      // Update payment with gateway response
      payment.markAsProcessing(
          processedPayment.getExternalPaymentId(),
          processedPayment.getOrderPaymentId(),
          processedPayment.getPaymentMethod(),
          processedPayment.getQrCode(),
          processedPayment.getQrCodeBase64());

      // Query payment order status to get most up-to-date information
      try {
        log.info("Querying order status from Mercado Pago for payment: {}", payment.getId());
        Payment queryResult =
            paymentOrderQuery.getOrderStatus(processedPayment.getOrderPaymentId());

        log.info(
            "status query result for payment {}: {}", payment.getId(), queryResult.getStatus());

        // Update payment with queried status
        if (queryResult.getStatus() == PaymentStatus.APPROVED) {
          payment.markAsApproved();
          log.info("Payment approved after status query: {}", payment.getId());
        } else if (queryResult.getStatus() == PaymentStatus.REJECTED) {
          payment.markAsRejected(queryResult.getErrorMessage());
          log.info("Payment rejected after status query: {}", payment.getId());
        }
        // If status is PROCESSING, keep current state
      } catch (Exception e) {
        log.warn("Failed to query payment status, using initial response status", e);
        // Fallback to initial gateway response if query fails
        if (processedPayment.getStatus() != PaymentStatus.APPROVED) {
          payment.markAsRejected(processedPayment.getErrorMessage());
        } else {
          payment.markAsApproved();
        }
      }

      // Save updated payment
      payment = paymentRepository.save(payment);

      // Send payment response to message queue
      paymentResponseMessage.sendPaymentResponse(payment);

      return payment;

    } catch (Exception e) {
      payment.markAsFailed(e.getMessage());
      paymentRepository.save(payment);
      throw new PaymentProcessingException(
          "Failed to process payment for order " + paymentRequest.getWorkOrderId(), e);
    }
  }
}
