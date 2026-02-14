package com.fiap.billing_service.application.service;

import com.fiap.billing_service.application.port.in.ProcessPaymentUseCase;
import com.fiap.billing_service.application.port.out.PaymentGatewayPort;
import com.fiap.billing_service.application.port.out.PaymentRepositoryPort;
import com.fiap.billing_service.application.port.out.PaymentResponseMessagePort;
import com.fiap.billing_service.domain.entity.Payment;
import com.fiap.billing_service.domain.exception.PaymentProcessingException;
import com.fiap.billing_service.infrastructure.adapter.in.messaging.dto.PaymentRequestDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class ProcessPaymentService implements ProcessPaymentUseCase {

    private final PaymentRepositoryPort paymentRepository;
    private final PaymentGatewayPort paymentGateway;
    private final PaymentResponseMessagePort paymentResponseMessage;

    public ProcessPaymentService(PaymentRepositoryPort paymentRepository,
                                PaymentGatewayPort paymentGateway,
                                PaymentResponseMessagePort paymentResponseMessage) {
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
        this.paymentResponseMessage = paymentResponseMessage;
    }

    @Override
    public Payment processPayment(PaymentRequestDto paymentRequest) {
        // Create payment entity
        var payment = new Payment(
            UUID.randomUUID(), 
            paymentRequest.getOrderId(), 
            paymentRequest.getClientId(),
            paymentRequest.getAmount()
        );
        
        payment = paymentRepository.save(payment);
        
        try {
            // Process payment through Mercado Pago (PIX)
            var processedPayment = paymentGateway.processPixPayment(
                paymentRequest.getAmount(),
                paymentRequest.getCustomerDocument(),
                paymentRequest.getCustomerEmail(),
                paymentRequest.getCustomerName(),
                paymentRequest.getDescription() != null ? 
                    paymentRequest.getDescription() : 
                    "Payment for order " + paymentRequest.getOrderId()
            );
            
            // Update payment with gateway response
            payment.markAsProcessing(
                processedPayment.getExternalPaymentId(),
                processedPayment.getPaymentMethod(),
                processedPayment.getQrCode(),
                processedPayment.getQrCodeBase64()
            );
            
            if (processedPayment.getStatus() == com.fiap.billing_service.domain.valueobject.PaymentStatus.APPROVED) {
                payment.markAsApproved();
            } else if (processedPayment.getStatus() == com.fiap.billing_service.domain.valueobject.PaymentStatus.REJECTED) {
                payment.markAsRejected(processedPayment.getErrorMessage());
            }
            
            // Save updated payment
            payment = paymentRepository.save(payment);
            
            // Send payment response to message queue
            paymentResponseMessage.sendPaymentResponse(payment);
            
            return payment;
            
        } catch (Exception e) {
            payment.markAsFailed(e.getMessage());
            paymentRepository.save(payment);
            throw new PaymentProcessingException("Failed to process payment for order " + paymentRequest.getOrderId(), e);
        }
    }
}
