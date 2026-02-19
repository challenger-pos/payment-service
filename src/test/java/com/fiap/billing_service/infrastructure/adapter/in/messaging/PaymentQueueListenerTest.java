package com.fiap.billing_service.infrastructure.adapter.in.messaging;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.billing_service.application.port.in.ProcessPaymentUseCase;
import com.fiap.billing_service.domain.entity.Payment;
import com.fiap.billing_service.infrastructure.adapter.in.messaging.dto.PaymentRequestDto;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentQueueListener Tests")
class PaymentQueueListenerTest {

  @Mock private ProcessPaymentUseCase processPaymentUseCase;

  @Mock private ObjectMapper objectMapper;

  private PaymentQueueListener listener;

  @BeforeEach
  void setUp() {
    listener = new PaymentQueueListener(processPaymentUseCase, objectMapper);
  }

  @Test
  @DisplayName("Should process valid payment request message and call use case")
  void testReceivePaymentRequest_ValidMessage_CallsUseCase() throws Exception {
    // Arrange
    UUID workOrderId = UUID.randomUUID();
    UUID customerId = UUID.randomUUID();

    String jsonMessage = "{\"workOrderId\":\"" + workOrderId + "\"}";

    PaymentRequestDto paymentRequest = new PaymentRequestDto();
    paymentRequest.setWorkOrderId(workOrderId);
    paymentRequest.setCustomerId(customerId);

    Payment processedPayment =
        new Payment(workOrderId, workOrderId, customerId, new BigDecimal("100.00"));

    when(objectMapper.readValue(jsonMessage, PaymentRequestDto.class)).thenReturn(paymentRequest);
    when(processPaymentUseCase.processPayment(any(PaymentRequestDto.class)))
        .thenReturn(processedPayment);

    // Act
    listener.receivePaymentRequest(jsonMessage);

    // Assert
    ArgumentCaptor<PaymentRequestDto> captor = ArgumentCaptor.forClass(PaymentRequestDto.class);
    verify(processPaymentUseCase, times(1)).processPayment(captor.capture());
    assertThat(captor.getValue().getWorkOrderId()).isEqualTo(workOrderId);
    assertThat(captor.getValue().getCustomerId()).isEqualTo(customerId);
  }

  @Test
  @DisplayName("Should throw exception when JSON deserialization fails")
  void testReceivePaymentRequest_InvalidJson_ThrowsException() throws Exception {
    // Arrange
    String invalidJson = "{invalid json}";
    when(objectMapper.readValue(invalidJson, PaymentRequestDto.class))
        .thenThrow(new com.fasterxml.jackson.core.JsonParseException(null, "Invalid JSON"));

    // Act & Assert
    assertThatThrownBy(() -> listener.receivePaymentRequest(invalidJson))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Failed to process payment request");
  }

  @Test
  @DisplayName("Should throw exception when message is null")
  void testReceivePaymentRequest_NullMessage_ThrowsException() throws Exception {
    // Arrange
    when(objectMapper.readValue((String) null, PaymentRequestDto.class))
        .thenThrow(new IllegalArgumentException("Message cannot be null"));

    // Act & Assert
    assertThatThrownBy(() -> listener.receivePaymentRequest(null))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Failed to process payment request");
  }

  @Test
  @DisplayName("Should throw exception when use case throws exception")
  void testReceivePaymentRequest_UseCaseThrowsException_PropagatesAsRuntimeException()
      throws Exception {
    // Arrange
    String jsonMessage = "{\"workOrderId\":\"123\"}";
    PaymentRequestDto paymentRequest = new PaymentRequestDto();

    when(objectMapper.readValue(jsonMessage, PaymentRequestDto.class)).thenReturn(paymentRequest);
    when(processPaymentUseCase.processPayment(any(PaymentRequestDto.class)))
        .thenThrow(new RuntimeException("Payment processing failed"));

    // Act & Assert
    assertThatThrownBy(() -> listener.receivePaymentRequest(jsonMessage))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Failed to process payment request");
  }

  @Test
  @DisplayName("Should verify objectMapper is called to deserialize message")
  void testReceivePaymentRequest_VerifiesDeserialization() throws Exception {
    // Arrange
    String jsonMessage = "{\"data\":\"test\"}";
    PaymentRequestDto paymentRequest = new PaymentRequestDto();

    when(objectMapper.readValue(jsonMessage, PaymentRequestDto.class)).thenReturn(paymentRequest);
    when(processPaymentUseCase.processPayment(any(PaymentRequestDto.class))).thenReturn(null);

    // Act
    listener.receivePaymentRequest(jsonMessage);

    // Assert
    verify(objectMapper, times(1)).readValue(jsonMessage, PaymentRequestDto.class);
  }

  @Test
  @DisplayName("Should handle exception with all required fields populated")
  void testReceivePaymentRequest_ExceptionIncludesAllDetails() throws Exception {
    // Arrange
    UUID workOrderId = UUID.randomUUID();
    String jsonMessage = "{\"workOrderId\":\"" + workOrderId + "\"}";
    PaymentRequestDto paymentRequest = new PaymentRequestDto();
    paymentRequest.setWorkOrderId(workOrderId);

    Exception originalException = new RuntimeException("Original payment processing error");

    when(objectMapper.readValue(jsonMessage, PaymentRequestDto.class)).thenReturn(paymentRequest);
    when(processPaymentUseCase.processPayment(any(PaymentRequestDto.class)))
        .thenThrow(originalException);

    // Act & Assert
    assertThatThrownBy(() -> listener.receivePaymentRequest(jsonMessage))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Failed to process payment request")
        .hasCause(originalException);
  }

  @Test
  @DisplayName("Should process payment with all request details preserved")
  void testReceivePaymentRequest_PreservesAllRequestDetails() throws Exception {
    // Arrange
    UUID workOrderId = UUID.randomUUID();
    UUID clientId = UUID.randomUUID();
    UUID budgetId = UUID.randomUUID();
    String description = "Test payment for order";

    String jsonMessage =
        "{\"workOrderId\":\"" + workOrderId + "\",\"clientId\":\"" + clientId + "\"}";

    PaymentRequestDto paymentRequest = new PaymentRequestDto();
    paymentRequest.setWorkOrderId(workOrderId);
    paymentRequest.setCustomerId(clientId);
    paymentRequest.setDescription(description);

    when(objectMapper.readValue(jsonMessage, PaymentRequestDto.class)).thenReturn(paymentRequest);
    when(processPaymentUseCase.processPayment(any(PaymentRequestDto.class))).thenReturn(null);

    // Act
    listener.receivePaymentRequest(jsonMessage);

    // Assert
    ArgumentCaptor<PaymentRequestDto> captor = ArgumentCaptor.forClass(PaymentRequestDto.class);
    verify(processPaymentUseCase).processPayment(captor.capture());

    PaymentRequestDto capturedRequest = captor.getValue();
    assertThat(capturedRequest.getWorkOrderId()).isEqualTo(workOrderId);
    assertThat(capturedRequest.getCustomerId()).isEqualTo(clientId);
    assertThat(capturedRequest.getDescription()).isEqualTo(description);
  }
}
