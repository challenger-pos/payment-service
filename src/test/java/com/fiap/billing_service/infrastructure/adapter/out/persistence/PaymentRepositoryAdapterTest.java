package com.fiap.billing_service.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.*;

import com.fiap.billing_service.DynamoDbTestBase;
import com.fiap.billing_service.domain.entity.Payment;
import com.fiap.billing_service.infrastructure.adapter.out.persistence.mapper.PaymentMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;

/**
 * Integration tests for PaymentRepositoryAdapter using DynamoDB Local.
 *
 * These tests verify the DynamoDB persistence layer with actual
 * DynamoDB operations rather than mocks, ensuring correctness
 * of query patterns and data serialization.
 */
@DisplayName("PaymentRepositoryAdapter Integration Tests with DynamoDB")
class PaymentRepositoryAdapterTest extends DynamoDbTestBase {

  @Autowired
  private DynamoDbEnhancedClient dynamoDbEnhancedClient;

  @Autowired
  private PaymentMapper paymentMapper;

  private PaymentRepositoryAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new PaymentRepositoryAdapter(dynamoDbEnhancedClient, paymentMapper);
  }

  @Test
  @DisplayName("Should save payment and retrieve it by work order ID")
  void testSave_SavesPayment_CanRetrieveIt() {
    // Arrange
    UUID paymentId = UUID.randomUUID();
    UUID budgetId = UUID.randomUUID();
    UUID workOrderId = UUID.randomUUID();
    UUID clientId = UUID.randomUUID();
    BigDecimal amount = new BigDecimal("100.00");
    LocalDateTime now = LocalDateTime.now();

    Payment payment = new Payment(paymentId, budgetId, workOrderId, clientId, amount);
    payment.setCreatedAt(now);
    payment.setStatus("PENDING");

    // Act
    Payment saved = adapter.save(payment);

    // Assert
    assertThat(saved).isNotNull();
    assertThat(saved.getId()).isEqualTo(paymentId);
    assertThat(saved.getWorkOrderId()).isEqualTo(workOrderId);

    // Verify we can retrieve it
    Optional<Payment> retrieved = adapter.findByWorkOrderId(workOrderId);
    assertThat(retrieved).isPresent();
    assertThat(retrieved.get().getId()).isEqualTo(paymentId);
  }

  @Test
  @DisplayName("Should find payment by work order ID")
  void testFindByWorkOrderId_ReturnPayment() {
    // Arrange
    UUID workOrderId = UUID.randomUUID();
    Payment payment = createTestPayment(workOrderId);
    adapter.save(payment);

    // Act
    Optional<Payment> result = adapter.findByWorkOrderId(workOrderId);

    // Assert
    assertThat(result).isPresent();
    assertThat(result.get().getWorkOrderId()).isEqualTo(workOrderId);
  }

  @Test
  @DisplayName("Should return empty optional when payment not found")
  void testFindByWorkOrderId_NotFound_ReturnEmpty() {
    // Arrange
    UUID nonExistentWorkOrderId = UUID.randomUUID();

    // Act
    Optional<Payment> result = adapter.findByWorkOrderId(nonExistentWorkOrderId);

    // Assert
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("Should preserve payment data through save operation")
  void testSave_PreservesPaymentData() {
    // Arrange
    UUID paymentId = UUID.randomUUID();
    UUID budgetId = UUID.randomUUID();
    UUID workOrderId = UUID.randomUUID();
    UUID clientId = UUID.randomUUID();
    BigDecimal amount = new BigDecimal("150.00");
    String status = "PENDING";

    Payment payment = new Payment(paymentId, budgetId, workOrderId, clientId, amount);
    payment.setStatus(status);

    // Act
    Payment result = adapter.save(payment);

    // Assert
    assertThat(result.getId()).isEqualTo(paymentId);
    assertThat(result.getBudgetId()).isEqualTo(budgetId);
    assertThat(result.getWorkOrderId()).isEqualTo(workOrderId);
    assertThat(result.getClientId()).isEqualTo(clientId);
    assertThat(result.getAmount()).isEqualTo(amount);
    assertThat(result.getStatus()).isEqualTo(status);
  }

  @Test
  @DisplayName("Should handle multiple saves of different payments")
  void testSave_MultipleDifferentPayments() {
    // Arrange
    Payment payment1 = createTestPayment(UUID.randomUUID());
    Payment payment2 = createTestPayment(UUID.randomUUID());

    // Act
    Payment result1 = adapter.save(payment1);
    Payment result2 = adapter.save(payment2);

    // Assert
    assertThat(result1.getWorkOrderId()).isNotEqualTo(result2.getWorkOrderId());
    
    // Verify both can be retrieved
    Optional<Payment> retrieved1 = adapter.findByWorkOrderId(payment1.getWorkOrderId());
    Optional<Payment> retrieved2 = adapter.findByWorkOrderId(payment2.getWorkOrderId());
    
    assertThat(retrieved1).isPresent();
    assertThat(retrieved2).isPresent();
  }

  @Test
  @DisplayName("Should update existing payment")
  void testUpdate_UpdatesPayment() {
    // Arrange
    UUID workOrderId = UUID.randomUUID();
    Payment payment = createTestPayment(workOrderId);
    adapter.save(payment);

    // Modify the payment
    payment.setStatus("APPROVED");
    payment.setExternalPaymentId("MP-123456");

    // Act
    adapter.update(payment);

    // Assert
    Optional<Payment> retrieved = adapter.findByWorkOrderId(workOrderId);
    assertThat(retrieved).isPresent();
    assertThat(retrieved.get().getStatus()).isEqualTo("APPROVED");
    assertThat(retrieved.get().getExternalPaymentId()).isEqualTo("MP-123456");
  }

  @Test
  @DisplayName("Should delete payment by work order ID")
  void testDeleteByWorkOrderId_DeletesPayment() {
    // Arrange
    UUID workOrderId = UUID.randomUUID();
    Payment payment = createTestPayment(workOrderId);
    adapter.save(payment);

    // Verify it exists
    assertThat(adapter.findByWorkOrderId(workOrderId)).isPresent();

    // Act
    adapter.deleteByWorkOrderId(workOrderId);

    // Assert
    assertThat(adapter.findByWorkOrderId(workOrderId)).isEmpty();
  }

  @Test
  @DisplayName("Should handle payment with all optional fields")
  void testSave_PaymentWithAllFields() {
    // Arrange
    UUID paymentId = UUID.randomUUID();
    UUID workOrderId = UUID.randomUUID();
    Payment payment = createTestPayment(workOrderId);
    payment.setId(paymentId);
    payment.setExternalPaymentId("MP-PAY-123");
    payment.setOrderPaymentId("ORDER-123");
    payment.setQrCode("test-qr-code");
    payment.setQrCodeBase64("base64-encoded-qr");
    payment.setErrorMessage(null);

    // Act
    Payment saved = adapter.save(payment);

    // Assert
    Optional<Payment> retrieved = adapter.findByWorkOrderId(workOrderId);
    assertThat(retrieved).isPresent();
    assertThat(retrieved.get().getExternalPaymentId()).isEqualTo("MP-PAY-123");
    assertThat(retrieved.get().getOrderPaymentId()).isEqualTo("ORDER-123");
    assertThat(retrieved.get().getQrCode()).isEqualTo("test-qr-code");
  }

  @Test
  @DisplayName("Should handle payment with error message")
  void testSave_PaymentWithErrorMessage() {
    // Arrange
    UUID workOrderId = UUID.randomUUID();
    Payment payment = createTestPayment(workOrderId);
    payment.setStatus("FAILED");
    payment.setErrorMessage("Payment declined: Insufficient funds");

    // Act
    Payment saved = adapter.save(payment);

    // Assert
    Optional<Payment> retrieved = adapter.findByWorkOrderId(workOrderId);
    assertThat(retrieved).isPresent();
    assertThat(retrieved.get().getStatus()).isEqualTo("FAILED");
    assertThat(retrieved.get().getErrorMessage()).contains("Insufficient funds");
  }

  /**
   * Helper method to create a test payment with standard values.
   */
  private Payment createTestPayment(UUID workOrderId) {
    return new Payment(
        UUID.randomUUID(),
        UUID.randomUUID(),
        workOrderId,
        UUID.randomUUID(),
        new BigDecimal("100.00")
    );
  }
}
