package com.fiap.billing_service.infrastructure.adapter.in.web.controller;

import io.opentracing.Span;
import io.opentracing.util.GlobalTracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Health check controller
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    @GetMapping
    public ResponseEntity<Map<String, String>> health() {
        Span span = GlobalTracer.get().activeSpan();
        if (span != null) {
            span.setTag("operation.type", "health");
        }
        String correlationId = MDC.get("correlationId");
        log.debug("[CorrelationId: {}] Health check requested", correlationId);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "billing-service");
        response.put("message", "Service is running and polling SQS queue");
        
        if (correlationId != null) {
            response.put("correlationId", correlationId);
        }
        
        return ResponseEntity.ok(response);
    }
}
