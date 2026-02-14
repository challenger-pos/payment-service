package com.fiap.billing_service.infrastructure.adapter.in.web.dto;

import java.util.UUID;

public class PaymentRequest {
    private UUID workOrderId;

    public UUID getWorkOrderId() {
        return workOrderId;
    }

    public void setWorkOrderId(UUID workOrderId) {
        this.workOrderId = workOrderId;
    }
}
