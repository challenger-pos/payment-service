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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessPaymentService Exception Handling Tests")
class ProcessPaymentServiceExceptionHandlingTest {

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

    paymentRequest = new PaymentRequestDto();
    paymentRequest.setWorkOrderId(workOrderId);
    paymentRequest.setClientId(clientId);
    paymentRequest.setBudgetId(budgetId);
    paymentRequest.setDescription("Test payment");

    PaymentRequestDto.OrderRequest orderRequest = paymentRequest.new OrderRequest();
    orderRequest.setTotalAmount("100.00");
    paymentRequest.setOrderRequest(orderRequest);
  }

  @Test
  @DisplayName("Should mark payment as failed when gateway processing throws exception")
  void testProcessPayment_GatewayException_MarksFailed() {
    // Arrange
    when(paymentRepository.findByWorkOrderId(workOrderId)).thenReturn(Optional.empty());

    Payment savePayment =
        new Payment(UUID.randomUUID(), budgetId, workOrderId, clientId, new BigDecimal("100.00"));

    when(paymentRepository.save(any(Payment.class))).thenReturn(savePayment);

    when(paymentGateway.processPixPayment(any(), any(), any()))
        .thenThrow(new RuntimeException("Gateway unavailable"));

    ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);

    // Act & Assert
    assertThrows(
        PaymentProcessingException.class, () -> service.processPayment(paymentRequest));

    verify(paymentRepository, times(2)).save(paymentCaptor.capture());
    Payment failedPayment = paymentCaptor.getValue();
    assertEquals(PaymentStatus.FAILED, failedPayment.getStatus());
    assertTrue(failedPayment.getErrorMessage().contains("Gateway unavailable"));
  }

  @Test
  @DisplayName(
      "Should use fallback status when query fails with initial response approved")
  void testProcessPayment_QueryFails_FallbackApproved() {
    // Arrange
    when(paymentRepository.findByWorkOrderId(workOrderId)).thenReturn(Optional.empty());

    Payment newPayment =
        new Payment(UUID.randomUUID(), budgetId, workOrderId, clientId, new BigDecimal("100.00"));

    PaymentResponse gatewayResponse =
        new PaymentResponse(
            "ext123", "order123", "pix", PaymentStatus.APPROVED, "qr", "qr64", null);

    when(paymentRepository.save(any(Payment.class))).thenReturn(newPayment);
    when(paymentGateway.processPixPayment(any(), any(), any())).thenReturn(gatewayResponse);

    when(paymentOrderQuery.getOrderStatus(anyString()))
        .thenThrow(new RuntimeException("Query timeout"));

    // Act
    Payment result = service.processPayment(paymentRequest);

    // Assert
    assertNotNull(result);
    assertEquals(PaymentStatus.APPROVED, result.getStatus());
    verify(paymentOrderQuery, times(1)).getOrderStatus(anyString());
    verify(paymentResponseMessage, times(1)).sendPaymentResponse(any());
  }

  @Test
  @DisplayName("Should use fallback status when query fails with initial response rejected")
  void testProcessPayment_QueryFails_FallbackRejected() {
    // Arrange
    when(paymentRepository.findByWorkOrderId(workOrderId)).thenReturn(Optional.empty());

    Payment newPayment =
        new Payment(UUID.randomUUID(), budgetId, workOrderId, clientId, new BigDecimal("100.00"));

    PaymentResponse gatewayResponse =
        new PaymentResponse(
            "ext123", "order123", "pix", PaymentStatus.REJECTED, "qr", "qr64", "Insufficient funds");

    when(paymentRepository.save(any(Payment.class))).thenReturn(newPayment);
    when(paymentGateway.processPixPayment(any(), any(), any())).thenReturn(gatewayResponse);

    when(paymentOrderQuery.getOrderStatus(anyString()))
        .thenThrow(new RuntimeException("Query timeout"));

    // Act
    Payment result = service.processPayment(paymentRequest);

    // Assert
    assertNotNull(result);
    assertEquals(PaymentStatus.REJECTED, result.getStatus());
    assertEquals("Insufficient funds", result.getErrorMessage());
    verify(paymentOrderQuery, times(1)).getOrderStatus(anyString());
    verify(paymentResponseMessage, times(1)).sendPaymentResponse(any());
  }

  @Test
  @DisplayName("Should keep processing status when query returns processing status")
  void testProcessPayment_QueryReturnProcessing_KeepsStatus() {
    // Arrange
    when(paymentRepository.findByWorkOrderId(workOrderId)).thenReturn(Optional.empty());

    Payment newPayment =
        new Payment(UUID.randomUUID(), budgetId, workOrderId, clientId, new BigDecimal("100.00"));

    PaymentResponse gatewayResponse =
        new PaymentResponse(
            "ext123", "order123", "pix", PaymentStatus.PROCESSING, "qr", "qr64", null);

    when(paymentRepository.save(any(Payment.class))).thenReturn(newPayment);
    when(paymentGateway.processPixPayment(any(), any(), any())).thenReturn(gatewayResponse);

    Payment queryResult =
        new Payment(newPayment.getId(), budgetId, workOrderId, clientId, new BigDecimal("100.00"));
    queryResult.markAsProcessing("ext123", "order123", "pix", "qr", "qr64");
    when(paymentOrderQuery.getOrderStatus(any())).thenReturn(queryResult);

    // Act
    Payment result = service.processPayment(paymentRequest);

    // Assert
    assertNotNull(result);
    assertEquals(PaymentStatus.PROCESSING, result.getStatus());
    verify(paymentResponseMessage, times(1)).sendPaymentResponse(any());
  }

  @Test
  @DisplayName("Should handle query returning rejected status with error message")
  void testProcessPayment_QueryReturnRejected_WithErrorMessage() {
    // Arrange
    when(paymentRepository.findByWorkOrderId(workOrderId)).thenReturn(Optional.empty());

    Payment newPayment =
        new Payment(UUID.randomUUID(), budgetId, workOrderId, clientId, new BigDecimal("100.00"));

    PaymentResponse gatewayResponse =
        new PaymentResponse(
            "ext123", "order123", "pix", PaymentStatus.PROCESSING, "qr", "qr64", null);

    when(paymentRepository.save(any(Payment.class))).thenReturn(newPayment);
    when(paymentGateway.processPixPayment(any(), any(), any())).thenReturn(gatewayResponse);

    Payment queryResult =
        new Payment(newPayment.getId(), budgetId, workOrderId, clientId, new BigDecimal("100.00"));
    queryResult.markAsRejected("Payment limit exceeded");
    when(paymentOrderQuery.getOrderStatus(any())).thenReturn(queryResult);

    // Act
    Payment result = service.processPayment(paymentRequest);

    // Assert
    assertNotNull(result);
    assertEquals(PaymentStatus.REJECTED, result.getStatus());
    assertEquals("Payment limit exceeded", result.getErrorMessage());
    verify(paymentResponseMessage, times(1)).sendPaymentResponse(any());
  }

  @Test
  @DisplayName("Should handle query returning approved status successfully")
  void testProcessPayment_QueryReturnApproved_Success() {
    // Arrange
    when(paymentRepository.findByWorkOrderId(workOrderId)).thenReturn(Optional.empty());

    Payment newPayment =
        new Payment(UUID.randomUUID(), budgetId, workOrderId, clientId, new BigDecimal("100.00"));

    PaymentResponse gatewayResponse =
        new PaymentResponse(
            "ext123", "order123", "pix", PaymentStatus.PROCESSING, "qr", "qr64", null);

    when(paymentRepository.save(any(Payment.class))).thenReturn(newPayment);
    when(paymentGateway.processPixPayment(any(), any(), any())).thenReturn(gatewayResponse);

    Payment queryResult =
        new Payment(newPayment.getId(), budgetId, workOrderId, clientId, new BigDecimal("100.00"));
    queryResult.markAsApproved();
    when(paymentOrderQuery.getOrderStatus(any())).thenReturn(queryResult);

    // Act
    Payment result = service.processPayment(paymentRequest);

    // Assert
    assertNotNull(result);
    assertEquals(PaymentStatus.APPROVED, result.getStatus());
    verify(paymentRepository, atLeast(2)).save(any());
    verify(paymentOrderQuery, times(1)).getOrderStatus(anyString());
  }

  @Test
  @DisplayName("Should handle null description by using default message")
  void testProcessPayment_NullDescription_UsesDefault() {
    // Arrange
    paymentRequest.setDescription(null);

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

    ArgumentCaptor<String> descriptionCaptor = ArgumentCaptor.forClass(String.class);

    // Act
    Payment result = service.processPayment(paymentRequest);

    // Assert
    assertNotNull(result);
    verify(paymentGateway).processPixPayment(any(), any(), descriptionCaptor.capture());
    assertTrue(descriptionCaptor.getValue().contains("Payment for order"));
  }

  @Test
  @DisplayName("Should complete successfully when message queue send succeeds")
  void testProcessPayment_MessageQueueSuccess() {
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
    ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
    verify(paymentResponseMessage).sendPaymentResponse(paymentCaptor.capture());
    assertEquals(PaymentStatus.APPROVED, paymentCaptor.getValue().getStatus());
  }
}
