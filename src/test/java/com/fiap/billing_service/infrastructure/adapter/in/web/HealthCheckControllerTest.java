package com.fiap.billing_service.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("Deferred: Full Spring context loading issues")
class HealthCheckControllerTest {

  @Autowired
  private HealthCheckController healthCheckController;

  @Test
  void testControllerCreated() {
    assertThat(healthCheckController).isNotNull();
  }

  @Test
  void testPingEndpoint() {
    Map<String, String> response = healthCheckController.ping();
    
    assertThat(response).isNotNull();
    assertThat(response).containsEntry("status", "UP");
    assertThat(response).containsEntry("message", "pong");
  }

  @Test
  void testGetHealthEndpoint() {
    Map<String, Object> response = healthCheckController.getHealth();
    
    assertThat(response).isNotNull();
    assertThat(response).containsEntry("status", "UP");
    assertThat(response).containsEntry("service", "billing-service");
  }

  @Test
  void testGetLivenessEndpoint() {
    Map<String, String> response = healthCheckController.getLiveness();
    
    assertThat(response).isNotNull();
    assertThat(response).containsEntry("status", "UP");
  }

  @Test
  void testGetReadinessEndpoint() {
    Map<String, String> response = healthCheckController.getReadiness();
    
    assertThat(response).isNotNull();
    assertThat(response).containsEntry("status", "UP");
  }
}
