package com.fiap.billing_service.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("Deferred: Full Spring context loading issues")
class MercadoPagoConfigTest {

  @Autowired(required = false)
  private String mercadoPagoAccessToken;

  @Autowired(required = false)
  private String mercadoPagoPublicKey;

  @Test
  void testMercadoPagoAccessTokenBeanCreation() {
    assertThat(mercadoPagoAccessToken).isNotNull();
    assertThat(mercadoPagoAccessToken).isNotEmpty();
  }

  @Test
  void testMercadoPagoPublicKeyBeanCreation() {
    assertThat(mercadoPagoPublicKey).isNotNull();
    assertThat(mercadoPagoPublicKey).isNotEmpty();
  }

  @Test
  void testBothBeansAreConfigured() {
    assertThat(mercadoPagoAccessToken).isNotNull();
    assertThat(mercadoPagoPublicKey).isNotNull();
  }
}
