package com.fiap.billing_service.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("HealthCheckController - Unit Tests")
class HealthCheckUnitTest {

  private HealthCheckController controller = new HealthCheckController();

  @Test
  @DisplayName("ping endpoint should return UP status with pong message")
  void testPingReturnsUpStatus() {
    Map<String, String> response = controller.ping();

    assertThat(response).isNotNull();
    assertThat(response).hasSize(2);
    assertThat(response.get("status")).isEqualTo("UP");
    assertThat(response.get("message")).isEqualTo("pong");
  }

  @Test
  @DisplayName("getHealth endpoint should return service name")
  void testGetHealthReturnsServiceName() {
    Map<String, Object> response = controller.getHealth();

    assertThat(response).isNotNull();
    assertThat(response).hasSize(2);
    assertThat(response.get("status")).isEqualTo("UP");
    assertThat(response.get("service")).isEqualTo("billing-service");
  }

  @Test
  @DisplayName("getLiveness should return UP status")
  void testLivenessProbeReturnsUp() {
    Map<String, String> response = controller.getLiveness();

    assertThat(response).isNotNull();
    assertThat(response).containsEntry("status", "UP");
  }

  @Test
  @DisplayName("getReadiness should return UP status")
  void testReadinessProbeReturnsUp() {
    Map<String, String> response = controller.getReadiness();

    assertThat(response).isNotNull();
    assertThat(response).containsEntry("status", "UP");
  }

  @Test
  @DisplayName("All health endpoints should have consistent UP status")
  void testAllHealthEndpointsReturnUpStatus() {
    assertThat(controller.ping().get("status")).isEqualTo("UP");
    assertThat(controller.getHealth().get("status")).isEqualTo("UP");
    assertThat(controller.getLiveness().get("status")).isEqualTo("UP");
    assertThat(controller.getReadiness().get("status")).isEqualTo("UP");
  }
}
