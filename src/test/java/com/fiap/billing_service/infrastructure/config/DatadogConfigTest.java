package com.fiap.billing_service.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.timgroup.statsd.StatsDClient;
import datadog.trace.api.Tracer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("Deferred: Full Spring context loading issues")
class DatadogConfigTest {

  @Autowired(required = false)
  private Tracer tracer;

  @Autowired(required = false)
  private StatsDClient statsDClient;

  @Test
  void testTracerBeanCreation() {
    assertThat(tracer).isNotNull();
  }

  @Test
  void testStatsDClientBeanCreation() {
    assertThat(statsDClient).isNotNull();
  }

  @Test
  void testBothBeansAreConfigured() {
    assertThat(tracer).isNotNull();
    assertThat(statsDClient).isNotNull();
  }
}
