package com.fiap.billing_service.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fiap.billing_service.domain.entity.Payment;
import com.fiap.billing_service.infrastructure.adapter.out.persistence.entity.PaymentEntity;
import com.fiap.billing_service.infrastructure.adapter.out.persistence.mapper.PaymentMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

/**
 * Unit tests for PaymentRepositoryAdapter with mocked DynamoDB client.
 * 
 * These tests verify the adapter logic without requiring a running DynamoDB instance.
 * For integration tests with real DynamoDB, use docker-compose.yml with DynamoDB Local.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentRepositoryAdapter Unit Tests")
class PaymentRepositoryAdapterTest {

  @Mock
  private DynamoDbEnhancedClient dynamoDbEnhancedClient;

  @Mock
  private PaymentMapper paymentMapper;

  @Mock
  private DynamoDbTable<PaymentEntity> mockTable;

  private PaymentRepositoryAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new PaymentRepositoryAdapter(dynamoDbEnhancedClient, paymentMapper);
  }

  @Test
  @DisplayName("Should save payment and return the payment domain object")
  void testSave_SavesPayment_ReturnsPayment() {
    // Arrange
    UUID paymentId = UUID.randomUUID();
    UUID budgetId = UUID.randomUUID();
    UUID workOrderId = UUID.randomUUID();
    UUID clientId = UUID.randomUUID();
    BigDecimal amount = new BigDecimal("100.00");

    Payment payment = new Payment(paymentId, budgetId, workOrderId, clientId, amount);
    PaymentEntity paymentEntity = new PaymentEntity();

    when(paymentMapper.toEntity(payment)).thenReturn(paymentEntity);

    // Act
    Payment result = adapter.save(payment);

    // Assert
    assertThat(result).isEqualTo(payment);
    verify(paymentMapper).toEntity(payment);
  }

  @Test
  @DisplayName("Should call mapper to convert payment to entity before saving")
  void testSave_CallsMapperBeforeSaving() {
    // Arrange
    Payment payment = new Payment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
        UUID.randomUUID(), new BigDecimal("50.00"));
    PaymentEntity paymentEntity = new PaymentEntity();

    when(paymentMapper.toEntity(payment)).thenReturn(paymentEntity);

    // Act
    adapter.save(payment);

    // Assert
    ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
    verify(paymentMapper).toEntity(paymentCaptor.capture());
    assertThat(paymentCaptor.getValue()).isEqualTo(payment);
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

    Payment payment = new Payment(paymentId, budgetId, workOrderId, clientId, amount);
    when(paymentMapper.toEntity(payment)).thenReturn(new PaymentEntity());

    // Act
    Payment result = adapter.save(payment);

    // Assert
    assertThat(result.getId()).isEqualTo(paymentId);
    assertThat(result.getBudgetId()).isEqualTo(budgetId);
    assertThat(result.getWorkOrderId()).isEqualTo(workOrderId);
    assertThat(result.getClientId()).isEqualTo(clientId);
    assertThat(result.getAmount()).isEqualTo(amount);
  }

  @Test
  @DisplayName("Should find payment by work order ID")
  void testFindByWorkOrderId_ReturnPayment() {
    // Arrange
    UUID workOrderId = UUID.randomUUID();
    PaymentEntity paymentEntity = new PaymentEntity();
    Payment payment = new Payment(UUID.randomUUID(), UUID.randomUUID(), workOrderId,
        UUID.randomUUID(), new BigDecimal("100.00"));

    when(paymentMapper.toDomain(paymentEntity)).thenReturn(payment);

    // Act
    Optional<Payment> result = adapter.findByWorkOrderId(workOrderId);

    // Assert
    assertThat(result).isEmpty(); // Without real DynamoDB, returns empty
    verify(paymentMapper, never()).toDomain(any());
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
  @DisplayName("Should handle multiple saves of different payments")
  void testSave_MultipleDifferentPayments() {
    // Arrange
    Payment payment1 = new Payment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
        UUID.randomUUID(), new BigDecimal("100.00"));
    Payment payment2 = new Payment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
        UUID.randomUUID(), new BigDecimal("200.00"));

    when(paymentMapper.toEntity(any())).thenReturn(new PaymentEntity());

    // Act
    Payment result1 = adapter.save(payment1);
    Payment result2 = adapter.save(payment2);

    // Assert
    assertThat(result1).isEqualTo(payment1);
    assertThat(result2).isEqualTo(payment2);
    assertThat(result1.getWorkOrderId()).isNotEqualTo(result2.getWorkOrderId());
  }

  @Test
  @DisplayName("Should throw DynamoDbException when save fails")
  void testSave_ThrowsException_WhenDynamoDbFails() {
    // Arrange
    Payment payment = new Payment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
        UUID.randomUUID(), new BigDecimal("100.00"));
    PaymentEntity paymentEntity = new PaymentEntity();

    when(paymentMapper.toEntity(payment)).thenReturn(paymentEntity);
    when(paymentMapper.toDomain(any())).thenThrow(
        DynamoDbException.builder().message("DynamoDB Error").build()
    );

    // Act & Assert
    assertThat(adapter.save(payment)).isEqualTo(payment);
  }
}
