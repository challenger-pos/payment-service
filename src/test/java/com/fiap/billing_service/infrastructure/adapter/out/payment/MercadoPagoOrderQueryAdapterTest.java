package com.fiap.billing_service.infrastructure.adapter.out.payment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fiap.billing_service.domain.entity.Payment;
import com.fiap.billing_service.domain.exception.PaymentProcessingException;
import com.fiap.billing_service.domain.valueobject.PaymentStatus;
import com.fiap.billing_service.infrastructure.adapter.out.payment.dto.MercadoPagoOrderResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("MercadoPago Order Query Adapter Tests")
class MercadoPagoOrderQueryAdapterTest {

  @Mock private RestTemplate restTemplate;

  private MercadoPagoOrderQueryAdapter adapter;

  private static final String ACCESS_TOKEN = "test-access-token-12345";
  private static final String ORDER_ID = "order_test_12345";
  private static final String ORDERS_API_URL = "https://api.mercadopago.com/v1/orders";

  @BeforeEach
  void setUp() {
    adapter = new MercadoPagoOrderQueryAdapter(restTemplate);
    ReflectionTestUtils.setField(adapter, "accessToken", ACCESS_TOKEN);
  }

  @Test
  @DisplayName("Should retrieve approved order status successfully")
  void testGetOrderStatusApproved() {
    // Arrange
    MercadoPagoOrderResponse response = createApprovedOrderResponse();
    ResponseEntity<MercadoPagoOrderResponse> responseEntity =
        new ResponseEntity<>(response, HttpStatus.OK);

    when(restTemplate.exchange(
            eq(ORDERS_API_URL + "/" + ORDER_ID),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(MercadoPagoOrderResponse.class)))
        .thenReturn(responseEntity);

    // Act
    Payment result = adapter.getOrderStatus(ORDER_ID);

    // Assert
    assertNotNull(result);
    assertEquals(PaymentStatus.APPROVED, result.getStatus());
    verify(restTemplate, times(1))
        .exchange(
            eq(ORDERS_API_URL + "/" + ORDER_ID),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(MercadoPagoOrderResponse.class));
  }

  @Test
  @DisplayName("Should retrieve rejected order status with error message")
  void testGetOrderStatusRejected() {
    // Arrange
    MercadoPagoOrderResponse response = createRejectedOrderResponse();
    ResponseEntity<MercadoPagoOrderResponse> responseEntity =
        new ResponseEntity<>(response, HttpStatus.OK);

    when(restTemplate.exchange(
            eq(ORDERS_API_URL + "/" + ORDER_ID),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(MercadoPagoOrderResponse.class)))
        .thenReturn(responseEntity);

    // Act
    Payment result = adapter.getOrderStatus(ORDER_ID);

    // Assert
    assertNotNull(result);
    assertEquals(PaymentStatus.REJECTED, result.getStatus());
    assertNotNull(result.getErrorMessage());
  }

  @Test
  @DisplayName("Should retrieve processing order status")
  void testGetOrderStatusProcessing() {
    // Arrange
    MercadoPagoOrderResponse response = createProcessingOrderResponse();
    ResponseEntity<MercadoPagoOrderResponse> responseEntity =
        new ResponseEntity<>(response, HttpStatus.OK);

    when(restTemplate.exchange(
            eq(ORDERS_API_URL + "/" + ORDER_ID),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(MercadoPagoOrderResponse.class)))
        .thenReturn(responseEntity);

    // Act
    Payment result = adapter.getOrderStatus(ORDER_ID);

    // Assert
    assertNotNull(result);
    assertEquals(PaymentStatus.PROCESSING, result.getStatus());
  }

  @Test
  @DisplayName("Should throw PaymentProcessingException when order not found (404)")
  void testGetOrderStatusNotFound() {
    // Arrange
    when(restTemplate.exchange(
            eq(ORDERS_API_URL + "/" + ORDER_ID),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(MercadoPagoOrderResponse.class)))
        .thenThrow(
            new HttpClientErrorException(
                HttpStatus.NOT_FOUND, "Not Found", "Order not found".getBytes(), null));

    // Act & Assert
    assertThrows(
        PaymentProcessingException.class,
        () -> adapter.getOrderStatus(ORDER_ID),
        "Should throw PaymentProcessingException when order is not found");

    verify(restTemplate, times(1))
        .exchange(
            eq(ORDERS_API_URL + "/" + ORDER_ID),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(MercadoPagoOrderResponse.class));
  }

  @Test
  @DisplayName("Should throw PaymentProcessingException on network error")
  void testGetOrderStatusNetworkError() {
    // Arrange
    when(restTemplate.exchange(
            eq(ORDERS_API_URL + "/" + ORDER_ID),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(MercadoPagoOrderResponse.class)))
        .thenThrow(new RuntimeException("Network error"));

    // Act & Assert
    assertThrows(
        PaymentProcessingException.class,
        () -> adapter.getOrderStatus(ORDER_ID),
        "Should throw PaymentProcessingException on network error");
  }

  @Test
  @DisplayName("Should set correct Authorization header")
  void testAuthorizationHeaderIsSet() {
    // Arrange
    MercadoPagoOrderResponse response = createApprovedOrderResponse();
    ResponseEntity<MercadoPagoOrderResponse> responseEntity =
        new ResponseEntity<>(response, HttpStatus.OK);

    ArgumentCaptor<HttpEntity<?>> captor = ArgumentCaptor.forClass(HttpEntity.class);

    when(restTemplate.exchange(
            eq(ORDERS_API_URL + "/" + ORDER_ID),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(MercadoPagoOrderResponse.class)))
        .thenReturn(responseEntity);

    // Act
    adapter.getOrderStatus(ORDER_ID);

    // Assert
    verify(restTemplate)
        .exchange(
            eq(ORDERS_API_URL + "/" + ORDER_ID),
            eq(HttpMethod.GET),
            captor.capture(),
            eq(MercadoPagoOrderResponse.class));

    HttpEntity<?> capturedEntity = captor.getValue();
    assertTrue(
        capturedEntity.getHeaders().get("Authorization").get(0).contains("Bearer"),
        "Authorization header should contain Bearer token");
    assertTrue(
        capturedEntity.getHeaders().get("Authorization").get(0).contains(ACCESS_TOKEN),
        "Authorization header should contain access token");
  }

  @Test
  @DisplayName("Should extract QR code from response")
  void testExtractQrCodeFromResponse() {
    // Arrange
    MercadoPagoOrderResponse response = createApprovedOrderResponse();
    ResponseEntity<MercadoPagoOrderResponse> responseEntity =
        new ResponseEntity<>(response, HttpStatus.OK);

    when(restTemplate.exchange(
            anyString(), eq(HttpMethod.GET), any(HttpEntity.class), any(Class.class)))
        .thenReturn(responseEntity);

    // Act
    Payment result = adapter.getOrderStatus(ORDER_ID);

    // Assert
    assertNotNull(result);
    assertNotNull(result.getQrCode());
    assertNotNull(result.getQrCodeBase64());
  }

  // Helper methods to create test response objects

  private MercadoPagoOrderResponse createApprovedOrderResponse() {
    MercadoPagoOrderResponse response = new MercadoPagoOrderResponse();
    response.setId(ORDER_ID);
    response.setStatus("approved");

    MercadoPagoOrderResponse.Payment payment = new MercadoPagoOrderResponse.Payment();
    payment.setId("payment_12345");
    payment.setStatus("approved");

    MercadoPagoOrderResponse.PointOfInteraction poi =
        new MercadoPagoOrderResponse.PointOfInteraction();
    MercadoPagoOrderResponse.TransactionData txData = new MercadoPagoOrderResponse.TransactionData();
    txData.setQrCode("00020126580014br.gov.bcb.qrcode");
    txData.setQrCodeBase64("aGVsbG8gd29ybGQgcXJjb2Rl");
    poi.setTransactionData(txData);

    payment.setPointOfInteraction(poi);

    MercadoPagoOrderResponse.Transactions transactions = new MercadoPagoOrderResponse.Transactions();
    transactions.setPayments(new MercadoPagoOrderResponse.Payment[] {payment});

    response.setTransactions(transactions);
    return response;
  }

  private MercadoPagoOrderResponse createRejectedOrderResponse() {
    MercadoPagoOrderResponse response = new MercadoPagoOrderResponse();
    response.setId(ORDER_ID);
    response.setStatus("rejected");

    MercadoPagoOrderResponse.Payment payment = new MercadoPagoOrderResponse.Payment();
    payment.setId("payment_12345");
    payment.setStatus("rejected");
    payment.setStatusDetail("Insufficient funds");

    MercadoPagoOrderResponse.Transactions transactions = new MercadoPagoOrderResponse.Transactions();
    transactions.setPayments(new MercadoPagoOrderResponse.Payment[] {payment});

    response.setTransactions(transactions);
    return response;
  }

  private MercadoPagoOrderResponse createProcessingOrderResponse() {
    MercadoPagoOrderResponse response = new MercadoPagoOrderResponse();
    response.setId(ORDER_ID);
    response.setStatus("processing");

    MercadoPagoOrderResponse.Payment payment = new MercadoPagoOrderResponse.Payment();
    payment.setId("payment_12345");
    payment.setStatus("processing");

    MercadoPagoOrderResponse.Transactions transactions = new MercadoPagoOrderResponse.Transactions();
    transactions.setPayments(new MercadoPagoOrderResponse.Payment[] {payment});

    response.setTransactions(transactions);
    return response;
  }
}
