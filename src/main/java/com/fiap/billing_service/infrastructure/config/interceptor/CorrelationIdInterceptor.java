package com.fiap.billing_service.infrastructure.config.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * Interceptor to add correlation ID to all requests
 */
@Component
public class CorrelationIdInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdInterceptor.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Check if correlation ID exists in request header
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        
        // If not present, generate a new one
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }
        
        // Add to MDC for logging
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        
        // Add to response header
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        
        log.debug("Request correlation ID: {}", correlationId);
        
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // Clean up MDC
        MDC.remove(CORRELATION_ID_MDC_KEY);
    }
}
