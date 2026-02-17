package com.fiap.billing_service.infrastructure.adapter.in.messaging.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/** DTO for receiving payment requests from the queue */
public class PaymentRequestDto {

  @NotNull
  @JsonProperty("budget_id")
  private UUID budgetId;

  @NotNull(message = "WorkOrder ID is required")
  @JsonProperty("work_order_id")
  private UUID workOrderId;

  @NotNull(message = "Client ID is required")
  @JsonProperty("client_id")
  private UUID clientId;

  @NotNull(message = "order is required")
  @JsonProperty("order_request")
  private OrderRequest orderRequest;

  private String description;

  public PaymentRequestDto() {}

  public PaymentRequestDto(
      UUID workOrderId, UUID clientId, UUID budgetId, OrderRequest orderRequest) {
    this.workOrderId = workOrderId;
    this.clientId = clientId;
    this.budgetId = budgetId;
    this.orderRequest = orderRequest;
  }

  public UUID getWorkOrderId() {
    return workOrderId;
  }

  public void setWorkOrderId(UUID workOrderId) {
    this.workOrderId = workOrderId;
  }

  public UUID getClientId() {
    return clientId;
  }

  public void setClientId(UUID clientId) {
    this.clientId = clientId;
  }

  public OrderRequest getOrderRequest() {
    return orderRequest;
  }

  public void setOrderRequest(OrderRequest orderRequest) {
    this.orderRequest = orderRequest;
  }

  public UUID getBudgetId() {
    return budgetId;
  }

  public void setBudgetId(UUID budgetId) {
    this.budgetId = budgetId;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  /** DTO for Mercado Pago Order API Request */
  public class OrderRequest {

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

    public OrderRequest() {}

    public OrderRequest(
        String externalReference, BigDecimal amount, String email, String firstName) {
      this.externalReference = externalReference;
      this.totalAmount = amount.toString();
      this.payer = new OrderRequest.Payer(email, firstName);

      Payment payment = new OrderRequest.Payment(amount.toString());
      this.transactions = new OrderRequest.Transactions(new Payment[] {payment});
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
      private String firstName = "APRO";

      public Payer() {}

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

      public Transactions() {}

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
      private OrderRequest.PaymentMethod paymentMethod;

      public Payment() {}

      public Payment(String amount) {
        this.amount = amount;
        this.paymentMethod = new OrderRequest.PaymentMethod("pix", "bank_transfer");
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

      public PaymentMethod() {}

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
}
