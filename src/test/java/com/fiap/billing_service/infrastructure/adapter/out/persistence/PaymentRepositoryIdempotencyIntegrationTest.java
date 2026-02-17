package com.fiap.billing_service.infrastructure.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.fiap.billing_service.infrastructure.adapter.out.persistence.entity.PaymentEntity;
import com.fiap.billing_service.infrastructure.adapter.out.persistence.repository.SpringDataPaymentRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Payment Repository Idempotency Integration Tests")
class PaymentRepositoryIdempotencyIntegrationTest {

  @Autowired private SpringDataPaymentRepository repository;

  @Test
  @DisplayName("Should enforce unique constraint on workOrderId")
  void testUniqueConstraintOnWorkOrderId() {
    // Arrange
    UUID workOrderId = UUID.randomUUID();
    UUID clientId = UUID.randomUUID();
    UUID budgetId = UUID.randomUUID();

    PaymentEntity payment1 = createPaymentEntity(budgetId, workOrderId, clientId);
    PaymentEntity payment2 = createPaymentEntity(budgetId, workOrderId, clientId);

    // Act
    repository.saveAndFlush(payment1); // First save should succeed

    // Assert - Second save with same workOrderId should fail
    assertThrows(
        DataIntegrityViolationException.class,
        () -> repository.saveAndFlush(payment2),
        "Should throw DataIntegrityViolationException for duplicate workOrderId");
  }

  @Test
  @DisplayName("Should allow different workOrderIds for same client")
  void testDifferentWorkOrderIds_SameClient_ShouldSucceed() {
    // Arrange
    UUID clientId = UUID.randomUUID();
    UUID budgetId1 = UUID.randomUUID();
    UUID budgetId2 = UUID.randomUUID();
    UUID workOrderId1 = UUID.randomUUID();
    UUID workOrderId2 = UUID.randomUUID();

    PaymentEntity payment1 = createPaymentEntity(budgetId1, workOrderId1, clientId);
    PaymentEntity payment2 = createPaymentEntity(budgetId2, workOrderId2, clientId);

    // Act & Assert - Both should save successfully
    assertDoesNotThrow(
        () -> {
          repository.saveAndFlush(payment1);
          repository.saveAndFlush(payment2);
        });

    assertEquals(2, repository.count());
  }

  @Test
  @DisplayName("Should find payment by workOrderId")
  void testFindByWorkOrderId() {
    // Arrange
    UUID workOrderId = UUID.randomUUID();
    UUID clientId = UUID.randomUUID();
    UUID budgetId = UUID.randomUUID();

    PaymentEntity payment = createPaymentEntity(budgetId, workOrderId, clientId);
    repository.saveAndFlush(payment);

    // Act
    var found = repository.findByWorkOrderId(workOrderId);

    // Assert
    assertTrue(found.isPresent());
    assertEquals(workOrderId, found.get().getWorkOrderId());
    assertEquals(clientId, found.get().getClientId());
  }

  @Test
  @DisplayName("Should return empty when workOrderId not found")
  void testFindByWorkOrderId_NotFound() {
    // Arrange
    UUID nonExistentWorkOrderId = UUID.randomUUID();

    // Act
    var found = repository.findByWorkOrderId(nonExistentWorkOrderId);

    // Assert
    assertFalse(found.isPresent());
  }

  @Test
  @DisplayName("Should update existing payment without violating constraint")
  void testUpdateExistingPayment_ShouldSucceed() {
    // Arrange
    UUID workOrderId = UUID.randomUUID();
    UUID clientId = UUID.randomUUID();
    UUID budgetId = UUID.randomUUID();

    PaymentEntity payment = createPaymentEntity(budgetId, workOrderId, clientId);
    payment = repository.saveAndFlush(payment);

    // Act - Update the same payment
    payment.setStatus("APPROVED");
    payment.setProcessedAt(LocalDateTime.now());

    // Assert - Update should succeed
    PaymentEntity finalPayment = payment;
    assertDoesNotThrow(() -> repository.saveAndFlush(finalPayment));

    var updated = repository.findById(payment.getId());
    assertTrue(updated.isPresent());
    assertEquals("APPROVED", updated.get().getStatus());
    assertNotNull(updated.get().getProcessedAt());
  }

  private PaymentEntity createPaymentEntity(UUID budgetId, UUID workOrderId, UUID clientId) {
    PaymentEntity payment = new PaymentEntity();
    payment.setId(UUID.randomUUID());
    payment.setBudgetId(budgetId);
    payment.setWorkOrderId(workOrderId);
    payment.setClientId(clientId);
    payment.setAmount(new BigDecimal("100.00"));
    payment.setStatus("PENDING");
    payment.setPaymentMethod("pix");
    payment.setCreatedAt(LocalDateTime.now());
    return payment;
  }
}
