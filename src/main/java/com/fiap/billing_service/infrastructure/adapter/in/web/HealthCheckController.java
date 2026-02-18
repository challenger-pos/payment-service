package com.fiap.billing_service.infrastructure.adapter.in.web;

import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Custom Health Check Endpoints for Billing Service
 *
 * <p>Provides additional health check endpoints beyond Spring Boot Actuator defaults
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthCheckController {

  /** Simple ping endpoint for quick health checks */
  @GetMapping("/ping")
  public Map<String, String> ping() {
    Map<String, String> response = new HashMap<>();
    response.put("status", "UP");
    response.put("message", "pong");
    return response;
  }

  /** Get basic application health status */
  @GetMapping
  public Map<String, Object> getHealth() {
    Map<String, Object> response = new HashMap<>();
    response.put("status", "UP");
    response.put("service", "billing-service");
    return response;
  }

  /** Get liveness probe status - Kubernetes uses this to determine if the pod should be restarted */
  @GetMapping("/liveness")
  public Map<String, String> getLiveness() {
    Map<String, String> response = new HashMap<>();
    response.put("status", "UP");
    return response;
  }

  /** Get readiness probe status - Kubernetes uses this to determine if the pod is ready to receive traffic */
  @GetMapping("/readiness")
  public Map<String, String> getReadiness() {
    Map<String, String> response = new HashMap<>();
    response.put("status", "UP");
    return response;
  }
}
