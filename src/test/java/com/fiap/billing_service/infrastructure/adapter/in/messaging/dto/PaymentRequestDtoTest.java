package com.fiap.billing_service.infrastructure.adapter.in.messaging.dto;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PaymentRequestDto Tests")
class PaymentRequestDtoTest {

  private PaymentRequestDto dto;
  private UUID workOrderId;
  private UUID customerId;

  @BeforeEach
  void setUp() {
    workOrderId = UUID.randomUUID();
    customerId = UUID.randomUUID();
    dto = new PaymentRequestDto();
  }

  @Test
  @DisplayName("Should construct with all parameters")
  void testConstructor_WithAllParameters_InitializesFields() {
    // Act
    dto = new PaymentRequestDto(workOrderId, customerId, new BigDecimal("100.00"), "APRO");

    // Assert
    assertThat(dto.getWorkOrderId()).isEqualTo(workOrderId);
    assertThat(dto.getCustomerId()).isEqualTo(customerId);
  }

  @Test
  @DisplayName("Should set and get work order ID")
  void testSetWorkOrderId() {
    // Act
    dto.setWorkOrderId(workOrderId);

    // Assert
    assertThat(dto.getWorkOrderId()).isEqualTo(workOrderId);
  }

  @Test
  @DisplayName("Should set and get customer ID")
  void testSetCustomerId() {
    // Act
    dto.setCustomerId(customerId);

    // Assert
    assertThat(dto.getCustomerId()).isEqualTo(customerId);
  }

  @Test
  @DisplayName("Should set and get description")
  void testSetDescription() {
    // Arrange
    String description = "Payment for monthly subscription";

    // Act
    dto.setDescription(description);

    // Assert
    assertThat(dto.getDescription()).isEqualTo(description);
  }

  @Test
  @DisplayName("Should construct nested OrderRequest without errors")
  void testOrderRequest_Constructor_InitializesFields() {
    // Arrange
    BigDecimal amount = new BigDecimal("250.00");

    // Act
    PaymentRequestDto.OrderRequest orderRequest =
        new PaymentRequestDto(customerId, workOrderId, new BigDecimal("100.00"), "false")
            .new OrderRequest(amount, "", "false");

    // Assert
    assertThat(orderRequest).isNotNull();
    assertThat(orderRequest.getType()).isEqualTo("online");
    assertThat(orderRequest.getTotalAmount()).isEqualTo("250.00");
  }

  @Test
  @DisplayName("Should set and get order request")
  void testSetOrderRequest() {
    // Arrange
    PaymentRequestDto.OrderRequest orderRequest =
        new PaymentRequestDto(customerId, workOrderId, new BigDecimal("150.00"), "true")
            .new OrderRequest(new BigDecimal("150.00"), "", "true");

    // Act
    dto.setOrderRequest(orderRequest);

    // Assert
    assertThat(dto.getOrderRequest()).isNotNull();
    assertThat(dto.getOrderRequest().getTotalAmount()).isEqualTo("150.00");
  }

  @Test
  @DisplayName("Should construct PaymentRequestDto with all nested objects properly initialized")
  void testFullPaymentRequestDtoConstruction_BuildsCompleteObjectGraph() {
    // Arrange
    UUID testWorkOrderId = UUID.randomUUID();
    UUID testClientId = UUID.randomUUID();
    PaymentRequestDto testDto = new PaymentRequestDto();
    testDto.setWorkOrderId(testWorkOrderId);
    testDto.setCustomerId(testClientId);
    testDto.setDescription("Test payment");

    PaymentRequestDto.OrderRequest orderRequest =
        testDto.new OrderRequest(new BigDecimal("500.00"), "", "true");
    testDto.setOrderRequest(orderRequest);

    // Assert
    assertThat(testDto.getWorkOrderId()).isEqualTo(testWorkOrderId);
    assertThat(testDto.getCustomerId()).isEqualTo(testClientId);
    assertThat(testDto.getDescription()).isEqualTo("Test payment");

    // Verify OrderRequest
    assertThat(testDto.getOrderRequest()).isNotNull();
    assertThat(testDto.getOrderRequest().getType()).isEqualTo("online");
    assertThat(testDto.getOrderRequest().getTotalAmount()).isEqualTo("500.00");
  }
}
