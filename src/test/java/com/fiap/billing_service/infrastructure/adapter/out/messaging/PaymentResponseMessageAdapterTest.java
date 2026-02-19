package com.fiap.billing_service.infrastructure.adapter.out.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.billing_service.domain.entity.Payment;
import com.fiap.billing_service.domain.valueobject.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentResponseMessageAdapter Tests")
class PaymentResponseMessageAdapterTest {

  private static final String SUCCESS_QUEUE_NAME = "payment-response-success";
  private static final String FAILURE_QUEUE_NAME = "payment-response-failure";
  private static final String SUCCESS_QUEUE_URL =
      "https://sqs.us-east-1.amazonaws.com/123456789/payment-response-success";
  private static final String FAILURE_QUEUE_URL =
      "https://sqs.us-east-1.amazonaws.com/123456789/payment-response-failure";

  @Mock private SqsClient sqsClient;

  @Mock private ObjectMapper objectMapper;

  private PaymentResponseMessageAdapter adapter;

  @BeforeEach
  void setUp() {
    // Create adapter with test queue names via reflection or constructor
    adapter = new PaymentResponseMessageAdapter(sqsClient, objectMapper);
    // Set queue names via reflection since they use @Value annotation
    setFieldValue(adapter, "successQueueName", SUCCESS_QUEUE_NAME);
    setFieldValue(adapter, "failureQueueName", FAILURE_QUEUE_NAME);
  }

  @Test
  @DisplayName("Should send approved payment to success queue")
  void testSendPaymentResponse_ApprovedStatus_UsesSuccessQueue() throws Exception {
    // Arrange
    Payment payment = createPaymentWithStatus(PaymentStatus.APPROVED);
    String messageJson = "{\"paymentId\":\"" + payment.getId().toString() + "\"}";

    GetQueueUrlResponse queueUrlResponse =
        GetQueueUrlResponse.builder().queueUrl(SUCCESS_QUEUE_URL).build();
    SendMessageResponse sendMessageResponse =
        SendMessageResponse.builder().messageId("msg-123").build();

    // Setup mocks - use lenient for AWS SDK v2 overloads
    lenient()
        .when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
        .thenReturn(queueUrlResponse);
<<<<<<< HEAD
    lenient().when(objectMapper.writeValueAsString(any())).thenReturn(messageJson);
=======
    lenient()
        .when(objectMapper.writeValueAsString(any()))
        .thenReturn(messageJson);
>>>>>>> 874da5d659f8f0227b13a5ef37e537fd54c18408
    lenient()
        .when(sqsClient.sendMessage(any(SendMessageRequest.class)))
        .thenReturn(sendMessageResponse);

    // Act
    adapter.sendPaymentResponse(payment);

    // Assert
    verify(sqsClient, times(1)).getQueueUrl(any(GetQueueUrlRequest.class));
    verify(objectMapper, times(1)).writeValueAsString(any());
    verify(sqsClient, times(1)).sendMessage(any(SendMessageRequest.class));
  }

  @Test
  @DisplayName("Should send processing payment to success queue")
  void testSendPaymentResponse_ProcessingStatus_UsesSuccessQueue() throws Exception {
    // Arrange
    Payment payment = createPaymentWithStatus(PaymentStatus.PROCESSING);
    String messageJson = "{\"paymentId\":\"" + payment.getId().toString() + "\"}";

    GetQueueUrlResponse queueUrlResponse =
        GetQueueUrlResponse.builder().queueUrl(SUCCESS_QUEUE_URL).build();
    SendMessageResponse sendMessageResponse =
        SendMessageResponse.builder().messageId("msg-124").build();

    lenient()
        .when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
        .thenReturn(queueUrlResponse);
<<<<<<< HEAD
    lenient().when(objectMapper.writeValueAsString(any())).thenReturn(messageJson);
=======
    lenient()
        .when(objectMapper.writeValueAsString(any()))
        .thenReturn(messageJson);
>>>>>>> 874da5d659f8f0227b13a5ef37e537fd54c18408
    lenient()
        .when(sqsClient.sendMessage(any(SendMessageRequest.class)))
        .thenReturn(sendMessageResponse);

    // Act
    adapter.sendPaymentResponse(payment);

    // Assert
    verify(sqsClient, times(1)).getQueueUrl(any(GetQueueUrlRequest.class));
    verify(sqsClient, times(1)).sendMessage(any(SendMessageRequest.class));
  }

  @Test
  @DisplayName("Should send rejected payment to failure queue")
  void testSendPaymentResponse_RejectedStatus_UsesFailureQueue() throws Exception {
    // Arrange
    Payment payment = createPaymentWithStatus(PaymentStatus.REJECTED);
    String messageJson = "{\"paymentId\":\"" + payment.getId().toString() + "\"}";

    GetQueueUrlResponse queueUrlResponse =
        GetQueueUrlResponse.builder().queueUrl(FAILURE_QUEUE_URL).build();
    SendMessageResponse sendMessageResponse =
        SendMessageResponse.builder().messageId("msg-125").build();

    lenient()
        .when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
        .thenReturn(queueUrlResponse);
<<<<<<< HEAD
    lenient().when(objectMapper.writeValueAsString(any())).thenReturn(messageJson);
=======
    lenient()
        .when(objectMapper.writeValueAsString(any()))
        .thenReturn(messageJson);
>>>>>>> 874da5d659f8f0227b13a5ef37e537fd54c18408
    lenient()
        .when(sqsClient.sendMessage(any(SendMessageRequest.class)))
        .thenReturn(sendMessageResponse);

    // Act
    adapter.sendPaymentResponse(payment);

    // Assert
    verify(sqsClient, times(1)).getQueueUrl(any(GetQueueUrlRequest.class));
    verify(sqsClient, times(1)).sendMessage(any(SendMessageRequest.class));
  }

  @Test
  @DisplayName("Should send failed payment to failure queue")
  void testSendPaymentResponse_FailedStatus_UsesFailureQueue() throws Exception {
    // Arrange
    Payment payment = createPaymentWithStatus(PaymentStatus.FAILED);
    String messageJson = "{\"paymentId\":\"" + payment.getId().toString() + "\"}";

    GetQueueUrlResponse queueUrlResponse =
        GetQueueUrlResponse.builder().queueUrl(FAILURE_QUEUE_URL).build();
    SendMessageResponse sendMessageResponse =
        SendMessageResponse.builder().messageId("msg-126").build();

    lenient()
        .when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
        .thenReturn(queueUrlResponse);
<<<<<<< HEAD
    lenient().when(objectMapper.writeValueAsString(any())).thenReturn(messageJson);
=======
    lenient()
        .when(objectMapper.writeValueAsString(any()))
        .thenReturn(messageJson);
>>>>>>> 874da5d659f8f0227b13a5ef37e537fd54c18408
    lenient()
        .when(sqsClient.sendMessage(any(SendMessageRequest.class)))
        .thenReturn(sendMessageResponse);

    // Act
    adapter.sendPaymentResponse(payment);

    // Assert
    verify(sqsClient, times(1)).getQueueUrl(any(GetQueueUrlRequest.class));
    verify(sqsClient, times(1)).sendMessage(any(SendMessageRequest.class));
  }

  @Test
  @DisplayName("Should send pending payment to failure queue")
  void testSendPaymentResponse_PendingStatus_UsesFailureQueue() throws Exception {
    // Arrange
    Payment payment = createPaymentWithStatus(PaymentStatus.PENDING);
    String messageJson = "{\"paymentId\":\"" + payment.getId().toString() + "\"}";

    GetQueueUrlResponse queueUrlResponse =
        GetQueueUrlResponse.builder().queueUrl(FAILURE_QUEUE_URL).build();
    SendMessageResponse sendMessageResponse =
        SendMessageResponse.builder().messageId("msg-127").build();

    lenient()
        .when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
        .thenReturn(queueUrlResponse);
<<<<<<< HEAD
    lenient().when(objectMapper.writeValueAsString(any())).thenReturn(messageJson);
=======
    lenient()
        .when(objectMapper.writeValueAsString(any()))
        .thenReturn(messageJson);
>>>>>>> 874da5d659f8f0227b13a5ef37e537fd54c18408
    lenient()
        .when(sqsClient.sendMessage(any(SendMessageRequest.class)))
        .thenReturn(sendMessageResponse);

    // Act
    adapter.sendPaymentResponse(payment);

    // Assert
    verify(sqsClient, times(1)).getQueueUrl(any(GetQueueUrlRequest.class));
    verify(sqsClient, times(1)).sendMessage(any(SendMessageRequest.class));
  }

  @Test
  @DisplayName("Should serialize message with all payment fields")
  void testSendPaymentResponse_SerializesAllFields() throws Exception {
    // Arrange
    UUID paymentId = UUID.randomUUID();
<<<<<<< HEAD
    UUID workOrderId = UUID.randomUUID();
    UUID customerId = UUID.randomUUID();
=======
    UUID budgetId = UUID.randomUUID();
    UUID workOrderId = UUID.randomUUID();
    UUID clientId = UUID.randomUUID();
>>>>>>> 874da5d659f8f0227b13a5ef37e537fd54c18408
    BigDecimal amount = new BigDecimal("150.00");
    LocalDateTime createdAt = LocalDateTime.now();
    LocalDateTime processedAt = LocalDateTime.now();

<<<<<<< HEAD
    Payment payment = new Payment(paymentId, workOrderId, customerId, amount);
=======
    Payment payment = new Payment(paymentId, budgetId, workOrderId, clientId, amount);
>>>>>>> 874da5d659f8f0227b13a5ef37e537fd54c18408
    payment.markAsProcessing("ext_123", "order_456", "PIX", "qr_code_data", "qr_base64");
    payment.markAsApproved();

    String messageJson = "{\"complete\":\"message\"}";

    GetQueueUrlResponse queueUrlResponse =
        GetQueueUrlResponse.builder().queueUrl(SUCCESS_QUEUE_URL).build();
    SendMessageResponse sendMessageResponse =
        SendMessageResponse.builder().messageId("msg-128").build();

    lenient()
        .when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
        .thenReturn(queueUrlResponse);
<<<<<<< HEAD
    lenient().when(objectMapper.writeValueAsString(any())).thenReturn(messageJson);
=======
    lenient()
        .when(objectMapper.writeValueAsString(any()))
        .thenReturn(messageJson);
>>>>>>> 874da5d659f8f0227b13a5ef37e537fd54c18408
    lenient()
        .when(sqsClient.sendMessage(any(SendMessageRequest.class)))
        .thenReturn(sendMessageResponse);

<<<<<<< HEAD
    ArgumentCaptor<Map<String, Object>> messageCaptor = ArgumentCaptor.forClass(Map.class);
=======
    ArgumentCaptor<Map<String, Object>> messageCaptor =
        ArgumentCaptor.forClass(Map.class);
>>>>>>> 874da5d659f8f0227b13a5ef37e537fd54c18408

    // Act
    adapter.sendPaymentResponse(payment);

    // Assert
    verify(objectMapper).writeValueAsString(messageCaptor.capture());
    Map<String, Object> capturedMessage = messageCaptor.getValue();

    org.assertj.core.api.Assertions.assertThat(capturedMessage)
        .containsEntry("paymentId", paymentId.toString())
<<<<<<< HEAD
        .containsEntry("workOrderId", workOrderId.toString())
        .containsEntry("customerId", customerId.toString())
=======
        .containsEntry("budgetId", budgetId.toString())
        .containsEntry("workOrderId", workOrderId.toString())
        .containsEntry("clientId", clientId.toString())
>>>>>>> 874da5d659f8f0227b13a5ef37e537fd54c18408
        .containsEntry("status", "APPROVED")
        .containsEntry("amount", amount)
        .containsEntry("externalPaymentId", "ext_123")
        .containsEntry("paymentMethod", "PIX")
        .containsEntry("qrCode", "qr_code_data")
        .containsEntry("qrCodeBase64", "qr_base64");
  }

  @Test
  @DisplayName("Should not throw exception when SQS client throws")
<<<<<<< HEAD
  void testSendPaymentResponse_SQSClientThrowsException_DoesNotPropagate() throws Exception {
=======
  void testSendPaymentResponse_SQSClientThrowsException_DoesNotPropagate()
      throws Exception {
>>>>>>> 874da5d659f8f0227b13a5ef37e537fd54c18408
    // Arrange
    Payment payment = createPaymentWithStatus(PaymentStatus.APPROVED);

    lenient()
        .when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
        .thenThrow(new RuntimeException("SQS connection failed"));

    // Act & Assert - should not throw
    org.assertj.core.api.Assertions.assertThatNoException()
        .isThrownBy(() -> adapter.sendPaymentResponse(payment));
  }

  @Test
  @DisplayName("Should not throw exception when JSON serialization fails")
<<<<<<< HEAD
  void testSendPaymentResponse_JsonSerializationFails_DoesNotPropagate() throws Exception {
=======
  void testSendPaymentResponse_JsonSerializationFails_DoesNotPropagate()
      throws Exception {
>>>>>>> 874da5d659f8f0227b13a5ef37e537fd54c18408
    // Arrange
    Payment payment = createPaymentWithStatus(PaymentStatus.APPROVED);

    lenient()
        .when(objectMapper.writeValueAsString(any()))
        .thenThrow(new RuntimeException("Jackson serialization error"));

    // Act & Assert - should not throw
    org.assertj.core.api.Assertions.assertThatNoException()
        .isThrownBy(() -> adapter.sendPaymentResponse(payment));
  }

  @Test
  @DisplayName("Should not throw exception when sendMessage fails")
  void testSendPaymentResponse_SendMessageFails_DoesNotPropagate() throws Exception {
    // Arrange
    Payment payment = createPaymentWithStatus(PaymentStatus.APPROVED);
    String messageJson = "{\"paymentId\":\"" + payment.getId().toString() + "\"}";

    GetQueueUrlResponse queueUrlResponse =
        GetQueueUrlResponse.builder().queueUrl(SUCCESS_QUEUE_URL).build();

    lenient()
        .when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
        .thenReturn(queueUrlResponse);
<<<<<<< HEAD
    lenient().when(objectMapper.writeValueAsString(any())).thenReturn(messageJson);
=======
    lenient()
        .when(objectMapper.writeValueAsString(any()))
        .thenReturn(messageJson);
>>>>>>> 874da5d659f8f0227b13a5ef37e537fd54c18408
    lenient()
        .when(sqsClient.sendMessage(any(SendMessageRequest.class)))
        .thenThrow(new RuntimeException("Failed to send message"));

    // Act & Assert - should not throw
    org.assertj.core.api.Assertions.assertThatNoException()
        .isThrownBy(() -> adapter.sendPaymentResponse(payment));
  }

  @Test
  @DisplayName("Should capture correct queue URL in send message request")
  void testSendPaymentResponse_VerifiesCorrectQueueUrl() throws Exception {
    // Arrange
    Payment payment = createPaymentWithStatus(PaymentStatus.APPROVED);
    String messageJson = "{\"paymentId\":\"" + payment.getId().toString() + "\"}";

    GetQueueUrlResponse queueUrlResponse =
        GetQueueUrlResponse.builder().queueUrl(SUCCESS_QUEUE_URL).build();
    SendMessageResponse sendMessageResponse =
        SendMessageResponse.builder().messageId("msg-129").build();

    lenient()
        .when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
        .thenReturn(queueUrlResponse);
<<<<<<< HEAD
    lenient().when(objectMapper.writeValueAsString(any())).thenReturn(messageJson);
=======
    lenient()
        .when(objectMapper.writeValueAsString(any()))
        .thenReturn(messageJson);
>>>>>>> 874da5d659f8f0227b13a5ef37e537fd54c18408
    lenient()
        .when(sqsClient.sendMessage(any(SendMessageRequest.class)))
        .thenReturn(sendMessageResponse);

    ArgumentCaptor<SendMessageRequest> sendRequestCaptor =
        ArgumentCaptor.forClass(SendMessageRequest.class);

    // Act
    adapter.sendPaymentResponse(payment);

    // Assert
    verify(sqsClient).sendMessage(sendRequestCaptor.capture());
    SendMessageRequest capturedRequest = sendRequestCaptor.getValue();

    org.assertj.core.api.Assertions.assertThat(capturedRequest.queueUrl())
        .isEqualTo(SUCCESS_QUEUE_URL);
  }

  // Helper methods

  private Payment createPaymentWithStatus(PaymentStatus status) {
    UUID paymentId = UUID.randomUUID();
<<<<<<< HEAD
    UUID workOrderId = UUID.randomUUID();
    UUID customerId = UUID.randomUUID();
    BigDecimal amount = new BigDecimal("100.00");

    Payment payment = new Payment(paymentId, workOrderId, customerId, amount);
=======
    UUID budgetId = UUID.randomUUID();
    UUID workOrderId = UUID.randomUUID();
    UUID clientId = UUID.randomUUID();
    BigDecimal amount = new BigDecimal("100.00");

    Payment payment = new Payment(paymentId, budgetId, workOrderId, clientId, amount);
>>>>>>> 874da5d659f8f0227b13a5ef37e537fd54c18408

    switch (status) {
      case APPROVED:
        payment.markAsProcessing("ext_123", "order_456", "PIX", "qr_code", "qr_b64");
        payment.markAsApproved();
        break;
      case PROCESSING:
        payment.markAsProcessing("ext_123", "order_456", "PIX", "qr_code", "qr_b64");
        break;
      case REJECTED:
        payment.markAsProcessing("ext_123", "order_456", "PIX", "qr_code", "qr_b64");
        payment.markAsRejected("Rejected by gateway");
        break;
      case FAILED:
        payment.markAsProcessing("ext_123", "order_456", "PIX", "qr_code", "qr_b64");
        payment.markAsFailed("Processing failed");
        break;
      case PENDING:
      default:
        // Keep as pending
        break;
    }

    return payment;
  }

  private void setFieldValue(Object target, String fieldName, Object value) {
    try {
      java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (Exception e) {
      throw new RuntimeException("Failed to set field: " + fieldName, e);
    }
  }
}
