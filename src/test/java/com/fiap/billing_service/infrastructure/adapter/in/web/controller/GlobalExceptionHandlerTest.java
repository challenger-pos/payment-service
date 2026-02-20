package com.fiap.billing_service.infrastructure.adapter.in.web.controller;

import static org.assertj.core.api.Assertions.*;

import com.fiap.billing_service.domain.exception.PaymentProcessingException;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler exceptionHandler;

  @BeforeEach
  void setUp() {
    exceptionHandler = new GlobalExceptionHandler();
    MDC.clear();
  }

  @Test
  @DisplayName("Should handle PaymentProcessingException with BAD_REQUEST status")
  void testHandlePaymentProcessingException_ReturnsBadRequest() {
    // Arrange
    PaymentProcessingException exception =
        new PaymentProcessingException("Payment validation failed");

    // Act
    ResponseEntity<Map<String, Object>> response =
        exceptionHandler.handlePaymentProcessingException(exception);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("status")).isEqualTo(400);
    assertThat(response.getBody().get("error")).isEqualTo("Bad Request");
    assertThat(response.getBody().get("message")).isEqualTo("Payment validation failed");
  }

  @Test
  @DisplayName("Should include correlation ID in PaymentProcessingException response when available")
  void testHandlePaymentProcessingException_IncludesCorrelationId() {
    // Arrange
    String correlationId = "corr-12345-abcde";
    MDC.put("correlationId", correlationId);

    PaymentProcessingException exception =
        new PaymentProcessingException("Insufficient funds");

    // Act
    ResponseEntity<Map<String, Object>> response =
        exceptionHandler.handlePaymentProcessingException(exception);

    // Assert
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("correlationId")).isEqualTo(correlationId);
    assertThat(response.getBody().get("message")).isEqualTo("Insufficient funds");

    MDC.clear();
  }

  @Test
  @DisplayName(
      "Should not include correlation ID in PaymentProcessingException response when not set")
  void testHandlePaymentProcessingException_NoCorrelationId() {
    // Arrange
    MDC.clear();
    PaymentProcessingException exception = new PaymentProcessingException("Invalid gateway");

    // Act
    ResponseEntity<Map<String, Object>> response =
        exceptionHandler.handlePaymentProcessingException(exception);

    // Assert
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody()).doesNotContainKey("correlationId");
  }

  @Test
  @DisplayName("Should handle generic Exception with INTERNAL_SERVER_ERROR status")
  void testHandleGenericException_ReturnsInternalServerError() {
    // Arrange
    Exception exception = new RuntimeException("Database connection failed");

    // Act
    ResponseEntity<Map<String, Object>> response = exceptionHandler.handleGenericException(exception);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("status")).isEqualTo(500);
    assertThat(response.getBody().get("error")).isEqualTo("Internal Server Error");
    assertThat(response.getBody().get("message")).isEqualTo("An unexpected error occurred: Database connection failed");
  }

  @Test
  @DisplayName("Should include timestamp in error response")
  void testHandleGenericException_IncludesTimestamp() {
    // Arrange
    Exception exception = new IllegalArgumentException("Invalid parameter");

    // Act
    ResponseEntity<Map<String, Object>> response = exceptionHandler.handleGenericException(exception);

    // Assert
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("timestamp")).isNotNull();
    assertThat(response.getBody().get("timestamp")).isInstanceOf(LocalDateTime.class);
  }

  @Test
  @DisplayName("Should include all required fields in error response")
  void testErrorResponse_ContainsAllRequiredFields() {
    // Arrange
    String correlationId = "corr-67890-vwxyz";
    MDC.put("correlationId", correlationId);

    PaymentProcessingException exception =
        new PaymentProcessingException("Payment already processed");

    // Act
    ResponseEntity<Map<String, Object>> response =
        exceptionHandler.handlePaymentProcessingException(exception);

    // Assert
    Map<String, Object> body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body).containsKeys("timestamp", "status", "error", "message", "correlationId");
    assertThat(body.get("timestamp")).isInstanceOf(LocalDateTime.class);
    assertThat(body.get("status")).isInstanceOf(Integer.class);
    assertThat(body.get("error")).isInstanceOf(String.class);
    assertThat(body.get("message")).isInstanceOf(String.class);
    assertThat(body.get("correlationId")).isInstanceOf(String.class);

    MDC.clear();
  }
}
