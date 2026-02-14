package com.fiap.billing_service.infrastructure.adapter.out.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * DTO for Mercado Pago Order API Response
 */
public class MercadoPagoOrderResponse {

    @JsonProperty("id")
    private String id;

    @JsonProperty("status")
    private String status;

    @JsonProperty("external_reference")
    private String externalReference;

    @JsonProperty("total_amount")
    private BigDecimal totalAmount;

    @JsonProperty("payer")
    private Payer payer;

    @JsonProperty("transactions")
    private Transactions transactions;

    public MercadoPagoOrderResponse() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Payer getPayer() {
        return payer;
    }

    public void setPayer(Payer payer) {
        this.payer = payer;
    }

    public Transactions getTransactions() {
        return transactions;
    }

    public void setTransactions(Transactions transactions) {
        this.transactions = transactions;
    }

    // Inner classes
    public static class Payer {
        @JsonProperty("email")
        private String email;

        @JsonProperty("first_name")
        private String firstName;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }
    }

    public static class Transactions {
        @JsonProperty("payments")
        private Payment[] payments;

        public Payment[] getPayments() {
            return payments;
        }

        public void setPayments(Payment[] payments) {
            this.payments = payments;
        }
    }

    public static class Payment {
        @JsonProperty("id")
        private String id;

        @JsonProperty("status")
        private String status;

        @JsonProperty("status_detail")
        private String statusDetail;

        @JsonProperty("amount")
        private BigDecimal amount;

        @JsonProperty("payment_method")
        private PaymentMethod paymentMethod;

        @JsonProperty("point_of_interaction")
        private PointOfInteraction pointOfInteraction;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getStatusDetail() {
            return statusDetail;
        }

        public void setStatusDetail(String statusDetail) {
            this.statusDetail = statusDetail;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public PaymentMethod getPaymentMethod() {
            return paymentMethod;
        }

        public void setPaymentMethod(PaymentMethod paymentMethod) {
            this.paymentMethod = paymentMethod;
        }

        public PointOfInteraction getPointOfInteraction() {
            return pointOfInteraction;
        }

        public void setPointOfInteraction(PointOfInteraction pointOfInteraction) {
            this.pointOfInteraction = pointOfInteraction;
        }
    }

    public static class PaymentMethod {
        @JsonProperty("id")
        private String id;

        @JsonProperty("type")
        private String type;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    public static class PointOfInteraction {
        @JsonProperty("transaction_data")
        private TransactionData transactionData;

        public TransactionData getTransactionData() {
            return transactionData;
        }

        public void setTransactionData(TransactionData transactionData) {
            this.transactionData = transactionData;
        }
    }

    public static class TransactionData {
        @JsonProperty("qr_code")
        private String qrCode;

        @JsonProperty("qr_code_base64")
        private String qrCodeBase64;

        @JsonProperty("ticket_url")
        private String ticketUrl;

        public String getQrCode() {
            return qrCode;
        }

        public void setQrCode(String qrCode) {
            this.qrCode = qrCode;
        }

        public String getQrCodeBase64() {
            return qrCodeBase64;
        }

        public void setQrCodeBase64(String qrCodeBase64) {
            this.qrCodeBase64 = qrCodeBase64;
        }

        public String getTicketUrl() {
            return ticketUrl;
        }

        public void setTicketUrl(String ticketUrl) {
            this.ticketUrl = ticketUrl;
        }
    }
}
