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
@DisplayName("ProcessPaymentService Success Path Tests")
class ProcessPaymentServiceSuccessTest {

  @Mock private PaymentRepositoryPort paymentRepository;
  @Mock private PaymentGatewayPort paymentGateway;
  @Mock private PaymentOrderQueryPort paymentOrderQuery;
  @Mock private PaymentResponseMessagePort paymentResponseMessage;

  private ProcessPaymentService service;

  private UUID workOrderId;
  private UUID customerId;
  private UUID paymentId;
  private PaymentRequestDto paymentRequest;

  @BeforeEach
  void setUp() {
    service =
        new ProcessPaymentService(
            paymentRepository, paymentGateway, paymentOrderQuery, paymentResponseMessage);

    workOrderId = UUID.randomUUID();
    customerId = UUID.randomUUID();
    paymentId = UUID.randomUUID();

    paymentRequest = new PaymentRequestDto();
    paymentRequest.setWorkOrderId(workOrderId);
    paymentRequest.setCustomerId(customerId);
    paymentRequest.setDescription("Test payment");
    paymentRequest.setFirstName("John");

    PaymentRequestDto.OrderRequest orderRequest = paymentRequest.new OrderRequest();
    orderRequest.setTotalAmount("100.00");
    paymentRequest.setOrderRequest(orderRequest);
  }

  @Test
  @DisplayName("Should process payment successfully with APPROVED status directly from gateway")
  void testProcessPayment_ApprovedDirectly_Success() {
    // Arrange
    when(paymentRepository.findByWorkOrderId(workOrderId)).thenReturn(Optional.empty());

    Payment createdPayment =
        new Payment(paymentId, workOrderId, customerId, new BigDecimal("100.00"));

    PaymentResponse gatewayResponse =
        new PaymentResponse(
            "ext-payment-123",
            "order-123",
            "pix",
            PaymentStatus.APPROVED,
            "qr-code-data",
            "qr-code-base64",
            null);

    when(paymentRepository.save(any(Payment.class))).thenReturn(createdPayment);
    when(paymentGateway.processPixPayment(any(), any(), any(), any()))
        .thenReturn(gatewayResponse);

    Payment queryResult =
        new Payment(paymentId, workOrderId, customerId, new BigDecimal("100.00"));
    queryResult.markAsApproved();
    when(paymentOrderQuery.getOrderStatus(anyString())).thenReturn(queryResult);

    // Act
    Payment result = service.processPayment(paymentRequest);

    // Assert
    assertNotNull(result);
    assertEquals(paymentId, result.getId());
    assertEquals(workOrderId, result.getWorkOrderId());
    assertEquals(customerId, result.getCustomerId());
    assertEquals(PaymentStatus.APPROVED, result.getStatus());
    assertNull(result.getErrorMessage());

    // Verify interactions
    verify(paymentRepository, times(1)).findByWorkOrderId(workOrderId);
    verify(paymentRepository, times(2)).save(any(Payment.class));
    verify(paymentGateway, times(1)).processPixPayment(any(), any(), any(), any());
    verify(paymentOrderQuery, times(1)).getOrderStatus("order-123");
    verify(paymentResponseMessage, times(1)).sendPaymentResponse(any(Payment.class));
  }

  @Test
  @DisplayName("Should process payment successfully with PROCESSING status")
  void testProcessPayment_Processing_Success() {
    // Arrange
    when(paymentRepository.findByWorkOrderId(workOrderId)).thenReturn(Optional.empty());

    Payment createdPayment =
        new Payment(paymentId, workOrderId, customerId, new BigDecimal("100.00"));

    PaymentResponse gatewayResponse =
        new PaymentResponse(
            "ext-payment-456",
            "order-456",
            "pix",
            PaymentStatus.PROCESSING,
            "qr-code-data",
            "qr-code-base64",
            null);

    when(paymentRepository.save(any(Payment.class))).thenReturn(createdPayment);
    when(paymentGateway.processPixPayment(any(), any(), any(), any()))
        .thenReturn(gatewayResponse);

    Payment queryResult =
        new Payment(paymentId, workOrderId, customerId, new BigDecimal("100.00"));
    queryResult.markAsProcessing(
        "ext-payment-456", "order-456", "pix", "qr-code-data", "qr-code-base64");
    when(paymentOrderQuery.getOrderStatus(anyString())).thenReturn(queryResult);

    // Act
    Payment result = service.processPayment(paymentRequest);

    // Assert
    assertNotNull(result);
    assertEquals(PaymentStatus.PROCESSING, result.getStatus());
    assertNull(result.getErrorMessage());
    assertEquals("ext-payment-456", result.getExternalPaymentId());
    assertEquals("order-456", result.getOrderPaymentId());

    // Verify interactions
    verify(paymentRepository, times(2)).save(any(Payment.class));
    verify(paymentGateway, times(1)).processPixPayment(any(), any(), any(), any());
    verify(paymentOrderQuery, times(1)).getOrderStatus("order-456");
    verify(paymentResponseMessage, times(1)).sendPaymentResponse(any(Payment.class));
  }

  @Test
  @DisplayName("Should process payment with REJECTED status and error message")
  void testProcessPayment_Rejected_Success() {
    // Arrange
    when(paymentRepository.findByWorkOrderId(workOrderId)).thenReturn(Optional.empty());

    Payment createdPayment =
        new Payment(paymentId, workOrderId, customerId, new BigDecimal("100.00"));

    PaymentResponse gatewayResponse =
        new PaymentResponse(
            "ext-payment-789",
            "order-789",
            "pix",
            PaymentStatus.REJECTED,
            "qr-code-data",
            "qr-code-base64",
            "Insufficient funds");

    when(paymentRepository.save(any(Payment.class))).thenReturn(createdPayment);
    when(paymentGateway.processPixPayment(any(), any(), any(), any()))
        .thenReturn(gatewayResponse);

    Payment queryResult =
        new Payment(paymentId, workOrderId, customerId, new BigDecimal("100.00"));
    queryResult.markAsRejected("Insufficient funds");
    when(paymentOrderQuery.getOrderStatus(anyString())).thenReturn(queryResult);

    // Act
    Payment result = service.processPayment(paymentRequest);

    // Assert
    assertNotNull(result);
    assertEquals(PaymentStatus.REJECTED, result.getStatus());
    assertEquals("Insufficient funds", result.getErrorMessage());

    // Verify interactions
    verify(paymentRepository, times(2)).save(any(Payment.class));
    verify(paymentGateway, times(1)).processPixPayment(any(), any(), any(), any());
    verify(paymentOrderQuery, times(1)).getOrderStatus("order-789");
    verify(paymentResponseMessage, times(1)).sendPaymentResponse(any(Payment.class));
  }

  @Test
  @DisplayName("Should handle idempotency: return existing APPROVED payment for duplicate request")
  void testProcessPayment_Idempotency_ExistingApproved() {
    // Arrange
    Payment existingPayment =
        new Payment(paymentId, workOrderId, customerId, new BigDecimal("100.00"));
    existingPayment.markAsApproved();

    when(paymentRepository.findByWorkOrderId(workOrderId))
        .thenReturn(Optional.of(existingPayment));

    // Act
    Payment result = service.processPayment(paymentRequest);

    // Assert
    assertNotNull(result);
    assertEquals(paymentId, result.getId());
    assertEquals(PaymentStatus.APPROVED, result.getStatus());

    // Verify that gateway was NOT called (idempotency achieved)
    verify(paymentRepository, times(1)).findByWorkOrderId(workOrderId);
    verify(paymentGateway, never()).processPixPayment(any(), any(), any(), any());
    verify(paymentOrderQuery, never()).getOrderStatus(anyString());
    verify(paymentResponseMessage, never()).sendPaymentResponse(any());
  }

  @Test
  @DisplayName("Should handle idempotency: return existing REJECTED payment for duplicate request")
  void testProcessPayment_Idempotency_ExistingRejected() {
    // Arrange
    Payment existingPayment =
        new Payment(paymentId, workOrderId, customerId, new BigDecimal("100.00"));
    existingPayment.markAsRejected("Card declined");

    when(paymentRepository.findByWorkOrderId(workOrderId))
        .thenReturn(Optional.of(existingPayment));

    // Act
    Payment result = service.processPayment(paymentRequest);

    // Assert
    assertNotNull(result);
    assertEquals(paymentId, result.getId());
    assertEquals(PaymentStatus.REJECTED, result.getStatus());
    assertEquals("Card declined", result.getErrorMessage());

    // Verify that gateway was NOT called
    verify(paymentGateway, never()).processPixPayment(any(), any(), any(), any());
    verify(paymentResponseMessage, never()).sendPaymentResponse(any());
  }

  @Test
  @DisplayName("Should handle idempotency: continue processing PENDING payment (retry scenario)")
  void testProcessPayment_Idempotency_PendingRetry() {
    // Arrange
    Payment pendingPayment =
        new Payment(paymentId, workOrderId, customerId, new BigDecimal("100.00"));
    // Payment is still PENDING (initial state, no markAs* call)

    when(paymentRepository.findByWorkOrderId(workOrderId))
        .thenReturn(Optional.of(pendingPayment));

    PaymentResponse gatewayResponse =
        new PaymentResponse(
            "ext-payment-retry",
            "order-retry",
            "pix",
            PaymentStatus.APPROVED,
            "qr-code-data",
            "qr-code-base64",
            null);

    when(paymentRepository.save(any(Payment.class))).thenReturn(pendingPayment);
    when(paymentGateway.processPixPayment(any(), any(), any(), any()))
        .thenReturn(gatewayResponse);

    Payment queryResult =
        new Payment(paymentId, workOrderId, customerId, new BigDecimal("100.00"));
    queryResult.markAsApproved();
    when(paymentOrderQuery.getOrderStatus(anyString())).thenReturn(queryResult);

    // Act
    Payment result = service.processPayment(paymentRequest);

    // Assert
    assertNotNull(result);
    assertEquals(PaymentStatus.APPROVED, result.getStatus());

    // Verify that gateway WAS called (not returning early)
    verify(paymentGateway, times(1)).processPixPayment(any(), any(), any(), any());
    verify(paymentResponseMessage, times(1)).sendPaymentResponse(any());
  }

  @Test
  @DisplayName("Should correctly set payment attributes from gateway response")
  void testProcessPayment_GatewayResponseMapped_Success() {
    // Arrange
    when(paymentRepository.findByWorkOrderId(workOrderId)).thenReturn(Optional.empty());

    Payment createdPayment =
        new Payment(paymentId, workOrderId, customerId, new BigDecimal("100.00"));

    PaymentResponse gatewayResponse =
        new PaymentResponse(
            "external-payment-id-xyz",
            "order-payment-id-abc",
            "pix",
            PaymentStatus.APPROVED,
            "00020126580014br.gov.bcb.pix...",
            "iVBORw0KGgoAAAANSUhEUgAAAAUA...",
            null);

    when(paymentRepository.save(any(Payment.class))).thenReturn(createdPayment);
    when(paymentGateway.processPixPayment(any(), any(), any(), any()))
        .thenReturn(gatewayResponse);

    Payment queryResult =
        new Payment(paymentId, workOrderId, customerId, new BigDecimal("100.00"));
    queryResult.markAsApproved();
    when(paymentOrderQuery.getOrderStatus(anyString())).thenReturn(queryResult);

    // Act
    Payment result = service.processPayment(paymentRequest);

    // Assert
    assertEquals("external-payment-id-xyz", result.getExternalPaymentId());
    assertEquals("order-payment-id-abc", result.getOrderPaymentId());
    assertEquals("pix", result.getPaymentMethod());
    assertEquals("00020126580014br.gov.bcb.pix...", result.getQrCode());
    assertEquals("iVBORw0KGgoAAAANSUhEUgAAAAUA...", result.getQrCodeBase64());
  }

  @Test
  @DisplayName("Should use default description when payment request has no description")
  void testProcessPayment_DefaultDescription_Success() {
    // Arrange
    paymentRequest.setDescription(null); // No description provided

    when(paymentRepository.findByWorkOrderId(workOrderId)).thenReturn(Optional.empty());

    Payment createdPayment =
        new Payment(paymentId, workOrderId, customerId, new BigDecimal("100.00"));

    PaymentResponse gatewayResponse =
        new PaymentResponse(
            "ext-123", "order-123", "pix", PaymentStatus.APPROVED, "qr", "qr64", null);

    when(paymentRepository.save(any(Payment.class))).thenReturn(createdPayment);
    when(paymentGateway.processPixPayment(any(), any(), any(), any()))
        .thenReturn(gatewayResponse);

    Payment queryResult =
        new Payment(paymentId, workOrderId, customerId, new BigDecimal("100.00"));
    queryResult.markAsApproved();
    when(paymentOrderQuery.getOrderStatus(anyString())).thenReturn(queryResult);

    ArgumentCaptor<String> descriptionCaptor = ArgumentCaptor.forClass(String.class);

    // Act
    Payment result = service.processPayment(paymentRequest);

    // Assert
    assertNotNull(result);
    verify(paymentGateway, times(1))
        .processPixPayment(
            any(), any(), descriptionCaptor.capture(), eq("John"));

    String capturedDescription = descriptionCaptor.getValue();
    assertTrue(
        capturedDescription.contains("Payment for order")
            && capturedDescription.contains(workOrderId.toString()));
  }

  @Test
  @DisplayName("Should send payment response message after successful processing")
  void testProcessPayment_ResponseMessageSent_Success() {
    // Arrange
    when(paymentRepository.findByWorkOrderId(workOrderId)).thenReturn(Optional.empty());

    Payment createdPayment =
        new Payment(paymentId, workOrderId, customerId, new BigDecimal("100.00"));

    PaymentResponse gatewayResponse =
        new PaymentResponse(
            "ext-123", "order-123", "pix", PaymentStatus.APPROVED, "qr", "qr64", null);

    when(paymentRepository.save(any(Payment.class))).thenReturn(createdPayment);
    when(paymentGateway.processPixPayment(any(), any(), any(), any()))
        .thenReturn(gatewayResponse);

    Payment queryResult =
        new Payment(paymentId, workOrderId, customerId, new BigDecimal("100.00"));
    queryResult.markAsApproved();
    when(paymentOrderQuery.getOrderStatus(anyString())).thenReturn(queryResult);

    ArgumentCaptor<Payment> messageCaptor = ArgumentCaptor.forClass(Payment.class);

    // Act
    Payment result = service.processPayment(paymentRequest);

    // Assert
    verify(paymentResponseMessage, times(1)).sendPaymentResponse(messageCaptor.capture());
    Payment sentPayment = messageCaptor.getValue();

    assertEquals(paymentId, sentPayment.getId());
    assertEquals(PaymentStatus.APPROVED, sentPayment.getStatus());
    assertNull(sentPayment.getErrorMessage());
  }
}
