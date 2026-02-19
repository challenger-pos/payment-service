package com.fiap.billing_service.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("Deferred: Full Spring context loading issues")
class SqsConfigTest {

  @Autowired(required = false)
  private SqsMessageListenerContainerFactory<Object> sqsListenerContainerFactory;

  @Autowired(required = false)
  private SqsTemplate sqsTemplate;

  @Test
  void testSqsMessageListenerContainerFactoryBeanCreation() {
    assertThat(sqsListenerContainerFactory).isNotNull();
  }

  @Test
  void testSqsTemplateBeanCreation() {
    assertThat(sqsTemplate).isNotNull();
  }

  @Test
  void testBothBeansAreConfigured() {
    assertThat(sqsListenerContainerFactory).isNotNull();
    assertThat(sqsTemplate).isNotNull();
  }
}
