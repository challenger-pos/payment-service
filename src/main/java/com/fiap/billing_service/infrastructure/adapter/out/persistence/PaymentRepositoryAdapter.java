package com.fiap.billing_service.infrastructure.adapter.out.persistence;

import com.fiap.billing_service.application.port.out.PaymentRepositoryPort;
import com.fiap.billing_service.domain.entity.Payment;
import com.fiap.billing_service.infrastructure.adapter.out.persistence.mapper.PaymentMapper;
import com.fiap.billing_service.infrastructure.adapter.out.persistence.repository.SpringDataPaymentRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PaymentRepositoryAdapter implements PaymentRepositoryPort {

  private final SpringDataPaymentRepository repository;
  private final PaymentMapper mapper;

  public PaymentRepositoryAdapter(SpringDataPaymentRepository repository, PaymentMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public Payment save(Payment payment) {
    var entity = mapper.toEntity(payment);
    repository.save(entity);
    return payment;
  }

  @Override
  public Optional<Payment> findByWorkOrderId(UUID workOrderId) {
    return repository.findByWorkOrderId(workOrderId).map(mapper::toDomain);
  }
}
