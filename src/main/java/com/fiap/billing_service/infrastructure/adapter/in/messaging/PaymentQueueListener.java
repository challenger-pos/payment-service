package com.fiap.billing_service.infrastructure.adapter.in.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.billing_service.application.port.in.ProcessPaymentUseCase;
import com.fiap.billing_service.infrastructure.adapter.in.messaging.dto.PaymentRequestDto;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adapter for listening to payment requests from SQS queue
 */
@Component
public class PaymentQueueListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentQueueListener.class);

    private final ProcessPaymentUseCase processPaymentUseCase;
    private final ObjectMapper objectMapper;

    public PaymentQueueListener(ProcessPaymentUseCase processPaymentUseCase, ObjectMapper objectMapper) {
        this.processPaymentUseCase = processPaymentUseCase;
        this.objectMapper = objectMapper;
    }

    /**
     * Listens to payment requests from the queue
     * @param message JSON message containing payment request data
     */
    @SqsListener("${aws.sqs.payment-request-queue}")
    public void receivePaymentRequest(String message) {
        log.info("Received payment request message: {}", message);

        try {
            // Parse the message
            PaymentRequestDto paymentRequest = objectMapper.readValue(message, PaymentRequestDto.class);
            
            log.info("Processing payment for order: {}, client: {}, amount: {}", 
                    paymentRequest.getOrderId(), 
                    paymentRequest.getClientId(), 
                    paymentRequest.getAmount());

            // Process the payment
            processPaymentUseCase.processPayment(paymentRequest);
            
            log.info("Payment processed successfully for order: {}", paymentRequest.getOrderId());

        } catch (Exception e) {
            log.error("Error processing payment request message: {}", message, e);
            // In a production environment, you might want to send this to a dead letter queue
            throw new RuntimeException("Failed to process payment request", e);
        }
    }
}
