package com.fiap.billing_service.infrastructure.adapter.out.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * DTO for Mercado Pago Order API Request
 */
public class MercadoPagoOrderRequest {

    @JsonProperty("type")
    private String type = "online";

    @JsonProperty("external_reference")
    private String externalReference;

    @JsonProperty("total_amount")
    private String totalAmount;

    @JsonProperty("payer")
    private Payer payer;

    @JsonProperty("transactions")
    private Transactions transactions;

    public MercadoPagoOrderRequest() {
    }

    public MercadoPagoOrderRequest(String externalReference, BigDecimal amount, String email) {
        this.externalReference = externalReference;
        this.totalAmount = amount.toString();
        this.payer = new Payer(email, "APRO");
        
        Payment payment = new Payment(amount.toString());
        this.transactions = new Transactions(new Payment[]{payment});
    }

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    public String getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(String totalAmount) {
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

        public Payer() {
        }

        public Payer(String email, String firstName) {
            this.email = email;
            this.firstName = firstName;
        }

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

        public Transactions() {
        }

        public Transactions(Payment[] payments) {
            this.payments = payments;
        }

        public Payment[] getPayments() {
            return payments;
        }

        public void setPayments(Payment[] payments) {
            this.payments = payments;
        }
    }

    public static class Payment {
        @JsonProperty("amount")
        private String amount;

        @JsonProperty("payment_method")
        private PaymentMethod paymentMethod;

        public Payment() {
        }

        public Payment(String amount) {
            this.amount = amount;
            this.paymentMethod = new PaymentMethod("pix", "bank_transfer");
        }

        public String getAmount() {
            return amount;
        }

        public void setAmount(String amount) {
            this.amount = amount;
        }

        public PaymentMethod getPaymentMethod() {
            return paymentMethod;
        }

        public void setPaymentMethod(PaymentMethod paymentMethod) {
            this.paymentMethod = paymentMethod;
        }
    }

    public static class PaymentMethod {
        @JsonProperty("id")
        private String id;

        @JsonProperty("type")
        private String type;

        public PaymentMethod() {
        }

        public PaymentMethod(String id, String type) {
            this.id = id;
            this.type = type;
        }

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
}
