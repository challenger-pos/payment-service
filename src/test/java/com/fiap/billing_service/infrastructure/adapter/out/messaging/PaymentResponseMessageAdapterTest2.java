package com.fiap.billing_service.infrastructure.adapter.out.messaging;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.billing_service.domain.entity.Payment;
import com.fiap.billing_service.domain.valueobject.PaymentStatus;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentResponseMessageAdapter Integration Tests")
class PaymentResponseMessageAdapterTest2 {

  @Mock private SqsClient sqsClient;

  private ObjectMapper objectMapper;
  private PaymentResponseMessageAdapter adapter;

  private UUID paymentId;
  private UUID workOrderId;
  private UUID customerId;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    adapter = new PaymentResponseMessageAdapter(sqsClient, objectMapper);

    paymentId = UUID.randomUUID();
    workOrderId = UUID.randomUUID();
    customerId = UUID.randomUUID();

    // Set queue names via reflection
    ReflectionTestUtils.setField(adapter, "successQueueName", "payment-response-success-queue");
    ReflectionTestUtils.setField(adapter, "failureQueueName", "payment-response-failure-queue");
  }

  @Test
  @DisplayName("Should send payment response to success queue when status is APPROVED")
  void testSendPaymentResponse_Approved_SendsToSuccessQueue() {
    // Arrange
    Payment approvedPayment =
        new Payment(paymentId, workOrderId, customerId, new BigDecimal("100.00"));
    approvedPayment.markAsApproved();

    GetQueueUrlResponse queueUrlResponse =
        GetQueueUrlResponse.builder()
            .queueUrl("https://sqs.us-east-2.amazonaws.com/123456789/payment-response-success-queue")
            .build();

    when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(queueUrlResponse);

    SendMessageResponse sendMessageResponse =
        SendMessageResponse.builder().messageId("test-message-id-123").build();

    when(sqsClient.sendMessage(any(SendMessageRequest.class))).thenReturn(sendMessageResponse);

    // Act
    adapter.sendPaymentResponse(approvedPayment);

    // Assert
    ArgumentCaptor<GetQueueUrlRequest> queueUrlCaptor =
        ArgumentCaptor.forClass(GetQueueUrlRequest.class);
    verify(sqsClient, times(1)).getQueueUrl(queueUrlCaptor.capture());

    GetQueueUrlRequest capturedRequest = queueUrlCaptor.getValue();
    assertEquals("payment-response-success-queue", capturedRequest.queueName());

    verify(sqsClient, times(1)).sendMessage(any(SendMessageRequest.class));
  }

  @Test
  @DisplayName("Should send payment response to success queue when status is PROCESSING")
  void testSendPaymentResponse_Processing_SendsToSuccessQueue() {
    // Arrange
    Payment processingPayment =
        new Payment(paymentId, workOrderId, customerId, new BigDecimal("100.00"));
    processingPayment.markAsProcessing(
        "ext-123", "order-123", "pix", "qr-code", "qr-base64");

    GetQueueUrlResponse queueUrlResponse =
        GetQueueUrlResponse.builder()
            .queueUrl("https://sqs.us-east-2.amazonaws.com/123456789/payment-response-success-queue")
            .build();

    when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(queueUrlResponse);

    SendMessageResponse sendMessageResponse =
        SendMessageResponse.builder().messageId("test-message-id-456").build();

    when(sqsClient.sendMessage(any(SendMessageRequest.class))).thenReturn(sendMessageResponse);

    // Act
    adapter.sendPaymentResponse(processingPayment);

    // Assert
    ArgumentCaptor<GetQueueUrlRequest> queueUrlCaptor =
        ArgumentCaptor.forClass(GetQueueUrlRequest.class);
    verify(sqsClient, times(1)).getQueueUrl(queueUrlCaptor.capture());

    GetQueueUrlRequest capturedRequest = queueUrlCaptor.getValue();
    assertEquals("payment-response-success-queue", capturedRequest.queueName());

    verify(sqsClient, times(1)).sendMessage(any(SendMessageRequest.class));
  }

  @Test
  @DisplayName("Should send payment response to failure queue when status is REJECTED")
  void testSendPaymentResponse_Rejected_SendsToFailureQueue() {
    // Arrange
    Payment rejectedPayment =
        new Payment(paymentId, workOrderId, customerId, new BigDecimal("100.00"));
    rejectedPayment.markAsRejected("Insufficient funds");

    GetQueueUrlResponse queueUrlResponse =
        GetQueueUrlResponse.builder()
            .queueUrl("https://sqs.us-east-2.amazonaws.com/123456789/payment-response-failure-queue")
            .build();

    when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(queueUrlResponse);

    SendMessageResponse sendMessageResponse =
        SendMessageResponse.builder().messageId("test-message-id-789").build();

    when(sqsClient.sendMessage(any(SendMessageRequest.class))).thenReturn(sendMessageResponse);

    // Act
    adapter.sendPaymentResponse(rejectedPayment);

    // Assert
    ArgumentCaptor<GetQueueUrlRequest> queueUrlCaptor =
        ArgumentCaptor.forClass(GetQueueUrlRequest.class);
    verify(sqsClient, times(1)).getQueueUrl(queueUrlCaptor.capture());

    GetQueueUrlRequest capturedRequest = queueUrlCaptor.getValue();
    assertEquals("payment-response-failure-queue", capturedRequest.queueName());

    verify(sqsClient, times(1)).sendMessage(any(SendMessageRequest.class));
  }

  @Test
  @DisplayName("Should send payment response to failure queue when status is FAILED")
  void testSendPaymentResponse_Failed_SendsToFailureQueue() {
    // Arrange
    Payment failedPayment =
        new Payment(paymentId, workOrderId, customerId, new BigDecimal("100.00"));
    failedPayment.markAsFailed("Gateway timeout");

    GetQueueUrlResponse queueUrlResponse =
        GetQueueUrlResponse.builder()
            .queueUrl("https://sqs.us-east-2.amazonaws.com/123456789/payment-response-failure-queue")
            .build();

    when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(queueUrlResponse);

    SendMessageResponse sendMessageResponse =
        SendMessageResponse.builder().messageId("test-message-id-fail").build();

    when(sqsClient.sendMessage(any(SendMessageRequest.class))).thenReturn(sendMessageResponse);

    // Act
    adapter.sendPaymentResponse(failedPayment);

    // Assert
    ArgumentCaptor<GetQueueUrlRequest> queueUrlCaptor =
        ArgumentCaptor.forClass(GetQueueUrlRequest.class);
    verify(sqsClient, times(1)).getQueueUrl(queueUrlCaptor.capture());

    GetQueueUrlRequest capturedRequest = queueUrlCaptor.getValue();
    assertEquals("payment-response-failure-queue", capturedRequest.queueName());

    verify(sqsClient, times(1)).sendMessage(any(SendMessageRequest.class));
  }

  @Test
  @DisplayName("Should correctly serialize payment data to JSON message")
  void testSendPaymentResponse_SerializesPaymentData_Correctly() {
    // Arrange
    Payment approvedPayment =
        new Payment(paymentId, workOrderId, customerId, new BigDecimal("100.00"));
    approvedPayment.markAsApproved();

    GetQueueUrlResponse queueUrlResponse =
        GetQueueUrlResponse.builder()
            .queueUrl("https://sqs.us-east-2.amazonaws.com/123456789/payment-response-success-queue")
            .build();

    when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(queueUrlResponse);

    SendMessageResponse sendMessageResponse =
        SendMessageResponse.builder().messageId("test-message-id").build();

    when(sqsClient.sendMessage(any(SendMessageRequest.class))).thenReturn(sendMessageResponse);

    // Act
    adapter.sendPaymentResponse(approvedPayment);

    // Assert
    ArgumentCaptor<SendMessageRequest> messageCaptor =
        ArgumentCaptor.forClass(SendMessageRequest.class);
    verify(sqsClient, times(1)).sendMessage(messageCaptor.capture());

    SendMessageRequest capturedRequest = messageCaptor.getValue();
    String messageBody = capturedRequest.messageBody();

    assertNotNull(messageBody);
    assertTrue(messageBody.contains(workOrderId.toString()));
    assertTrue(messageBody.contains("workOrderId"));
  }

  @Test
  @DisplayName("Should handle SQS client exception gracefully (no exception thrown)")
  void testSendPaymentResponse_SqsClientException_HandledGracefully() {
    // Arrange
    Payment payment =
        new Payment(paymentId, workOrderId, customerId, new BigDecimal("100.00"));
    payment.markAsApproved();

    when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
        .thenThrow(new RuntimeException("SQS service unavailable"));

    // Act & Assert - should not throw exception
    assertDoesNotThrow(() -> adapter.sendPaymentResponse(payment));

    verify(sqsClient, times(1)).getQueueUrl(any(GetQueueUrlRequest.class));
    verify(sqsClient, never()).sendMessage(any(SendMessageRequest.class));
  }

  @Test
  @DisplayName("Should handle JSON serialization exception gracefully")
  void testSendPaymentResponse_JsonSerializationError_HandledGracefully() throws Exception {
    // Arrange
    Payment payment =
        new Payment(paymentId, workOrderId, customerId, new BigDecimal("100.00"));
    payment.markAsApproved();

    // Create a mock ObjectMapper that throws exception
    ObjectMapper failingMapper = mock(ObjectMapper.class);
    when(failingMapper.writeValueAsString(any()))
        .thenThrow(new RuntimeException("JSON serialization failed"));

    PaymentResponseMessageAdapter adapterWithFailingMapper =
        new PaymentResponseMessageAdapter(sqsClient, failingMapper);
    ReflectionTestUtils.setField(
        adapterWithFailingMapper, "successQueueName", "payment-response-success-queue");
    ReflectionTestUtils.setField(
        adapterWithFailingMapper, "failureQueueName", "payment-response-failure-queue");

    // Act & Assert - should not throw exception
    assertDoesNotThrow(() -> adapterWithFailingMapper.sendPaymentResponse(payment));

    verify(sqsClient, never()).getQueueUrl(any(GetQueueUrlRequest.class));
  }

  @Test
  @DisplayName("Should include workOrderId in message payload")
  void testSendPaymentResponse_IncludesWorkOrderId_InMessagePayload() {
    // Arrange
    Payment approvedPayment =
        new Payment(paymentId, workOrderId, customerId, new BigDecimal("100.00"));
    approvedPayment.markAsApproved();

    GetQueueUrlResponse queueUrlResponse =
        GetQueueUrlResponse.builder()
            .queueUrl("https://sqs.us-east-2.amazonaws.com/123456789/payment-response-success-queue")
            .build();

    when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(queueUrlResponse);

    SendMessageResponse sendMessageResponse =
        SendMessageResponse.builder().messageId("test-message-id").build();

    when(sqsClient.sendMessage(any(SendMessageRequest.class))).thenReturn(sendMessageResponse);

    // Act
    adapter.sendPaymentResponse(approvedPayment);

    // Assert
    ArgumentCaptor<SendMessageRequest> messageCaptor =
        ArgumentCaptor.forClass(SendMessageRequest.class);
    verify(sqsClient, times(1)).sendMessage(messageCaptor.capture());

    SendMessageRequest capturedRequest = messageCaptor.getValue();
    String messageBody = capturedRequest.messageBody();

    assertTrue(messageBody.contains(workOrderId.toString()));
  }

  @Test
  @DisplayName("Should use correct queue URL when sending message")
  void testSendPaymentResponse_UsesCorrectQueueUrl_ForSendingMessage() {
    // Arrange
    Payment approvedPayment =
        new Payment(paymentId, workOrderId, customerId, new BigDecimal("100.00"));
    approvedPayment.markAsApproved();

    String expectedQueueUrl =
        "https://sqs.us-east-2.amazonaws.com/123456789/payment-response-success-queue";

    GetQueueUrlResponse queueUrlResponse =
        GetQueueUrlResponse.builder().queueUrl(expectedQueueUrl).build();

    when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(queueUrlResponse);

    SendMessageResponse sendMessageResponse =
        SendMessageResponse.builder().messageId("test-message-id").build();

    when(sqsClient.sendMessage(any(SendMessageRequest.class))).thenReturn(sendMessageResponse);

    // Act
    adapter.sendPaymentResponse(approvedPayment);

    // Assert
    ArgumentCaptor<SendMessageRequest> messageCaptor =
        ArgumentCaptor.forClass(SendMessageRequest.class);
    verify(sqsClient, times(1)).sendMessage(messageCaptor.capture());

    SendMessageRequest capturedRequest = messageCaptor.getValue();
    assertEquals(expectedQueueUrl, capturedRequest.queueUrl());
  }
}
