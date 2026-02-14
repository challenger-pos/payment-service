package com.fiap.billing_service.infrastructure.adapter.in.messaging.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for receiving payment requests from the queue
 */
public class PaymentRequestDto {

    @NotNull(message = "Order ID is required")
    @JsonProperty("order_id")
    private UUID orderId;

    @NotNull(message = "Client ID is required")
    @JsonProperty("client_id")
    private UUID clientId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @JsonProperty("customer_email")
    private String customerEmail;

    @JsonProperty("customer_document")
    private String customerDocument;

    @JsonProperty("customer_name")
    private String customerName;

    @JsonProperty("description")
    private String description;

    public PaymentRequestDto() {
    }

    public PaymentRequestDto(UUID orderId, UUID clientId, BigDecimal amount) {
        this.orderId = orderId;
        this.clientId = clientId;
        this.amount = amount;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public UUID getClientId() {
        return clientId;
    }

    public void setClientId(UUID clientId) {
        this.clientId = clientId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerDocument() {
        return customerDocument;
    }

    public void setCustomerDocument(String customerDocument) {
        this.customerDocument = customerDocument;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
