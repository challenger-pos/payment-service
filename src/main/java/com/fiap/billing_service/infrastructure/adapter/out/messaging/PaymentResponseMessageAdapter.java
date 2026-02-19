package com.fiap.billing_service.infrastructure.adapter.out.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.billing_service.application.port.out.PaymentResponseMessagePort;
import com.fiap.billing_service.domain.entity.Payment;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

/** Messaging adapter for sending payment responses to SQS */
@Component
public class PaymentResponseMessageAdapter implements PaymentResponseMessagePort {

  private static final Logger log = LoggerFactory.getLogger(PaymentResponseMessageAdapter.class);

  private final SqsClient sqsClient;
  private final ObjectMapper objectMapper;

  @Value("${aws.sqs.payment-response-success-queue}")
  private String successQueueName;

  @Value("${aws.sqs.payment-response-failure-queue}")
  private String failureQueueName;

  public PaymentResponseMessageAdapter(SqsClient sqsClient, ObjectMapper objectMapper) {
    this.sqsClient = sqsClient;
    this.objectMapper = objectMapper;
  }

  @Override
  public void sendPaymentResponse(Payment payment) {
    try {
      // Determine which queue to use based on payment status
      String queueName =
          isSuccessStatus(payment.getStatus().name()) ? successQueueName : failureQueueName;

      log.info(
          "Sending payment response to queue: {} for paymentId={}, status={}",
          queueName,
          payment.getId(),
          payment.getStatus());

      // Get queue URL
      GetQueueUrlRequest getQueueUrlRequest =
          GetQueueUrlRequest.builder().queueName(queueName).build();

      String queueUrl = sqsClient.getQueueUrl(getQueueUrlRequest).queueUrl();

      // Build message structure
      Map<String, Object> message = new HashMap<>();
      message.put("workOrderId", payment.getWorkOrderId().toString());

      // Convert message to JSON
      String messageJson = objectMapper.writeValueAsString(message);

      // Send message to SQS
      SendMessageRequest sendMessageRequest =
          SendMessageRequest.builder().queueUrl(queueUrl).messageBody(messageJson).build();

      var result = sqsClient.sendMessage(sendMessageRequest);

      log.info(
          "Payment response sent successfully to queue: {} with MessageId: {}",
          queueName,
          result.messageId());

    } catch (Exception e) {
      log.error("Error sending payment response to SQS for paymentId={}", payment.getId(), e);
      // Don't throw exception to avoid breaking the payment flow
      // In production, you might want to implement retry logic or dead letter queue
    }
  }

  private boolean isSuccessStatus(String status) {
    return "APPROVED".equals(status) || "PROCESSING".equals(status);
  }
}
