package com.fiap.billing_service.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fiap.billing_service.application.port.out.PaymentGatewayPort;
import com.fiap.billing_service.application.port.out.PaymentOrderQueryPort;
import com.fiap.billing_service.application.port.out.PaymentRepositoryPort;
import com.fiap.billing_service.application.port.out.PaymentResponseMessagePort;
import com.fiap.billing_service.domain.dto.PaymentResponse;
import com.fiap.billing_service.domain.entity.Payment;
import com.fiap.billing_service.domain.exception.PaymentProcessingException;
import com.fiap.billing_service.domain.valueobject.PaymentStatus;
import com.fiap.billing_service.infrastructure.adapter.in.messaging.dto.PaymentRequestDto;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessPaymentService Idempotency Tests")
class ProcessPaymentServiceIdempotencyTest {

  @Mock private PaymentRepositoryPort paymentRepository;
  @Mock private PaymentGatewayPort paymentGateway;
  @Mock private PaymentOrderQueryPort paymentOrderQuery;
  @Mock private PaymentResponseMessagePort paymentResponseMessage;

  private ProcessPaymentService service;

  private UUID workOrderId;
  private UUID clientId;
  private UUID budgetId;
  private PaymentRequestDto paymentRequest;

  @BeforeEach
  void setUp() {
    service =
        new ProcessPaymentService(
            paymentRepository, paymentGateway, paymentOrderQuery, paymentResponseMessage);

    workOrderId = UUID.randomUUID();
    clientId = UUID.randomUUID();
    budgetId = UUID.randomUUID();

    // Setup payment request DTO
    paymentRequest = new PaymentRequestDto();
    paymentRequest.setWorkOrderId(workOrderId);
    paymentRequest.setClientId(clientId);
    paymentRequest.setBudgetId(budgetId);

    PaymentRequestDto.OrderRequest orderRequest = paymentRequest.new OrderRequest();
    orderRequest.setTotalAmount("100.00");
    paymentRequest.setOrderRequest(orderRequest);
    paymentRequest.setDescription("Test payment");
  }

  @Test
  @DisplayName("Should return existing payment when duplicate request with APPROVED status")
  void testProcessPayment_DuplicateRequest_ApprovedStatus_ReturnsExisting() {
    // Arrange
    Payment existingPayment =
        new Payment(UUID.randomUUID(), budgetId, workOrderId, clientId, new BigDecimal("100.00"));
    existingPayment.markAsApproved();

    when(paymentRepository.findByWorkOrderId(workOrderId)).thenReturn(Optional.of(existingPayment));

    // Act
    Payment result = service.processPayment(paymentRequest);

    // Assert
    assertNotNull(result);
    assertEquals(existingPayment.getId(), result.getId());
    assertEquals(PaymentStatus.APPROVED, result.getStatus());

    // Verify no new payment was created or processed
    verify(paymentRepository, times(1)).findByWorkOrderId(workOrderId);
    verify(paymentRepository, never()).save(any());
    verify(paymentGateway, never()).processPixPayment(any(), any(), any());
    verify(paymentResponseMessage, never()).sendPaymentResponse(any());
  }

  @Test
  @DisplayName("Should return existing payment when duplicate request with REJECTED status")
  void testProcessPayment_DuplicateRequest_RejectedStatus_ReturnsExisting() {
    // Arrange
    Payment existingPayment =
        new Payment(UUID.randomUUID(), budgetId, workOrderId, clientId, new BigDecimal("100.00"));
    existingPayment.markAsRejected("Insufficient funds");

    when(paymentRepository.findByWorkOrderId(workOrderId)).thenReturn(Optional.of(existingPayment));

    // Act
    Payment result = service.processPayment(paymentRequest);

    // Assert
    assertNotNull(result);
    assertEquals(existingPayment.getId(), result.getId());
    assertEquals(PaymentStatus.REJECTED, result.getStatus());
    assertEquals("Insufficient funds", result.getErrorMessage());

    // Verify no new processing occurred
    verify(paymentRepository, times(1)).findByWorkOrderId(workOrderId);
    verify(paymentGateway, never()).processPixPayment(any(), any(), any());
  }

  @Test
  @DisplayName("Should return existing payment when duplicate request with PROCESSING status")
  void testProcessPayment_DuplicateRequest_ProcessingStatus_ReturnsExisting() {
    // Arrange
    Payment existingPayment =
        new Payment(UUID.randomUUID(), budgetId, workOrderId, clientId, new BigDecimal("100.00"));
    existingPayment.markAsProcessing("ext123", "order123", "pix", "qr", "qr64");

    when(paymentRepository.findByWorkOrderId(workOrderId)).thenReturn(Optional.of(existingPayment));

    // Act
    Payment result = service.processPayment(paymentRequest);

    // Assert
    assertNotNull(result);
    assertEquals(existingPayment.getId(), result.getId());
    assertEquals(PaymentStatus.PROCESSING, result.getStatus());

    verify(paymentRepository, times(1)).findByWorkOrderId(workOrderId);
    verify(paymentGateway, never()).processPixPayment(any(), any(), any());
  }

  @Test
  @DisplayName("Should continue processing when existing payment has PENDING status")
  void testProcessPayment_DuplicateRequest_PendingStatus_ContinuesProcessing() {
    // Arrange
    Payment existingPayment =
        new Payment(UUID.randomUUID(), budgetId, workOrderId, clientId, new BigDecimal("100.00"));
    // Status is PENDING by default

    when(paymentRepository.findByWorkOrderId(workOrderId))
        .thenReturn(Optional.of(existingPayment))
        .thenReturn(Optional.empty()); // Second call returns empty to allow creation

    Payment newPayment =
        new Payment(UUID.randomUUID(), budgetId, workOrderId, clientId, new BigDecimal("100.00"));

    PaymentResponse gatewayResponse =
        new PaymentResponse(
            "ext123", "order123", "pix", PaymentStatus.APPROVED, "qr", "qr64", null);

    when(paymentRepository.save(any(Payment.class))).thenReturn(newPayment);
    when(paymentGateway.processPixPayment(any(), any(), any())).thenReturn(gatewayResponse);
    when(paymentOrderQuery.getOrderStatus(any())).thenReturn(newPayment);

    // Act
    Payment result = service.processPayment(paymentRequest);

    // Assert
    assertNotNull(result);
    verify(paymentRepository, atLeastOnce()).findByWorkOrderId(workOrderId);
    verify(paymentGateway, times(1)).processPixPayment(any(), any(), any());
  }

  @Test
  @DisplayName(
      "Should handle race condition with DataIntegrityViolationException and return existing")
  void testProcessPayment_RaceCondition_ReturnsExisting() {
    // Arrange
    when(paymentRepository.findByWorkOrderId(workOrderId))
        .thenReturn(Optional.empty()); // First check: no payment

    // Simulate race condition: constraint violation on save
    when(paymentRepository.save(any(Payment.class)))
        .thenThrow(new DataIntegrityViolationException("Duplicate key"));

    // After exception, finding the existing payment
    Payment existingPayment =
        new Payment(UUID.randomUUID(), budgetId, workOrderId, clientId, new BigDecimal("100.00"));
    existingPayment.markAsApproved();

    when(paymentRepository.findByWorkOrderId(workOrderId))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(existingPayment));

    // Act
    Payment result = service.processPayment(paymentRequest);

    // Assert
    assertNotNull(result);
    assertEquals(existingPayment.getId(), result.getId());
    assertEquals(PaymentStatus.APPROVED, result.getStatus());

    verify(paymentRepository, times(2)).findByWorkOrderId(workOrderId);
    verify(paymentRepository, times(1)).save(any());
    verify(paymentGateway, never()).processPixPayment(any(), any(), any());
  }

  @Test
  @DisplayName(
      "Should throw exception when race condition occurs but existing payment not found")
  void testProcessPayment_RaceCondition_PaymentNotFound_ThrowsException() {
    // Arrange
    when(paymentRepository.findByWorkOrderId(workOrderId)).thenReturn(Optional.empty());

    when(paymentRepository.save(any(Payment.class)))
        .thenThrow(new DataIntegrityViolationException("Duplicate key"));

    // After exception, still not finding the payment (should not happen in practice)
    when(paymentRepository.findByWorkOrderId(workOrderId)).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(PaymentProcessingException.class, () -> service.processPayment(paymentRequest));

    verify(paymentRepository, times(2)).findByWorkOrderId(workOrderId);
    verify(paymentRepository, times(1)).save(any());
  }

  @Test
  @DisplayName("Should create new payment when no duplicate exists")
  void testProcessPayment_NoDuplicate_CreatesNewPayment() {
    // Arrange
    when(paymentRepository.findByWorkOrderId(workOrderId)).thenReturn(Optional.empty());

    Payment newPayment =
        new Payment(UUID.randomUUID(), budgetId, workOrderId, clientId, new BigDecimal("100.00"));

    PaymentResponse gatewayResponse =
        new PaymentResponse(
            "ext123", "order123", "pix", PaymentStatus.APPROVED, "qr", "qr64", null);

    when(paymentRepository.save(any(Payment.class))).thenReturn(newPayment);
    when(paymentGateway.processPixPayment(any(), any(), any())).thenReturn(gatewayResponse);

    Payment queryResult = new Payment(newPayment.getId(), budgetId, workOrderId, clientId, new BigDecimal("100.00"));
    queryResult.markAsApproved();
    when(paymentOrderQuery.getOrderStatus(any())).thenReturn(queryResult);

    // Act
    Payment result = service.processPayment(paymentRequest);

    // Assert
    assertNotNull(result);
    verify(paymentRepository, times(1)).findByWorkOrderId(workOrderId);
    verify(paymentRepository, atLeast(1)).save(any());
    verify(paymentGateway, times(1))
        .processPixPayment(eq(new BigDecimal("100.00")), isNull(), any());
    verify(paymentOrderQuery, times(1)).getOrderStatus("order123");
    verify(paymentResponseMessage, times(1)).sendPaymentResponse(any());
  }

  @Test
  @DisplayName("Should return existing FAILED payment without reprocessing")
  void testProcessPayment_DuplicateRequest_FailedStatus_ReturnsExisting() {
    // Arrange
    Payment existingPayment =
        new Payment(UUID.randomUUID(), budgetId, workOrderId, clientId, new BigDecimal("100.00"));
    existingPayment.markAsFailed("Network error");

    when(paymentRepository.findByWorkOrderId(workOrderId)).thenReturn(Optional.of(existingPayment));

    // Act
    Payment result = service.processPayment(paymentRequest);

    // Assert
    assertNotNull(result);
    assertEquals(existingPayment.getId(), result.getId());
    assertEquals(PaymentStatus.FAILED, result.getStatus());
    assertEquals("Network error", result.getErrorMessage());

    verify(paymentRepository, times(1)).findByWorkOrderId(workOrderId);
    verify(paymentGateway, never()).processPixPayment(any(), any(), any());
  }
}
