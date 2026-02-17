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
  private UUID clientId;
  private UUID budgetId;

  @BeforeEach
  void setUp() {
    workOrderId = UUID.randomUUID();
    clientId = UUID.randomUUID();
    budgetId = UUID.randomUUID();
    dto = new PaymentRequestDto();
  }

  @Test
  @DisplayName("Should construct with all parameters")
  void testConstructor_WithAllParameters_InitializesFields() {
    // Arrange
    PaymentRequestDto.OrderRequest orderRequest =
        new PaymentRequestDto(budgetId, clientId, UUID.randomUUID(), null)
            .new OrderRequest("ext_ref", new BigDecimal("100.00"), "test@example.com", "John");

    // Act
    dto = new PaymentRequestDto(workOrderId, clientId, budgetId, orderRequest);

    // Assert
    assertThat(dto.getWorkOrderId()).isEqualTo(workOrderId);
    assertThat(dto.getClientId()).isEqualTo(clientId);
    assertThat(dto.getBudgetId()).isEqualTo(budgetId);
    assertThat(dto.getOrderRequest()).isEqualTo(orderRequest);
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
  @DisplayName("Should set and get client ID")
  void testSetClientId() {
    // Act
    dto.setClientId(clientId);

    // Assert
    assertThat(dto.getClientId()).isEqualTo(clientId);
  }

  @Test
  @DisplayName("Should set and get budget ID")
  void testSetBudgetId() {
    // Act
    dto.setBudgetId(budgetId);

    // Assert
    assertThat(dto.getBudgetId()).isEqualTo(budgetId);
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
  @DisplayName("Should construct nested OrderRequest with amount and email")
  void testOrderRequest_Constructor_InitializesFields() {
    // Arrange
    String externalReference = "ext_123";
    BigDecimal amount = new BigDecimal("250.00");
    String email = "customer@example.com";
    String firstName = "Alice";

    // Act
    PaymentRequestDto.OrderRequest orderRequest =
        new PaymentRequestDto(budgetId, clientId, workOrderId, null)
            .new OrderRequest(externalReference, amount, email, firstName);

    // Assert
    assertThat(orderRequest.getType()).isEqualTo("online");
    assertThat(orderRequest.getExternalReference()).isEqualTo(externalReference);
    assertThat(orderRequest.getTotalAmount()).isEqualTo("250.00");
    assertThat(orderRequest.getPayer()).isNotNull();
    assertThat(orderRequest.getPayer().getEmail()).isEqualTo(email);
    assertThat(orderRequest.getPayer().getFirstName()).isEqualTo(firstName);
    assertThat(orderRequest.getTransactions()).isNotNull();
    assertThat(orderRequest.getTransactions().getPayments()).hasSize(1);
  }

  @Test
  @DisplayName("Should set and get order request")
  void testSetOrderRequest() {
    // Arrange
    PaymentRequestDto.OrderRequest orderRequest =
        new PaymentRequestDto(budgetId, clientId, workOrderId, null)
            .new OrderRequest("ref_456", new BigDecimal("150.00"), "user@test.com", "Bob");

    // Act
    dto.setOrderRequest(orderRequest);

    // Assert
    assertThat(dto.getOrderRequest()).isEqualTo(orderRequest);
    assertThat(dto.getOrderRequest().getExternalReference()).isEqualTo("ref_456");
  }

  @Test
  @DisplayName("Should construct PaymentRequestDto with all nested objects properly initialized")
  void testFullPaymentRequestDtoConstruction_BuildsCompleteObjectGraph() {
    // Arrange
    UUID testWorkOrderId = UUID.randomUUID();
    UUID testClientId = UUID.randomUUID();
    UUID testBudgetId = UUID.randomUUID();
    PaymentRequestDto testDto = new PaymentRequestDto();
    testDto.setWorkOrderId(testWorkOrderId);
    testDto.setClientId(testClientId);
    testDto.setBudgetId(testBudgetId);
    testDto.setDescription("Test payment");

    PaymentRequestDto.OrderRequest orderRequest =
        testDto.new OrderRequest(
            "order_xyz", new BigDecimal("500.00"), "premium@test.com", "Premium User");
    testDto.setOrderRequest(orderRequest);

    // Assert
    assertThat(testDto.getWorkOrderId()).isEqualTo(testWorkOrderId);
    assertThat(testDto.getClientId()).isEqualTo(testClientId);
    assertThat(testDto.getBudgetId()).isEqualTo(testBudgetId);
    assertThat(testDto.getDescription()).isEqualTo("Test payment");

    // Verify OrderRequest
    assertThat(testDto.getOrderRequest()).isNotNull();
    assertThat(testDto.getOrderRequest().getType()).isEqualTo("online");
    assertThat(testDto.getOrderRequest().getExternalReference()).isEqualTo("order_xyz");
    assertThat(testDto.getOrderRequest().getTotalAmount()).isEqualTo("500.00");

    // Verify Payer within OrderRequest
    assertThat(testDto.getOrderRequest().getPayer()).isNotNull();
    assertThat(testDto.getOrderRequest().getPayer().getEmail()).isEqualTo("premium@test.com");
    assertThat(testDto.getOrderRequest().getPayer().getFirstName()).isEqualTo("Premium User");

    // Verify Transactions and Payments
    assertThat(testDto.getOrderRequest().getTransactions()).isNotNull();
    assertThat(testDto.getOrderRequest().getTransactions().getPayments()).hasSize(1);
    assertThat(testDto.getOrderRequest().getTransactions().getPayments()[0].getAmount())
        .isEqualTo("500.00");
  }
}
