package com.fiap.billing_service.infrastructure.adapter.in.web.controller;

import com.fiap.billing_service.domain.exception.PaymentProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(PaymentProcessingException.class)
    public ResponseEntity<Map<String, Object>> handlePaymentProcessingException(PaymentProcessingException ex) {
        String correlationId = MDC.get("correlationId");
        log.error("[CorrelationId: {}] Payment processing error: {}", correlationId, ex.getMessage(), ex);
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST, correlationId);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        String correlationId = MDC.get("correlationId");
        log.error("[CorrelationId: {}] Unexpected error occurred: {}", correlationId, ex.getMessage(), ex);
        return buildErrorResponse("An unexpected error occurred: " + ex.getMessage(), 
            HttpStatus.INTERNAL_SERVER_ERROR, correlationId);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(String message, HttpStatus status, String correlationId) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", status.value());
        error.put("error", status.getReasonPhrase());
        error.put("message", message);
        
        if (correlationId != null) {
            error.put("correlationId", correlationId);
        }
        
        return ResponseEntity.status(status).body(error);
    }
}
