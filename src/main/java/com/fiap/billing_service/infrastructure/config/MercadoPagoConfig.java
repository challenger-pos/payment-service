package com.fiap.billing_service.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MercadoPagoConfig {

    @Value("${mercadopago.access-token}")
    private String accessToken;

    @Value("${mercadopago.public-key}")
    private String publicKey;

    @Bean
    public String mercadoPagoAccessToken() {
        return accessToken;
    }

    @Bean
    public String mercadoPagoPublicKey() {
        return publicKey;
    }
}
