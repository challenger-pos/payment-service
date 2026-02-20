package com.fiap.billing_service.infrastructure.adapter.in.web.controller;

import static org.assertj.core.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@DisplayName("HealthController Tests")
class HealthControllerTest {

  private HealthController controller;

  @BeforeEach
  void setUp() {
    controller = new HealthController();
    MDC.clear();
  }

  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  @Test
  @DisplayName("Should return health check with UP status")
  void testHealth_ReturnsUpStatus() {
    // Act
    ResponseEntity<Map<String, String>> response = controller.health();

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("status")).isEqualTo("UP");
  }

  @Test
  @DisplayName("Should return correct service name in response")
  void testHealth_ReturnsServiceName() {
    // Act
    ResponseEntity<Map<String, String>> response = controller.health();

    // Assert
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("service")).isEqualTo("billing-service");
  }

  @Test
  @DisplayName("Should return informative message about SQS polling")
  void testHealth_ReturnsMessageAboutSQS() {
    // Act
    ResponseEntity<Map<String, String>> response = controller.health();

    // Assert
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("message"))
        .isEqualTo("Service is running and polling SQS queue");
  }

  @Test
  @DisplayName("Should include correlation ID in response when available")
  void testHealth_IncludesCorrelationIdWhenPresent() {
    // Arrange
    String correlationId = "test-correlation-id-12345";
    MDC.put("correlationId", correlationId);

    // Act
    ResponseEntity<Map<String, String>> response = controller.health();

    // Assert
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("correlationId")).isEqualTo(correlationId);
    assertThat(response.getBody()).hasSize(4); // status, service, message, correlationId
  }

  @Test
  @DisplayName("Should not include correlation ID when not present in MDC")
  void testHealth_DoesNotIncludeCorrelationIdWhenAbsent() {
    // Arrange
    MDC.clear();

    // Act
    ResponseEntity<Map<String, String>> response = controller.health();

    // Assert
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody()).doesNotContainKey("correlationId");
    assertThat(response.getBody()).hasSize(3); // status, service, message
  }

  @Test
  @DisplayName("Should return response entity with body")
  void testHealth_ResponseBodyNotNull() {
    // Act
    ResponseEntity<Map<String, String>> response = controller.health();

    // Assert
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody()).isInstanceOf(Map.class);
  }

  @Test
  @DisplayName("Should contain all expected fields")
  void testHealth_ContainsAllExpectedFields() {
    // Act
    ResponseEntity<Map<String, String>> response = controller.health();

    // Assert
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody()).containsKeys("status", "service", "message");
  }

  @Test
  @DisplayName("Should return OK status code")
  void testHealth_ReturnsOkStatusCode() {
    // Act
    ResponseEntity<Map<String, String>> response = controller.health();

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getStatusCode().value()).isEqualTo(200);
  }

  @Test
  @DisplayName("Should handle multiple health check calls with different correlation IDs")
  void testHealth_MultipleCallsWithDifferentCorrelationIds() {
    // First call
    MDC.put("correlationId", "corr-id-1");
    ResponseEntity<Map<String, String>> response1 = controller.health();

    MDC.clear();

    // Second call
    MDC.put("correlationId", "corr-id-2");
    ResponseEntity<Map<String, String>> response2 = controller.health();

    // Assert
    assertThat(response1.getBody().get("correlationId")).isEqualTo("corr-id-1");
    assertThat(response2.getBody().get("correlationId")).isEqualTo("corr-id-2");
  }

  @Test
  @DisplayName("Should verify response is consistent across calls")
  void testHealth_ResponseConsistent() {
    // Act
    ResponseEntity<Map<String, String>> response1 = controller.health();
    ResponseEntity<Map<String, String>> response2 = controller.health();

    // Assert
    assertThat(response1.getStatusCode()).isEqualTo(response2.getStatusCode());
    assertThat(response1.getBody().get("status")).isEqualTo(response2.getBody().get("status"));
    assertThat(response1.getBody().get("service")).isEqualTo(response2.getBody().get("service"));
  }
}
