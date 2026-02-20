package com.fiap.billing_service.infrastructure.config;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Utility class for structured logging with MDC context.
 * Helps add contextual information to logs for Datadog integration.
 */
@Component
public class StructuredLoggingConfig {

    public void addContext(String key, String value) {
        if (key != null && value != null) {
            MDC.put(key, value);
        }
    }

    public void addContext(Map<String, String> context) {
        if (context != null) {
            context.forEach((key, value) -> {
                if (key != null && value != null) {
                    MDC.put(key, value);
                }
            });
        }
    }

    public void removeContext(String key) {
        MDC.remove(key);
    }

    public void clearContext() {
        MDC.clear();
    }

    public void addOperationContext(String operationType, String entityId) {
        if (operationType != null) {
            MDC.put("operation.type", operationType);
        }
        if (entityId != null) {
            MDC.put("operation.entity_id", entityId);
        }
    }

    public void addUserContext(String userId, String userEmail) {
        if (userId != null) {
            MDC.put("user.id", userId);
        }
        if (userEmail != null) {
            MDC.put("user.email", userEmail);
        }
    }

    public void addHttpContext(String method, String path, String statusCode) {
        if (method != null) {
            MDC.put("http.method", method);
        }
        if (path != null) {
            MDC.put("http.path", path);
        }
        if (statusCode != null) {
            MDC.put("http.status_code", statusCode);
        }
    }
}
