package com.fiap.billing_service.infrastructure.adapter.out.persistence.repository;

import com.fiap.billing_service.infrastructure.adapter.out.persistence.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataPaymentRepository extends JpaRepository<PaymentEntity, UUID> {
    Optional<PaymentEntity> findByWorkOrderId(UUID workOrderId);
}
