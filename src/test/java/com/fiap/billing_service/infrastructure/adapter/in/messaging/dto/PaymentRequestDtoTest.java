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
<<<<<<< HEAD
  private UUID customerId;
=======
  private UUID clientId;
  private UUID budgetId;
>>>>>>> 874da5d659f8f0227b13a5ef37e537fd54c18408

  @BeforeEach
  void setUp() {
    workOrderId = UUID.randomUUID();
<<<<<<< HEAD
    customerId = UUID.randomUUID();
=======
    clientId = UUID.randomUUID();
    budgetId = UUID.randomUUID();
>>>>>>> 874da5d659f8f0227b13a5ef37e537fd54c18408
    dto = new PaymentRequestDto();
  }

  @Test
  @DisplayName("Should construct with all parameters")
  void testConstructor_WithAllParameters_InitializesFields() {
    // Arrange
    PaymentRequestDto.OrderRequest orderRequest =
<<<<<<< HEAD
        new PaymentRequestDto(customerId, UUID.randomUUID(), new BigDecimal("100.00"), "APRO")
        .new OrderRequest(new BigDecimal("100.00"), "", "true");

    // Act
    dto = new PaymentRequestDto(workOrderId, customerId, new BigDecimal("100.00"), "APRO");

    // Assert
    assertThat(dto.getWorkOrderId()).isEqualTo(workOrderId);
    assertThat(dto.getCustomerId()).isEqualTo(customerId);
=======
        new PaymentRequestDto(budgetId, clientId, UUID.randomUUID(), null)
            .new OrderRequest("ext_ref", new BigDecimal("100.00"), "test@example.com", "John");

    // Act
    dto = new PaymentRequestDto(workOrderId, clientId, budgetId, orderRequest);

    // Assert
    assertThat(dto.getWorkOrderId()).isEqualTo(workOrderId);
    assertThat(dto.getClientId()).isEqualTo(clientId);
    assertThat(dto.getBudgetId()).isEqualTo(budgetId);
>>>>>>> 874da5d659f8f0227b13a5ef37e537fd54c18408
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
<<<<<<< HEAD
    dto.setCustomerId(customerId);

    // Assert
    assertThat(dto.getCustomerId()).isEqualTo(customerId);
=======
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
>>>>>>> 874da5d659f8f0227b13a5ef37e537fd54c18408
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
<<<<<<< HEAD
        new PaymentRequestDto(customerId, workOrderId, new BigDecimal("100.00"), "false")
        .new OrderRequest(amount, "", "false");
=======
        new PaymentRequestDto(budgetId, clientId, workOrderId, null)
            .new OrderRequest(externalReference, amount, email, firstName);
>>>>>>> 874da5d659f8f0227b13a5ef37e537fd54c18408

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
<<<<<<< HEAD
        new PaymentRequestDto(customerId, workOrderId, new BigDecimal("150.00"), "true")
        .new OrderRequest(new BigDecimal("150.00"), "", "true");
=======
        new PaymentRequestDto(budgetId, clientId, workOrderId, null)
            .new OrderRequest("ref_456", new BigDecimal("150.00"), "user@test.com", "Bob");
>>>>>>> 874da5d659f8f0227b13a5ef37e537fd54c18408

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
<<<<<<< HEAD
    PaymentRequestDto testDto = new PaymentRequestDto();
    testDto.setWorkOrderId(testWorkOrderId);
    testDto.setCustomerId(testClientId);
    testDto.setDescription("Test payment");

    PaymentRequestDto.OrderRequest orderRequest =
        testDto.new OrderRequest(new BigDecimal("500.00"), "", "true");
=======
    UUID testBudgetId = UUID.randomUUID();
    PaymentRequestDto testDto = new PaymentRequestDto();
    testDto.setWorkOrderId(testWorkOrderId);
    testDto.setClientId(testClientId);
    testDto.setBudgetId(testBudgetId);
    testDto.setDescription("Test payment");

    PaymentRequestDto.OrderRequest orderRequest =
        testDto.new OrderRequest(
            "order_xyz", new BigDecimal("500.00"), "premium@test.com", "Premium User");
>>>>>>> 874da5d659f8f0227b13a5ef37e537fd54c18408
    testDto.setOrderRequest(orderRequest);

    // Assert
    assertThat(testDto.getWorkOrderId()).isEqualTo(testWorkOrderId);
<<<<<<< HEAD
    assertThat(testDto.getCustomerId()).isEqualTo(testClientId);
=======
    assertThat(testDto.getClientId()).isEqualTo(testClientId);
    assertThat(testDto.getBudgetId()).isEqualTo(testBudgetId);
>>>>>>> 874da5d659f8f0227b13a5ef37e537fd54c18408
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
