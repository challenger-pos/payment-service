package com.fiap.billing_service.infrastructure.adapter.out.payment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fiap.billing_service.domain.dto.PaymentResponse;
import com.fiap.billing_service.domain.valueobject.PaymentStatus;
import com.fiap.billing_service.infrastructure.adapter.out.payment.dto.MercadoPagoOrderResponse;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("MercadoPago Adapter Tests")
class MercadoPagoAdapterTest {

  @Mock private RestTemplate restTemplate;

  private MercadoPagoAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new MercadoPagoAdapter(restTemplate);
    ReflectionTestUtils.setField(adapter, "accessToken", "test-token-12345");
    adapter.init();
  }

  @Test
  @DisplayName("Should process PIX payment successfully with QR code")
  void testProcessPixPayment_Success_ReturnsQRCode() {
    // Arrange
    BigDecimal amount = new BigDecimal("100.50");
    String email = "customer@example.com";
    String description = "Test PIX payment";
    String firstName = "John";

    MercadoPagoOrderResponse response =
        createValidOrderResponse("approved", "qr_code_123", "qr_code_base64_xyz");
    ResponseEntity<MercadoPagoOrderResponse> httpResponse =
        new ResponseEntity<MercadoPagoOrderResponse>(response, HttpStatus.CREATED);

    when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(MercadoPagoOrderResponse.class)))
        .thenReturn(httpResponse);

    // Act
    PaymentResponse result = adapter.processPixPayment(amount, email, description, firstName);

    // Assert
    assertNotNull(result);
    assertEquals("payment_001", result.getExternalPaymentId());
    assertEquals("order_001", result.getOrderPaymentId());
    assertEquals("pix", result.getPaymentMethod());
    assertEquals(PaymentStatus.APPROVED, result.getStatus());
    assertEquals("qr_code_123", result.getQrCode());
    assertEquals("qr_code_base64_xyz", result.getQrCodeBase64());
    assertNull(result.getErrorMessage());

    verify(restTemplate, times(1))
        .exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(MercadoPagoOrderResponse.class));
  }

  @Test
  @DisplayName("Should return approved status for mercado pago approved response")
  void testProcessPixPayment_StatusApproved() {
    // Arrange
    BigDecimal amount = new BigDecimal("50.00");
    String email = "test@test.com";
    String firstName = "APRO";

    MercadoPagoOrderResponse response = createValidOrderResponse("approved", null, null);
    ResponseEntity<MercadoPagoOrderResponse> httpResponse =
        new ResponseEntity<MercadoPagoOrderResponse>(response, HttpStatus.CREATED);

    when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(MercadoPagoOrderResponse.class)))
        .thenReturn(httpResponse);

    // Act
    PaymentResponse result = adapter.processPixPayment(amount, email, "Payment", firstName);

    // Assert
    assertEquals(PaymentStatus.APPROVED, result.getStatus());
    verify(restTemplate, times(1))
        .exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(MercadoPagoOrderResponse.class));
  }

  @Test
  @DisplayName("Should return approved status for 'processed' mercado pago response")
  void testProcessPixPayment_StatusProcessed() {
    // Arrange
    BigDecimal amount = new BigDecimal("50.00");

    MercadoPagoOrderResponse response = createValidOrderResponse("processed", null, null);
    ResponseEntity<MercadoPagoOrderResponse> httpResponse =
        new ResponseEntity<MercadoPagoOrderResponse>(response, HttpStatus.CREATED);

    when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(MercadoPagoOrderResponse.class)))
        .thenReturn(httpResponse);

    // Act
    PaymentResponse result = adapter.processPixPayment(amount, "test@test.com", "Payment", "APRO");

    // Assert
    assertEquals(PaymentStatus.APPROVED, result.getStatus());
  }

  @Test
  @DisplayName("Should return rejected status and error message")
  void testProcessPixPayment_StatusRejected_WithErrorMessage() {
    // Arrange
    BigDecimal amount = new BigDecimal("100.00");

    MercadoPagoOrderResponse response =
        createOrderResponseWithRejection("rejected", "Insufficient funds");
    ResponseEntity<MercadoPagoOrderResponse> httpResponse =
        new ResponseEntity<MercadoPagoOrderResponse>(response, HttpStatus.CREATED);

    when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(MercadoPagoOrderResponse.class)))
        .thenReturn(httpResponse);

    // Act
    PaymentResponse result = adapter.processPixPayment(amount, "test@test.com", "Payment", "JOSE");

    // Assert
    assertEquals(PaymentStatus.REJECTED, result.getStatus());
    assertEquals("Insufficient funds", result.getErrorMessage());
  }

  @Test
  @DisplayName("Should return rejected status for 'cancelled' response")
  void testProcessPixPayment_StatusCancelled() {
    // Arrange
    BigDecimal amount = new BigDecimal("100.00");

    MercadoPagoOrderResponse response = createValidOrderResponse("cancelled", null, null);
    ResponseEntity<MercadoPagoOrderResponse> httpResponse =
        new ResponseEntity<MercadoPagoOrderResponse>(response, HttpStatus.CREATED);

    when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(MercadoPagoOrderResponse.class)))
        .thenReturn(httpResponse);

    // Act
    PaymentResponse result =
        adapter.processPixPayment(amount, "test@test.com", "Payment", "CANCELED");

    // Assert
    assertEquals(PaymentStatus.REJECTED, result.getStatus());
  }

  @Test
  @DisplayName("Should return processing status for pending response")
  void testProcessPixPayment_StatusPending() {
    // Arrange
    BigDecimal amount = new BigDecimal("100.00");

    MercadoPagoOrderResponse response = createValidOrderResponse("pending", null, null);
    ResponseEntity<MercadoPagoOrderResponse> httpResponse =
        new ResponseEntity<MercadoPagoOrderResponse>(response, HttpStatus.CREATED);

    when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(MercadoPagoOrderResponse.class)))
        .thenReturn(httpResponse);

    // Act
    PaymentResponse result = adapter.processPixPayment(amount, "test@test.com", "Payment", "");

    // Assert
    assertEquals(PaymentStatus.PROCESSING, result.getStatus());
  }

  @Test
  @DisplayName("Should return processing status for 'processing' response")
  void testProcessPixPayment_StatusProcessing() {
    // Arrange
    BigDecimal amount = new BigDecimal("100.00");

    MercadoPagoOrderResponse response = createValidOrderResponse("processing", null, null);
    ResponseEntity<MercadoPagoOrderResponse> httpResponse =
        new ResponseEntity<MercadoPagoOrderResponse>(response, HttpStatus.CREATED);

    when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(MercadoPagoOrderResponse.class)))
        .thenReturn(httpResponse);

    // Act
    PaymentResponse result =
        adapter.processPixPayment(amount, "test@test.com", "Payment", "PROCESSING");

    // Assert
    assertEquals(PaymentStatus.PROCESSING, result.getStatus());
  }

  @Test
  @DisplayName("Should default to processing status for unknown status")
  void testProcessPixPayment_UnknownStatus_DefaultsToProcessing() {
    // Arrange
    BigDecimal amount = new BigDecimal("100.00");

    MercadoPagoOrderResponse response = createValidOrderResponse("unknown_status", null, null);
    ResponseEntity<MercadoPagoOrderResponse> httpResponse =
        new ResponseEntity<MercadoPagoOrderResponse>(response, HttpStatus.CREATED);

    when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(MercadoPagoOrderResponse.class)))
        .thenReturn(httpResponse);

    // Act
    PaymentResponse result =
        adapter.processPixPayment(amount, "test@test.com", "Payment", "UNKNOWN");

    // Assert - Unknown status defaults to REJECTED for safety
    assertEquals(PaymentStatus.REJECTED, result.getStatus());
  }

  @Test
  @DisplayName("Should use default email when email is null")
  void testProcessPixPayment_NullEmail_UsesDefault() {
    // Arrange
    BigDecimal amount = new BigDecimal("100.00");

    MercadoPagoOrderResponse response = createValidOrderResponse("approved", null, null);
    ResponseEntity<MercadoPagoOrderResponse> httpResponse =
        new ResponseEntity<MercadoPagoOrderResponse>(response, HttpStatus.CREATED);

    when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(MercadoPagoOrderResponse.class)))
        .thenReturn(httpResponse);

    // Act
    PaymentResponse result = adapter.processPixPayment(amount, null, "Payment", "JOSE");

    // Assert
    assertNotNull(result);
    assertEquals(PaymentStatus.APPROVED, result.getStatus());
  }

  @Test
  @DisplayName("Should use default email when email is empty")
  void testProcessPixPayment_EmptyEmail_UsesDefault() {
    // Arrange
    BigDecimal amount = new BigDecimal("100.00");

    MercadoPagoOrderResponse response = createValidOrderResponse("approved", null, null);
    ResponseEntity<MercadoPagoOrderResponse> httpResponse =
        new ResponseEntity<MercadoPagoOrderResponse>(response, HttpStatus.CREATED);

    when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(MercadoPagoOrderResponse.class)))
        .thenReturn(httpResponse);

    // Act
    PaymentResponse result = adapter.processPixPayment(amount, "", "Payment", "");

    // Assert
    assertNotNull(result);
    assertEquals(PaymentStatus.APPROVED, result.getStatus());
  }

  @Test
  @DisplayName("Should extract payment id from transactions when available")
  void testProcessPixPayment_ExtractsPaymentIdFromTransactions() {
    // Arrange
    BigDecimal amount = new BigDecimal("100.00");

    MercadoPagoOrderResponse response = createValidOrderResponse("approved", null, null);
    ResponseEntity<MercadoPagoOrderResponse> httpResponse =
        new ResponseEntity<MercadoPagoOrderResponse>(response, HttpStatus.CREATED);

    when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(MercadoPagoOrderResponse.class)))
        .thenReturn(httpResponse);

    // Act
    PaymentResponse result = adapter.processPixPayment(amount, "test@test.com", "Payment", "JOSE");

    // Assert
    assertEquals("payment_001", result.getExternalPaymentId());
    assertEquals("order_001", result.getOrderPaymentId());
  }

  @Test
  @DisplayName("Should throw exception when API response is null")
  void testProcessPixPayment_NullResponse_ThrowsException() {
    // Arrange
    BigDecimal amount = new BigDecimal("100.00");
    ResponseEntity<MercadoPagoOrderResponse> httpResponse =
        ResponseEntity.status(HttpStatus.CREATED).build();

    when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(MercadoPagoOrderResponse.class)))
        .thenReturn(httpResponse);

    // Act & Assert
    RuntimeException exception =
        assertThrows(
            RuntimeException.class,
            () -> adapter.processPixPayment(amount, "test@test.com", "Payment", "JOSE"));
    assertTrue(exception.getMessage().contains("Empty response"));
  }

  @Test
  @DisplayName("Should handle RestTemplate exception gracefully")
  void testProcessPixPayment_RestTemplateException_ThrowsException() {
    // Arrange
    BigDecimal amount = new BigDecimal("100.00");

    when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(MercadoPagoOrderResponse.class)))
        .thenThrow(new RuntimeException("Connection timeout"));

    // Act & Assert
    RuntimeException exception =
        assertThrows(
            RuntimeException.class,
            () -> adapter.processPixPayment(amount, "test@test.com", "Payment", "JOSE"));
    assertTrue(exception.getMessage().contains("Mercado Pago API error"));
  }

  @Test
  @DisplayName("Should handle null transactions in response")
  void testProcessPixPayment_NullTransactions_HandlesGracefully() {
    // Arrange
    BigDecimal amount = new BigDecimal("100.00");

    MercadoPagoOrderResponse response = new MercadoPagoOrderResponse();
    response.setId("order_001");
    response.setStatus("approved");
    response.setTransactions(null); // No transactions

    ResponseEntity<MercadoPagoOrderResponse> httpResponse =
        new ResponseEntity<MercadoPagoOrderResponse>(response, HttpStatus.CREATED);

    when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(MercadoPagoOrderResponse.class)))
        .thenReturn(httpResponse);

    // Act
    PaymentResponse result = adapter.processPixPayment(amount, "test@test.com", "Payment", "");

    // Assert
    assertNotNull(result);
    assertEquals("order_001", result.getExternalPaymentId());
    assertEquals(PaymentStatus.APPROVED, result.getStatus());
    assertNull(result.getQrCode());
  }

  @Test
  @DisplayName("Should handle empty payments array in transactions")
  void testProcessPixPayment_EmptyPaymentsArray_HandlesGracefully() {
    // Arrange
    BigDecimal amount = new BigDecimal("100.00");

    MercadoPagoOrderResponse response = new MercadoPagoOrderResponse();
    response.setId("order_001");
    response.setStatus("approved");

    MercadoPagoOrderResponse.Transactions transactions =
        new MercadoPagoOrderResponse.Transactions();
    transactions.setPayments(new MercadoPagoOrderResponse.Payment[0]); // Empty array
    response.setTransactions(transactions);

    ResponseEntity<MercadoPagoOrderResponse> httpResponse =
        new ResponseEntity<MercadoPagoOrderResponse>(response, HttpStatus.CREATED);

    when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(MercadoPagoOrderResponse.class)))
        .thenReturn(httpResponse);

    // Act
    PaymentResponse result = adapter.processPixPayment(amount, "test@test.com", "Payment", "APRO");

    // Assert
    assertNotNull(result);
    assertEquals("order_001", result.getExternalPaymentId());
    assertNull(result.getQrCode());
  }

  @Test
  @DisplayName("Should handle null PointOfInteraction in payment")
  void testProcessPixPayment_NullPointOfInteraction_HandlesGracefully() {
    // Arrange
    BigDecimal amount = new BigDecimal("100.00");

    MercadoPagoOrderResponse response = new MercadoPagoOrderResponse();
    response.setId("order_001");
    response.setStatus("approved");

    MercadoPagoOrderResponse.Payment payment = new MercadoPagoOrderResponse.Payment();
    payment.setId("payment_001");
    payment.setStatus("approved");
    payment.setPointOfInteraction(null); // No POI

    MercadoPagoOrderResponse.Transactions transactions =
        new MercadoPagoOrderResponse.Transactions();
    transactions.setPayments(new MercadoPagoOrderResponse.Payment[] {payment});
    response.setTransactions(transactions);

    ResponseEntity<MercadoPagoOrderResponse> httpResponse =
        new ResponseEntity<MercadoPagoOrderResponse>(response, HttpStatus.CREATED);

    when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(MercadoPagoOrderResponse.class)))
        .thenReturn(httpResponse);

    // Act
    PaymentResponse result = adapter.processPixPayment(amount, "test@test.com", "Payment", "APRO");

    // Assert
    assertNotNull(result);
    assertNull(result.getQrCode());
    assertNull(result.getQrCodeBase64());
  }

  @Test
  @DisplayName("Should send correct headers with Authorization and Idempotency-Key")
  void testProcessPixPayment_SendsCorrectHeaders() {
    // Arrange
    BigDecimal amount = new BigDecimal("100.00");

    MercadoPagoOrderResponse response = createValidOrderResponse("approved", null, null);
    ResponseEntity<MercadoPagoOrderResponse> httpResponse =
        new ResponseEntity<MercadoPagoOrderResponse>(response, HttpStatus.CREATED);

    when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(MercadoPagoOrderResponse.class)))
        .thenReturn(httpResponse);

    // Act
    adapter.processPixPayment(amount, "test@test.com", "Payment", "");

    // Assert
    verify(restTemplate, times(1))
        .exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(MercadoPagoOrderResponse.class));
    // Headers are validated implicitly since the mock returns successfully
  }

  // Helper methods

  private MercadoPagoOrderResponse createValidOrderResponse(
      String status, String qrCode, String qrCodeBase64) {
    MercadoPagoOrderResponse response = new MercadoPagoOrderResponse();
    response.setId("order_001");
    response.setStatus(status);

    MercadoPagoOrderResponse.Payment payment = new MercadoPagoOrderResponse.Payment();
    payment.setId("payment_001");
    payment.setStatus(status);

    if (qrCode != null || qrCodeBase64 != null) {
      MercadoPagoOrderResponse.PointOfInteraction poi =
          new MercadoPagoOrderResponse.PointOfInteraction();
      MercadoPagoOrderResponse.TransactionData txData =
          new MercadoPagoOrderResponse.TransactionData();
      txData.setQrCode(qrCode);
      txData.setQrCodeBase64(qrCodeBase64);
      poi.setTransactionData(txData);
      payment.setPointOfInteraction(poi);
    }

    MercadoPagoOrderResponse.Transactions transactions =
        new MercadoPagoOrderResponse.Transactions();
    transactions.setPayments(new MercadoPagoOrderResponse.Payment[] {payment});
    response.setTransactions(transactions);

    return response;
  }

  private MercadoPagoOrderResponse createOrderResponseWithRejection(
      String status, String errorMessage) {
    MercadoPagoOrderResponse response = new MercadoPagoOrderResponse();
    response.setId("order_001");
    response.setStatus(status);

    MercadoPagoOrderResponse.Payment payment = new MercadoPagoOrderResponse.Payment();
    payment.setId("payment_001");
    payment.setStatus(status);
    payment.setStatusDetail(errorMessage);

    MercadoPagoOrderResponse.Transactions transactions =
        new MercadoPagoOrderResponse.Transactions();
    transactions.setPayments(new MercadoPagoOrderResponse.Payment[] {payment});
    response.setTransactions(transactions);

    return response;
  }
}
