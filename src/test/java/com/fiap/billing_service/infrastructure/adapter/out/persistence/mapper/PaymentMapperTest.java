package com.fiap.billing_service.infrastructure.adapter.out.persistence.mapper;

import static org.junit.jupiter.api.Assertions.*;

import com.fiap.billing_service.domain.entity.Payment;
import com.fiap.billing_service.domain.valueobject.PaymentStatus;
import com.fiap.billing_service.infrastructure.adapter.out.persistence.entity.PaymentEntity;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentMapper Tests")
class PaymentMapperTest {

  private PaymentMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new PaymentMapper();
  }

  // ==================== toEntity() Tests ====================

  @Test
  @DisplayName("Should convert Payment domain to PaymentEntity successfully")
  void testToEntity_ValidPayment_MapsAllFields() {
    // Arrange
    UUID id = UUID.randomUUID();
    UUID workOrderId = UUID.randomUUID();
    UUID customerId = UUID.randomUUID();
    BigDecimal amount = new BigDecimal("100.00");

    Payment payment = new Payment(id, workOrderId, customerId, amount);
    payment.markAsProcessing(
        "ext_pay_123", "order_pay_456", "pix", "qr_code_xyz", "qr_code_base64_abc");
    payment.markAsApproved();

    // Act
    PaymentEntity entity = mapper.toEntity(payment);

    // Assert
    assertNotNull(entity);
    assertEquals(id, entity.getId());
    assertEquals(workOrderId, entity.getWorkOrderId());
    assertEquals(customerId, entity.getCustomerId());
    assertEquals(amount, entity.getAmount());
    assertEquals("APPROVED", entity.getStatus());
    assertEquals("ext_pay_123", entity.getExternalPaymentId());
    assertEquals("order_pay_456", entity.getOrderPaymentId());
    assertEquals("pix", entity.getPaymentMethod());
    assertEquals("qr_code_xyz", entity.getQrCode());
    assertEquals("qr_code_base64_abc", entity.getQrCodeBase64());
    assertNotNull(entity.getCreatedAt());
  }

  @Test
  @DisplayName("Should map PENDING payment status to entity")
  void testToEntity_PendingStatus() {
    // Arrange
    Payment payment =
        new Payment(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("50.00"));
    // Payment defaults to PENDING status

    // Act
    PaymentEntity entity = mapper.toEntity(payment);

    // Assert
    assertEquals("PENDING", entity.getStatus());
  }

  @Test
  @DisplayName("Should map PROCESSING payment status to entity")
  void testToEntity_ProcessingStatus() {
    // Arrange
    Payment payment =
        new Payment(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("50.00"));
    payment.markAsProcessing("ext_id", "order_id", "pix", null, null);

    // Act
    PaymentEntity entity = mapper.toEntity(payment);

    // Assert
    assertEquals("PROCESSING", entity.getStatus());
    assertEquals("ext_id", entity.getExternalPaymentId());
    assertEquals("order_id", entity.getOrderPaymentId());
  }

  @Test
  @DisplayName("Should map APPROVED payment status to entity")
  void testToEntity_ApprovedStatus() {
    // Arrange
    Payment payment =
        new Payment(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("50.00"));
    payment.markAsProcessing("ext_id", "order_id", "pix", "qr", "qr_b64");
    payment.markAsApproved();

    // Act
    PaymentEntity entity = mapper.toEntity(payment);

    // Assert
    assertEquals("APPROVED", entity.getStatus());
  }

  @Test
  @DisplayName("Should map REJECTED payment status with error message to entity")
  void testToEntity_RejectedStatus_WithErrorMessage() {
    // Arrange
    Payment payment =
        new Payment(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("50.00"));
    payment.markAsRejected("Insufficient funds");

    // Act
    PaymentEntity entity = mapper.toEntity(payment);

    // Assert
    assertEquals("REJECTED", entity.getStatus());
    assertEquals("Insufficient funds", entity.getErrorMessage());
  }

  @Test
  @DisplayName("Should map FAILED payment status with error message to entity")
  void testToEntity_FailedStatus_WithErrorMessage() {
    // Arrange
    Payment payment =
        new Payment(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("50.00"));
    payment.markAsFailed("Payment gateway timeout");

    // Act
    PaymentEntity entity = mapper.toEntity(payment);

    // Assert
    assertEquals("FAILED", entity.getStatus());
    assertEquals("Payment gateway timeout", entity.getErrorMessage());
  }

  @Test
  @DisplayName("Should handle null optional fields in toEntity")
  void testToEntity_NullOptionalFields() {
    // Arrange
    Payment payment =
        new Payment(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("50.00"));
    // Don't set optional fields

    // Act
    PaymentEntity entity = mapper.toEntity(payment);

    // Assert
    assertNull(entity.getExternalPaymentId());
    assertNull(entity.getOrderPaymentId());
    assertNull(entity.getPaymentMethod());
    assertNull(entity.getQrCode());
    assertNull(entity.getQrCodeBase64());
    assertNull(entity.getProcessedAt());
    assertNull(entity.getErrorMessage());
  }

  @Test
  @DisplayName("Should preserve amount precision in conversion")
  void testToEntity_PreservesAmountPrecision() {
    // Arrange
    BigDecimal preciseAmount = new BigDecimal("1234.56");
    Payment payment =
        new Payment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), preciseAmount);

    // Act
    PaymentEntity entity = mapper.toEntity(payment);

    // Assert
    assertEquals(preciseAmount, entity.getAmount());
  }

  @Test
  @DisplayName("Should preserve all UUID fields in toEntity")
  void testToEntity_PreservesAllUUIDs() {
    // Arrange
    UUID id = UUID.randomUUID();
    UUID workOrderId = UUID.randomUUID();
    UUID customerId = UUID.randomUUID();

    Payment payment = new Payment(id, workOrderId, customerId, new BigDecimal("100.00"));

    // Act
    PaymentEntity entity = mapper.toEntity(payment);

    // Assert
    assertEquals(id, entity.getId());
    assertEquals(workOrderId, entity.getWorkOrderId());
    assertEquals(customerId, entity.getCustomerId());
  }

  // ==================== toDomain() Tests ====================

  @Test
  @DisplayName("Should convert PaymentEntity with APPROVED status to domain")
  void testToDomain_ApprovedStatus() {
    // Arrange
    UUID id = UUID.randomUUID();
    UUID workOrderId = UUID.randomUUID();
    UUID customerId = UUID.randomUUID();
    BigDecimal amount = new BigDecimal("100.00");

    PaymentEntity entity = new PaymentEntity();
    entity.setId(id);
    entity.setWorkOrderId(workOrderId);
    entity.setCustomerId(customerId);
    entity.setAmount(amount);
    entity.setStatus("APPROVED");
    entity.setExternalPaymentId("ext_pay_123");
    entity.setOrderPaymentId("order_pay_456");
    entity.setPaymentMethod("pix");
    entity.setQrCode("qr_code");
    entity.setQrCodeBase64("qr_code_b64");

    // Act
    Payment payment = mapper.toDomain(entity);

    // Assert
    assertNotNull(payment);
    assertEquals(id, payment.getId());
    assertEquals(workOrderId, payment.getWorkOrderId());
    assertEquals(customerId, payment.getCustomerId());
    assertEquals(amount, payment.getAmount());
    assertEquals(PaymentStatus.APPROVED, payment.getStatus());
    assertEquals("ext_pay_123", payment.getExternalPaymentId());
    assertEquals("order_pay_456", payment.getOrderPaymentId());
    assertEquals("pix", payment.getPaymentMethod());
    assertEquals("qr_code", payment.getQrCode());
    assertEquals("qr_code_b64", payment.getQrCodeBase64());
  }

  @Test
  @DisplayName("Should convert PaymentEntity with REJECTED status to domain")
  void testToDomain_RejectedStatus() {
    // Arrange
    PaymentEntity entity = new PaymentEntity();
    entity.setId(UUID.randomUUID());
    entity.setBudgetId(UUID.randomUUID());
    entity.setWorkOrderId(UUID.randomUUID());
    entity.setCustomerId(UUID.randomUUID());
    entity.setAmount(new BigDecimal("100.00"));
    entity.setStatus("REJECTED");
    entity.setErrorMessage("Insufficient funds");

    // Act
    Payment payment = mapper.toDomain(entity);

    // Assert
    assertEquals(PaymentStatus.REJECTED, payment.getStatus());
    assertEquals("Insufficient funds", payment.getErrorMessage());
  }

  @Test
  @DisplayName("Should convert PaymentEntity with FAILED status to domain")
  void testToDomain_FailedStatus() {
    // Arrange
    PaymentEntity entity = new PaymentEntity();
    entity.setId(UUID.randomUUID());
    entity.setBudgetId(UUID.randomUUID());
    entity.setWorkOrderId(UUID.randomUUID());
    entity.setCustomerId(UUID.randomUUID());
    entity.setAmount(new BigDecimal("100.00"));
    entity.setStatus("FAILED");
    entity.setErrorMessage("Timeout");

    // Act
    Payment payment = mapper.toDomain(entity);

    // Assert
    assertEquals(PaymentStatus.FAILED, payment.getStatus());
    assertEquals("Timeout", payment.getErrorMessage());
  }

  @Test
  @DisplayName("Should convert PaymentEntity with PROCESSING status to domain")
  void testToDomain_ProcessingStatus() {
    // Arrange
    PaymentEntity entity = new PaymentEntity();
    entity.setId(UUID.randomUUID());
    entity.setBudgetId(UUID.randomUUID());
    entity.setWorkOrderId(UUID.randomUUID());
    entity.setCustomerId(UUID.randomUUID());
    entity.setAmount(new BigDecimal("100.00"));
    entity.setStatus("PROCESSING");
    entity.setExternalPaymentId("ext_pay_123");
    entity.setOrderPaymentId("order_pay_456");
    entity.setPaymentMethod("pix");
    entity.setQrCode("qr_code");
    entity.setQrCodeBase64("qr_code_b64");

    // Act
    Payment payment = mapper.toDomain(entity);

    // Assert
    assertEquals(PaymentStatus.PROCESSING, payment.getStatus());
    assertEquals("ext_pay_123", payment.getExternalPaymentId());
    assertEquals("order_pay_456", payment.getOrderPaymentId());
  }

  @Test
  @DisplayName("Should handle null status in toDomain")
  void testToDomain_NullStatus() {
    // Arrange
    PaymentEntity entity = new PaymentEntity();
    entity.setId(UUID.randomUUID());
    entity.setBudgetId(UUID.randomUUID());
    entity.setWorkOrderId(UUID.randomUUID());
    entity.setCustomerId(UUID.randomUUID());
    entity.setAmount(new BigDecimal("100.00"));
    entity.setStatus(null); // No status

    // Act
    Payment payment = mapper.toDomain(entity);

    // Assert
    assertNotNull(payment);
    // Status should remain PENDING (default from constructor)
    assertEquals(PaymentStatus.PENDING, payment.getStatus());
  }

  @Test
  @DisplayName("Should handle null optional fields in toDomain")
  void testToDomain_NullOptionalFields() {
    // Arrange
    PaymentEntity entity = new PaymentEntity();
    entity.setId(UUID.randomUUID());
    entity.setBudgetId(UUID.randomUUID());
    entity.setWorkOrderId(UUID.randomUUID());
    entity.setCustomerId(UUID.randomUUID());
    entity.setAmount(new BigDecimal("100.00"));
    entity.setStatus("PENDING");

    // Act
    Payment payment = mapper.toDomain(entity);

    // Assert
    assertNull(payment.getExternalPaymentId());
    assertNull(payment.getOrderPaymentId());
    assertNull(payment.getPaymentMethod());
    assertNull(payment.getQrCode());
    assertNull(payment.getQrCodeBase64());
    assertNull(payment.getErrorMessage());
  }

  @Test
  @DisplayName("Should preserve amount precision in toDomain")
  void testToDomain_PreservesAmountPrecision() {
    // Arrange
    BigDecimal preciseAmount = new BigDecimal("9999.99");
    PaymentEntity entity = new PaymentEntity();
    entity.setId(UUID.randomUUID());
    entity.setBudgetId(UUID.randomUUID());
    entity.setWorkOrderId(UUID.randomUUID());
    entity.setCustomerId(UUID.randomUUID());
    entity.setAmount(preciseAmount);
    entity.setStatus("PENDING");

    // Act
    Payment payment = mapper.toDomain(entity);

    // Assert
    assertEquals(preciseAmount, payment.getAmount());
  }

  @Test
  @DisplayName("Should round-trip conversion: domain -> entity -> domain")
  void testRoundTrip_DomainToEntityToDomain() {
    // Arrange
    UUID id = UUID.randomUUID();
    UUID workOrderId = UUID.randomUUID();
    UUID customerId = UUID.randomUUID();
    BigDecimal amount = new BigDecimal("250.75");

    Payment originalPayment = new Payment(id, workOrderId, customerId, amount);
    originalPayment.markAsProcessing("ext_123", "order_456", "pix", "qr_xyz", "qr_b64_123");
    originalPayment.markAsApproved();

    // Act - Step 1: domain to entity
    PaymentEntity entity = mapper.toEntity(originalPayment);

    // Act - Step 2: entity back to domain
    Payment recoveredPayment = mapper.toDomain(entity);

    // Assert
    assertEquals(id, recoveredPayment.getId());
    assertEquals(workOrderId, recoveredPayment.getWorkOrderId());
    assertEquals(customerId, recoveredPayment.getCustomerId());
    assertEquals(amount, recoveredPayment.getAmount());
    assertEquals(PaymentStatus.APPROVED, recoveredPayment.getStatus());
    assertEquals("ext_123", recoveredPayment.getExternalPaymentId());
    assertEquals("order_456", recoveredPayment.getOrderPaymentId());
    assertEquals("pix", recoveredPayment.getPaymentMethod());
    assertEquals("qr_xyz", recoveredPayment.getQrCode());
    assertEquals("qr_b64_123", recoveredPayment.getQrCodeBase64());
  }

  @Test
  @DisplayName("Should handle all payment statuses: PENDING")
  void testAllStatuses_Pending() {
    // Arrange
    PaymentEntity entity = createEntityWithStatus("PENDING");

    // Act
    Payment payment = mapper.toDomain(entity);

    // Assert
    assertEquals(PaymentStatus.PENDING, payment.getStatus());
  }

  @Test
  @DisplayName("Should handle all payment statuses: PROCESSING")
  void testAllStatuses_Processing() {
    // Arrange
    PaymentEntity entity = createEntityWithStatus("PROCESSING");
    entity.setExternalPaymentId("ext_id");
    entity.setOrderPaymentId("order_id");
    entity.setPaymentMethod("pix");

    // Act
    Payment payment = mapper.toDomain(entity);

    // Assert
    assertEquals(PaymentStatus.PROCESSING, payment.getStatus());
  }

  @Test
  @DisplayName("Should handle all payment statuses: APPROVED")
  void testAllStatuses_Approved() {
    // Arrange
    PaymentEntity entity = createEntityWithStatus("APPROVED");
    entity.setExternalPaymentId("ext_id");
    entity.setOrderPaymentId("order_id");
    entity.setPaymentMethod("pix");

    // Act
    Payment payment = mapper.toDomain(entity);

    // Assert
    assertEquals(PaymentStatus.APPROVED, payment.getStatus());
  }

  @Test
  @DisplayName("Should handle all payment statuses: REJECTED")
  void testAllStatuses_Rejected() {
    // Arrange
    PaymentEntity entity = createEntityWithStatus("REJECTED");
    entity.setErrorMessage("Error details");

    // Act
    Payment payment = mapper.toDomain(entity);

    // Assert
    assertEquals(PaymentStatus.REJECTED, payment.getStatus());
    assertEquals("Error details", payment.getErrorMessage());
  }

  @Test
  @DisplayName("Should handle all payment statuses: FAILED")
  void testAllStatuses_Failed() {
    // Arrange
    PaymentEntity entity = createEntityWithStatus("FAILED");
    entity.setErrorMessage("Failure reason");

    // Act
    Payment payment = mapper.toDomain(entity);

    // Assert
    assertEquals(PaymentStatus.FAILED, payment.getStatus());
    assertEquals("Failure reason", payment.getErrorMessage());
  }

  // Helper method
  private PaymentEntity createEntityWithStatus(String status) {
    PaymentEntity entity = new PaymentEntity();
    entity.setId(UUID.randomUUID());
    entity.setBudgetId(UUID.randomUUID());
    entity.setWorkOrderId(UUID.randomUUID());
    entity.setCustomerId(UUID.randomUUID());
    entity.setAmount(new BigDecimal("100.00"));
    entity.setStatus(status);
    return entity;
  }
}
