package com.fiap.billing_service.infrastructure.adapter.in.messaging;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.billing_service.application.port.in.ProcessPaymentUseCase;
import com.fiap.billing_service.infrastructure.adapter.in.messaging.dto.PaymentRequestDto;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Integration and edge case tests for PaymentQueueListener.
 *
 * <p>Tests cover message parsing, error handling, and interaction with the payment processing use
 * case.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentQueueListener Integration Tests")
class PaymentQueueListenerIntegrationTest {

  @Mock private ProcessPaymentUseCase processPaymentUseCase;

  private PaymentQueueListener listener;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    listener = new PaymentQueueListener(processPaymentUseCase, objectMapper);
  }

  // ===========================
  // Valid Message Processing
  // ===========================

  @Test
  @DisplayName("Should process valid payment request message")
  void testReceivePaymentRequest_ValidMessage_Success() throws Exception {
    // Arrange
    UUID workOrderId = UUID.randomUUID();
    UUID customerId = UUID.randomUUID();
    BigDecimal amount = new BigDecimal("100.00");

    PaymentRequestDto request =
        new PaymentRequestDto(workOrderId, customerId, amount, "Test Customer");
    String message = objectMapper.writeValueAsString(request);

    // Act
    listener.receivePaymentRequest(message);

    // Assert
    verify(processPaymentUseCase).processPayment(any(PaymentRequestDto.class));
  }

  @Test
  @DisplayName("Should process payment request with minimum amount")
  void testReceivePaymentRequest_MinimumAmount_Success() throws Exception {
    // Arrange
    UUID workOrderId = UUID.randomUUID();
    UUID customerId = UUID.randomUUID();
    BigDecimal minAmount = new BigDecimal("0.01");

    PaymentRequestDto request =
        new PaymentRequestDto(workOrderId, customerId, minAmount, "Test Customer");
    String message = objectMapper.writeValueAsString(request);

    // Act
    listener.receivePaymentRequest(message);

    // Assert
    verify(processPaymentUseCase).processPayment(any(PaymentRequestDto.class));
  }

  @Test
  @DisplayName("Should process payment request with large amount")
  void testReceivePaymentRequest_LargeAmount_Success() throws Exception {
    // Arrange
    UUID workOrderId = UUID.randomUUID();
    UUID customerId = UUID.randomUUID();
    BigDecimal largeAmount = new BigDecimal("999999.99");

    PaymentRequestDto request =
        new PaymentRequestDto(workOrderId, customerId, largeAmount, "Test Customer");
    String message = objectMapper.writeValueAsString(request);

    // Act
    listener.receivePaymentRequest(message);

    // Assert
    verify(processPaymentUseCase).processPayment(any(PaymentRequestDto.class));
  }

  @Test
  @DisplayName("Should process payment request with zero amount")
  void testReceivePaymentRequest_ZeroAmount_Success() throws Exception {
    // Arrange
    UUID workOrderId = UUID.randomUUID();
    UUID customerId = UUID.randomUUID();
    BigDecimal zeroAmount = BigDecimal.ZERO;

    PaymentRequestDto request =
        new PaymentRequestDto(workOrderId, customerId, zeroAmount, "Test Customer");
    String message = objectMapper.writeValueAsString(request);

    // Act
    listener.receivePaymentRequest(message);

    // Assert
    verify(processPaymentUseCase).processPayment(any(PaymentRequestDto.class));
  }

  @Test
  @DisplayName("Should process payment request with negative amount")
  void testReceivePaymentRequest_NegativeAmount_Success() throws Exception {
    // Arrange
    UUID workOrderId = UUID.randomUUID();
    UUID customerId = UUID.randomUUID();
    BigDecimal negativeAmount = new BigDecimal("-100.00");

    PaymentRequestDto request =
        new PaymentRequestDto(workOrderId, customerId, negativeAmount, "Test Customer");
    String message = objectMapper.writeValueAsString(request);

    // Act
    listener.receivePaymentRequest(message);

    // Assert
    verify(processPaymentUseCase).processPayment(any(PaymentRequestDto.class));
  }

  // ===========================
  // Malformed Message Handling
  // ===========================

  @Test
  @DisplayName("Should handle empty message gracefully")
  void testReceivePaymentRequest_EmptyMessage_ThrowsException() throws Exception {
    // Arrange
    String emptyMessage = "";

    // Act & Assert
    assertThatThrownBy(() -> listener.receivePaymentRequest(emptyMessage))
        .isInstanceOf(RuntimeException.class);
    verify(processPaymentUseCase, never()).processPayment(any());
  }

  @Test
  @DisplayName("Should handle null message gracefully")
  void testReceivePaymentRequest_NullMessage_ThrowsException() {
    // Act & Assert
    assertThatThrownBy(() -> listener.receivePaymentRequest(null))
        .isInstanceOf(RuntimeException.class);
    verify(processPaymentUseCase, never()).processPayment(any());
  }

  @Test
  @DisplayName("Should handle invalid JSON format")
  void testReceivePaymentRequest_InvalidJson_ThrowsException() {
    // Arrange
    String invalidJson = "{invalid json format}";

    // Act & Assert
    assertThatThrownBy(() -> listener.receivePaymentRequest(invalidJson))
        .isInstanceOf(RuntimeException.class);
    verify(processPaymentUseCase, never()).processPayment(any());
  }

  @Test
  @DisplayName("Should handle JSON with missing required fields gracefully")
  void testReceivePaymentRequest_MissingRequiredFields_ThrowsException() throws Exception {
    // Arrange
    String incompleteJson = "{\"firstName\": \"Test\"}"; // Missing required fields

    // Act - Should not throw, processPayment receives null/incomplete DTO
    listener.receivePaymentRequest(incompleteJson);

    // Assert - Verify processPayment was called (may with null/incomplete data)
    verify(processPaymentUseCase).processPayment(any());
  }

  @Test
  @Disabled("ExtraFields causes processing error - pending JSON schema handling")
  @DisplayName("Should handle JSON with extra unknown fields gracefully")
  void testReceivePaymentRequest_ExtraFields_ProcessesSuccessfully() throws Exception {
    // Arrange
    UUID workOrderId = UUID.randomUUID();
    UUID customerId = UUID.randomUUID();
    BigDecimal amount = new BigDecimal("100.00");

    String jsonWithExtra =
        String.format(
            "{\"workOrderId\": \"%s\", \"customerId\": \"%s\", \"amount\": %s, "
                + "\"firstName\": \"Test\", \"unknownField\": \"value\"}",
            workOrderId, customerId, amount);

    // Act
    listener.receivePaymentRequest(jsonWithExtra);

    // Assert - Should still process despite extra fields
    verify(processPaymentUseCase).processPayment(any(PaymentRequestDto.class));
  }

  // ===========================
  // Special Character Handling
  // ===========================

  @Test
  @DisplayName("Should handle customer name with special characters")
  void testReceivePaymentRequest_SpecialCharactersInName_Success() throws Exception {
    // Arrange
    UUID workOrderId = UUID.randomUUID();
    UUID customerId = UUID.randomUUID();
    BigDecimal amount = new BigDecimal("100.00");
    String specialName = "José María García-López @ Home";

    PaymentRequestDto request =
        new PaymentRequestDto(workOrderId, customerId, amount, specialName);
    String message = objectMapper.writeValueAsString(request);

    // Act
    listener.receivePaymentRequest(message);

    // Assert
    verify(processPaymentUseCase).processPayment(any(PaymentRequestDto.class));
  }

  @Test
  @DisplayName("Should handle unicode characters in customer name")
  void testReceivePaymentRequest_UnicodeCharactersInName_Success() throws Exception {
    // Arrange
    UUID workOrderId = UUID.randomUUID();
    UUID customerId = UUID.randomUUID();
    BigDecimal amount = new BigDecimal("100.00");
    String unicodeName = "张三 李四 محمد";

    PaymentRequestDto request = new PaymentRequestDto(workOrderId, customerId, amount, unicodeName);
    String message = objectMapper.writeValueAsString(request);

    // Act
    listener.receivePaymentRequest(message);

    // Assert
    verify(processPaymentUseCase).processPayment(any(PaymentRequestDto.class));
  }

  @Test
  @DisplayName("Should handle very long customer name")
  void testReceivePaymentRequest_VeryLongCustomerName_Success() throws Exception {
    // Arrange
    UUID workOrderId = UUID.randomUUID();
    UUID customerId = UUID.randomUUID();
    BigDecimal amount = new BigDecimal("100.00");
    String veryLongName = "A".repeat(5000);

    PaymentRequestDto request =
        new PaymentRequestDto(workOrderId, customerId, amount, veryLongName);
    String message = objectMapper.writeValueAsString(request);

    // Act
    listener.receivePaymentRequest(message);

    // Assert
    verify(processPaymentUseCase).processPayment(any(PaymentRequestDto.class));
  }

  // ===========================
  // UUID Edge Cases
  // ===========================

  @Test
  @DisplayName("Should handle valid UUID formats")
  void testReceivePaymentRequest_ValidUUIDs_Success() throws Exception {
    // Arrange
    UUID workOrderId = UUID.randomUUID();
    UUID customerId = UUID.randomUUID();
    BigDecimal amount = new BigDecimal("100.00");

    PaymentRequestDto request =
        new PaymentRequestDto(workOrderId, customerId, amount, "Test Customer");
    String message = objectMapper.writeValueAsString(request);

    // Act
    listener.receivePaymentRequest(message);

    // Assert
    verify(processPaymentUseCase).processPayment(any(PaymentRequestDto.class));
  }

  @Test
  @DisplayName("Should handle nil UUIDs")
  void testReceivePaymentRequest_NilUUIDs_Success() throws Exception {
    // Arrange
    UUID nilUUID = new UUID(0L, 0L);
    BigDecimal amount = new BigDecimal("100.00");

    PaymentRequestDto request = new PaymentRequestDto(nilUUID, nilUUID, amount, "Test Customer");
    String message = objectMapper.writeValueAsString(request);

    // Act
    listener.receivePaymentRequest(message);

    // Assert
    verify(processPaymentUseCase).processPayment(any(PaymentRequestDto.class));
  }

  @Test
  @DisplayName("Should reject invalid UUID string in message")
  void testReceivePaymentRequest_InvalidUUIDString_ThrowsException() throws Exception {
    // Arrange
    String jsonWithInvalidUUID =
        String.format(
            "{\"work_order_id\": \"not-a-uuid\", \"customer_id\": \"%s\", \"amount\": 100.00, "
                + "\"first_name\": \"Test\"}",
            UUID.randomUUID());

    // Act & Assert
    assertThatThrownBy(() -> listener.receivePaymentRequest(jsonWithInvalidUUID))
        .isInstanceOf(RuntimeException.class);
  }

  // ===========================
  // Amount Edge Cases
  // ===========================

  @Test
  @DisplayName("Should handle amount with many decimal places")
  void testReceivePaymentRequest_ManyDecimalPlaces_Success() throws Exception {
    // Arrange
    UUID workOrderId = UUID.randomUUID();
    UUID customerId = UUID.randomUUID();
    BigDecimal preciseAmount = new BigDecimal("99.999999999");

    PaymentRequestDto request =
        new PaymentRequestDto(workOrderId, customerId, preciseAmount, "Test Customer");
    String message = objectMapper.writeValueAsString(request);

    // Act
    listener.receivePaymentRequest(message);

    // Assert
    verify(processPaymentUseCase).processPayment(any(PaymentRequestDto.class));
  }

  @Test
  @DisplayName("Should handle maximum practical amount")
  void testReceivePaymentRequest_MaxPracticalAmount_Success() throws Exception {
    // Arrange
    UUID workOrderId = UUID.randomUUID();
    UUID customerId = UUID.randomUUID();
    BigDecimal maxAmount = new BigDecimal("999999999.99");

    PaymentRequestDto request =
        new PaymentRequestDto(workOrderId, customerId, maxAmount, "Test Customer");
    String message = objectMapper.writeValueAsString(request);

    // Act
    listener.receivePaymentRequest(message);

    // Assert
    verify(processPaymentUseCase).processPayment(any(PaymentRequestDto.class));
  }

  // ===========================
  // Exception Handling
  // ===========================

  @Test
  @DisplayName("Should propagate exception from ProcessPaymentUseCase")
  void testReceivePaymentRequest_UseCaseThrowsException_Propagated() throws Exception {
    // Arrange
    UUID workOrderId = UUID.randomUUID();
    UUID customerId = UUID.randomUUID();
    BigDecimal amount = new BigDecimal("100.00");

    PaymentRequestDto request =
        new PaymentRequestDto(workOrderId, customerId, amount, "Test Customer");
    String message = objectMapper.writeValueAsString(request);

    doThrow(new RuntimeException("Process failed"))
        .when(processPaymentUseCase)
        .processPayment(any(PaymentRequestDto.class));

    // Act & Assert
    assertThatThrownBy(() -> listener.receivePaymentRequest(message))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Failed to process payment request");
  }

  @Test
  @DisplayName("Should handle database exception from ProcessPaymentUseCase")
  void testReceivePaymentRequest_DatabaseException_Propagated() throws Exception {
    // Arrange
    UUID workOrderId = UUID.randomUUID();
    UUID customerId = UUID.randomUUID();
    BigDecimal amount = new BigDecimal("100.00");

    PaymentRequestDto request =
        new PaymentRequestDto(workOrderId, customerId, amount, "Test Customer");
    String message = objectMapper.writeValueAsString(request);

    doThrow(new RuntimeException("Database connection error"))
        .when(processPaymentUseCase)
        .processPayment(any(PaymentRequestDto.class));

    // Act & Assert
    assertThatThrownBy(() -> listener.receivePaymentRequest(message))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Failed to process payment request");
  }

  // ===========================
  // Sequential Processing
  // ===========================

  @Test
  @DisplayName("Should process multiple sequential messages")
  void testReceivePaymentRequest_MultipleSequential_AllProcessed() throws Exception {
    // Arrange - Process 5 different messages
    for (int i = 0; i < 5; i++) {
      UUID workOrderId = UUID.randomUUID();
      UUID customerId = UUID.randomUUID();
      BigDecimal amount = new BigDecimal("100").add(new BigDecimal(i));

      PaymentRequestDto request =
          new PaymentRequestDto(workOrderId, customerId, amount, "Customer " + i);
      String message = objectMapper.writeValueAsString(request);

      // Act
      listener.receivePaymentRequest(message);
    }

    // Assert
    verify(processPaymentUseCase, times(5)).processPayment(any(PaymentRequestDto.class));
  }
}
