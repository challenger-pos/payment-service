package com.fiap.billing_service.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fiap.billing_service.domain.entity.Payment;
import com.fiap.billing_service.infrastructure.adapter.out.persistence.entity.PaymentEntity;
import com.fiap.billing_service.infrastructure.adapter.out.persistence.mapper.PaymentMapper;
import com.fiap.billing_service.infrastructure.adapter.out.persistence.repository.SpringDataPaymentRepository;
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
@DisplayName("PaymentRepositoryAdapter Tests")
class PaymentRepositoryAdapterTest {

  @Mock private SpringDataPaymentRepository repository;

  @Mock private PaymentMapper mapper;

  private PaymentRepositoryAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new PaymentRepositoryAdapter(repository, mapper);
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

    when(mapper.toEntity(payment)).thenReturn(paymentEntity);

    // Act
    Payment result = adapter.save(payment);

    // Assert
    assertThat(result).isEqualTo(payment);
    verify(repository).save(paymentEntity);
    verify(mapper).toEntity(payment);
  }

  @Test
  @DisplayName("Should call mapper to convert payment to entity before saving")
  void testSave_CallsMapperBeforeSaving() {
    // Arrange
    Payment payment = new Payment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
        UUID.randomUUID(), new BigDecimal("50.00"));
    PaymentEntity paymentEntity = new PaymentEntity();

    when(mapper.toEntity(payment)).thenReturn(paymentEntity);

    // Act
    adapter.save(payment);

    // Assert
    ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
    verify(mapper).toEntity(paymentCaptor.capture());
    assertThat(paymentCaptor.getValue()).isEqualTo(payment);
  }

  @Test
  @DisplayName("Should use repository to save the entity")
  void testSave_UsesRepositoryToSaveEntity() {
    // Arrange
    Payment payment = new Payment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
        UUID.randomUUID(), new BigDecimal("75.00"));
    PaymentEntity paymentEntity = new PaymentEntity();

    when(mapper.toEntity(payment)).thenReturn(paymentEntity);

    // Act
    adapter.save(payment);

    // Assert
    ArgumentCaptor<PaymentEntity> entityCaptor = ArgumentCaptor.forClass(PaymentEntity.class);
    verify(repository).save(entityCaptor.capture());
    assertThat(entityCaptor.getValue()).isEqualTo(paymentEntity);
  }

  @Test
  @DisplayName("Should find payment by work order ID")
  void testFindByWorkOrderId_ReturnPayment() {
    // Arrange
    UUID workOrderId = UUID.randomUUID();
    PaymentEntity paymentEntity = new PaymentEntity();
    Payment payment = new Payment(UUID.randomUUID(), UUID.randomUUID(), workOrderId,
        UUID.randomUUID(), new BigDecimal("100.00"));

    when(repository.findByWorkOrderId(workOrderId)).thenReturn(Optional.of(paymentEntity));
    when(mapper.toDomain(paymentEntity)).thenReturn(payment);

    // Act
    Optional<Payment> result = adapter.findByWorkOrderId(workOrderId);

    // Assert
    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo(payment);
    verify(repository).findByWorkOrderId(workOrderId);
    verify(mapper).toDomain(paymentEntity);
  }

  @Test
  @DisplayName("Should return empty optional when payment not found")
  void testFindByWorkOrderId_NotFound_ReturnEmpty() {
    // Arrange
    UUID workOrderId = UUID.randomUUID();

    when(repository.findByWorkOrderId(workOrderId)).thenReturn(Optional.empty());

    // Act
    Optional<Payment> result = adapter.findByWorkOrderId(workOrderId);

    // Assert
    assertThat(result).isEmpty();
    verify(repository).findByWorkOrderId(workOrderId);
    verify(mapper, never()).toDomain(any());
  }

  @Test
  @DisplayName("Should invoke repository with correct work order ID")
  void testFindByWorkOrderId_UsesCorrectWorkOrderId() {
    // Arrange
    UUID workOrderId = UUID.randomUUID();
    when(repository.findByWorkOrderId(workOrderId)).thenReturn(Optional.empty());

    // Act
    adapter.findByWorkOrderId(workOrderId);

    // Assert
    ArgumentCaptor<UUID> uuidCaptor = ArgumentCaptor.forClass(UUID.class);
    verify(repository).findByWorkOrderId(uuidCaptor.capture());
    assertThat(uuidCaptor.getValue()).isEqualTo(workOrderId);
  }

  @Test
  @DisplayName("Should call mapper to convert entity to domain when found")
  void testFindByWorkOrderId_CallsMapperWhenFound() {
    // Arrange
    UUID workOrderId = UUID.randomUUID();
    PaymentEntity paymentEntity = new PaymentEntity();
    Payment payment = new Payment(UUID.randomUUID(), UUID.randomUUID(), workOrderId,
        UUID.randomUUID(), new BigDecimal("200.00"));

    when(repository.findByWorkOrderId(workOrderId)).thenReturn(Optional.of(paymentEntity));
    when(mapper.toDomain(paymentEntity)).thenReturn(payment);

    // Act
    adapter.findByWorkOrderId(workOrderId);

    // Assert
    verify(mapper).toDomain(paymentEntity);
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
    when(mapper.toEntity(payment)).thenReturn(new PaymentEntity());

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
  @DisplayName("Should handle multiple saves of different payments")
  void testSave_MultipleDifferentPayments() {
    // Arrange
    Payment payment1 = new Payment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
        UUID.randomUUID(), new BigDecimal("100.00"));
    Payment payment2 = new Payment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
        UUID.randomUUID(), new BigDecimal("200.00"));

    when(mapper.toEntity(any())).thenReturn(new PaymentEntity());

    // Act
    Payment result1 = adapter.save(payment1);
    Payment result2 = adapter.save(payment2);

    // Assert
    assertThat(result1).isEqualTo(payment1);
    assertThat(result2).isEqualTo(payment2);
    verify(repository, times(2)).save(any());
  }

  @Test
  @DisplayName("Should handle concurrent find operations for same work order ID")
  void testFindByWorkOrderId_ConcurrentCalls() {
    // Arrange
    UUID workOrderId = UUID.randomUUID();
    PaymentEntity paymentEntity = new PaymentEntity();
    Payment payment = new Payment(UUID.randomUUID(), UUID.randomUUID(), workOrderId,
        UUID.randomUUID(), new BigDecimal("100.00"));

    when(repository.findByWorkOrderId(workOrderId)).thenReturn(Optional.of(paymentEntity));
    when(mapper.toDomain(paymentEntity)).thenReturn(payment);

    // Act
    Optional<Payment> result1 = adapter.findByWorkOrderId(workOrderId);
    Optional<Payment> result2 = adapter.findByWorkOrderId(workOrderId);

    // Assert
    assertThat(result1).isPresent();
    assertThat(result2).isPresent();
    assertThat(result1.get()).isEqualTo(result2.get());
    verify(repository, times(2)).findByWorkOrderId(workOrderId);
  }
}
