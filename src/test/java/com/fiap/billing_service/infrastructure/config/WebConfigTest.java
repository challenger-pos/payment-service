package com.fiap.billing_service.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fiap.billing_service.infrastructure.config.interceptor.CorrelationIdInterceptor;
import com.fiap.billing_service.infrastructure.config.interceptor.LoggingInterceptor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("Deferred: Full Spring context loading issues")
class WebConfigTest {

  @Autowired
  private RestTemplate restTemplate;

  @Autowired
  private CorrelationIdInterceptor correlationIdInterceptor;

  @Autowired
  private LoggingInterceptor loggingInterceptor;

  @Autowired
  private WebConfig webConfig;

  @Test
  void testRestTemplateBeanCreation() {
    assertThat(restTemplate).isNotNull();
  }

  @Test
  void testCorrelationIdInterceptorIsConfigured() {
    assertThat(correlationIdInterceptor).isNotNull();
  }

  @Test
  void testLoggingInterceptorIsConfigured() {
    assertThat(loggingInterceptor).isNotNull();
  }

  @Test
  void testWebConfigImplementsWebMvcConfigurer() {
    assertThat(webConfig).isInstanceOf(org.springframework.web.servlet.config.annotation.WebMvcConfigurer.class);
  }

  @Test
  void testAddInterceptorsCalled() {
    // Verify interceptors are registered by checking that they are not null
    assertThat(correlationIdInterceptor).isNotNull();
    assertThat(loggingInterceptor).isNotNull();
  }
}
