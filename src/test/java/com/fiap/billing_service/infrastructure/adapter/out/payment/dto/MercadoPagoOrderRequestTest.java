package com.fiap.billing_service.infrastructure.adapter.out.payment.dto;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MercadoPagoOrderRequest Tests")
class MercadoPagoOrderRequestTest {

  private MercadoPagoOrderRequest request;

  @BeforeEach
  void setUp() {
    request = new MercadoPagoOrderRequest();
  }

  @Test
  @DisplayName("Should construct with default type as 'online'")
  void testConstructor_DefaultType_IsOnline() {
    assertThat(request.getType()).isEqualTo("online");
  }

  @Test
  @DisplayName("Should construct with all parameters")
  void testConstructor_WithParameters_InitializesAllFields() {
    // Arrange
    String externalReference = "ext_order_123";
    BigDecimal amount = new BigDecimal("150.00");
    String email = "customer@example.com";
<<<<<<< HEAD
    String firstName = "APRO";

    // Act
    request = new MercadoPagoOrderRequest(externalReference, amount, email, firstName);
=======

    // Act
    request = new MercadoPagoOrderRequest(externalReference, amount, email);
>>>>>>> 874da5d659f8f0227b13a5ef37e537fd54c18408

    // Assert
    assertThat(request.getExternalReference()).isEqualTo(externalReference);
    assertThat(request.getTotalAmount()).isEqualTo("150.00");
    assertThat(request.getPayer()).isNotNull();
    assertThat(request.getPayer().getEmail()).isEqualTo(email);
    assertThat(request.getPayer().getFirstName()).isEqualTo("APRO");
    assertThat(request.getTransactions()).isNotNull();
    assertThat(request.getTransactions().getPayments()).hasSize(1);
  }

  @Test
  @DisplayName("Should set external reference")
  void testSetExternalReference() {
    // Act
    request.setExternalReference("ext_456");

    // Assert
    assertThat(request.getExternalReference()).isEqualTo("ext_456");
  }

  @Test
  @DisplayName("Should set total amount")
  void testSetTotalAmount() {
    // Act
    request.setTotalAmount("200.50");

    // Assert
    assertThat(request.getTotalAmount()).isEqualTo("200.50");
  }

  @Test
  @DisplayName("Should set type")
  void testSetType() {
    // Act
    request.setType("offline");

    // Assert
    assertThat(request.getType()).isEqualTo("offline");
  }

  @Test
  @DisplayName("Should construct nested Payer with email and first name")
  void testPayer_Constructor_InitializesFields() {
    // Arrange
    String email = "user@example.com";
    String firstName = "John";

    // Act
    MercadoPagoOrderRequest.Payer payer = new MercadoPagoOrderRequest.Payer(email, firstName);

    // Assert
    assertThat(payer.getEmail()).isEqualTo(email);
    assertThat(payer.getFirstName()).isEqualTo(firstName);
  }

  @Test
  @DisplayName("Should set and get payer")
  void testSetPayer() {
    // Arrange
    MercadoPagoOrderRequest.Payer payer =
        new MercadoPagoOrderRequest.Payer("payer@example.com", "Jane");

    // Act
    request.setPayer(payer);

    // Assert
    assertThat(request.getPayer()).isEqualTo(payer);
    assertThat(request.getPayer().getEmail()).isEqualTo("payer@example.com");
    assertThat(request.getPayer().getFirstName()).isEqualTo("Jane");
  }

  @Test
  @DisplayName("Should construct nested Transactions with payments array")
  void testTransactions_Constructor_StoresPayments() {
    // Arrange
<<<<<<< HEAD
    MercadoPagoOrderRequest.Payment payment1 = new MercadoPagoOrderRequest.Payment("100.00");
    MercadoPagoOrderRequest.Payment payment2 = new MercadoPagoOrderRequest.Payment("50.00");
=======
    MercadoPagoOrderRequest.Payment payment1 =
        new MercadoPagoOrderRequest.Payment("100.00");
    MercadoPagoOrderRequest.Payment payment2 =
        new MercadoPagoOrderRequest.Payment("50.00");
>>>>>>> 874da5d659f8f0227b13a5ef37e537fd54c18408
    MercadoPagoOrderRequest.Payment[] payments = {payment1, payment2};

    // Act
    MercadoPagoOrderRequest.Transactions transactions =
        new MercadoPagoOrderRequest.Transactions(payments);

    // Assert
    assertThat(transactions.getPayments()).hasSize(2);
    assertThat(transactions.getPayments()[0].getAmount()).isEqualTo("100.00");
    assertThat(transactions.getPayments()[1].getAmount()).isEqualTo("50.00");
  }

  @Test
  @DisplayName("Should set and get transactions")
  void testSetTransactions() {
    // Arrange
<<<<<<< HEAD
    MercadoPagoOrderRequest.Payment payment = new MercadoPagoOrderRequest.Payment("75.00");
=======
    MercadoPagoOrderRequest.Payment payment =
        new MercadoPagoOrderRequest.Payment("75.00");
>>>>>>> 874da5d659f8f0227b13a5ef37e537fd54c18408
    MercadoPagoOrderRequest.Transactions transactions =
        new MercadoPagoOrderRequest.Transactions(new MercadoPagoOrderRequest.Payment[] {payment});

    // Act
    request.setTransactions(transactions);

    // Assert
    assertThat(request.getTransactions()).isEqualTo(transactions);
    assertThat(request.getTransactions().getPayments()).hasSize(1);
    assertThat(request.getTransactions().getPayments()[0].getAmount()).isEqualTo("75.00");
  }

  @Test
  @DisplayName("Should construct nested Payment with amount and default payment method")
  void testPayment_Constructor_CreatesPaymentMethod() {
    // Act
    MercadoPagoOrderRequest.Payment payment = new MercadoPagoOrderRequest.Payment("100.00");

    // Assert
    assertThat(payment.getAmount()).isEqualTo("100.00");
    assertThat(payment.getPaymentMethod()).isNotNull();
    assertThat(payment.getPaymentMethod().getId()).isEqualTo("pix");
    assertThat(payment.getPaymentMethod().getType()).isEqualTo("bank_transfer");
  }

  @Test
  @DisplayName("Should set and get payment method")
  void testPayment_SetPaymentMethod() {
    // Arrange
    MercadoPagoOrderRequest.Payment payment = new MercadoPagoOrderRequest.Payment("50.00");
    MercadoPagoOrderRequest.PaymentMethod paymentMethod =
        new MercadoPagoOrderRequest.PaymentMethod("card", "credit_card");

    // Act
    payment.setPaymentMethod(paymentMethod);

    // Assert
    assertThat(payment.getPaymentMethod()).isEqualTo(paymentMethod);
    assertThat(payment.getPaymentMethod().getId()).isEqualTo("card");
    assertThat(payment.getPaymentMethod().getType()).isEqualTo("credit_card");
  }

  @Test
  @DisplayName("Should construct nested PaymentMethod with id and type")
  void testPaymentMethod_Constructor_InitializesFields() {
    // Act
    MercadoPagoOrderRequest.PaymentMethod method =
        new MercadoPagoOrderRequest.PaymentMethod("boleto", "bank_transfer");

    // Assert
    assertThat(method.getId()).isEqualTo("boleto");
    assertThat(method.getType()).isEqualTo("bank_transfer");
  }

  @Test
  @DisplayName("Should construct complete order request with nested objects")
  void testFullOrderConstruction_BuildsCompleteObjectGraph() {
    // Act
<<<<<<< HEAD
    request =
        new MercadoPagoOrderRequest(
            "order_789", new BigDecimal("299.99"), "buyer@test.com", "APRO");
=======
    request = new MercadoPagoOrderRequest("order_789", new BigDecimal("299.99"), "buyer@test.com");
>>>>>>> 874da5d659f8f0227b13a5ef37e537fd54c18408

    // Assert - verify entire object graph
    assertThat(request.getType()).isEqualTo("online");
    assertThat(request.getExternalReference()).isEqualTo("order_789");
    assertThat(request.getTotalAmount()).isEqualTo("299.99");

    // Payer
    assertThat(request.getPayer()).isNotNull();
    assertThat(request.getPayer().getEmail()).isEqualTo("buyer@test.com");
    assertThat(request.getPayer().getFirstName()).isEqualTo("APRO");

    // Transactions and Payments
    assertThat(request.getTransactions()).isNotNull();
    assertThat(request.getTransactions().getPayments()).hasSize(1);
    assertThat(request.getTransactions().getPayments()[0].getAmount()).isEqualTo("299.99");

    // PaymentMethod
    MercadoPagoOrderRequest.PaymentMethod method =
        request.getTransactions().getPayments()[0].getPaymentMethod();
    assertThat(method).isNotNull();
    assertThat(method.getId()).isEqualTo("pix");
    assertThat(method.getType()).isEqualTo("bank_transfer");
  }
}
