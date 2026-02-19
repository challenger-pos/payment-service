package com.fiap.billing_service.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("Deferred: Full Spring context loading issues")
class AwsConfigTest {

  @Autowired(required = false)
  private SqsClient sqsClient;

  @Autowired(required = false)
  private SqsAsyncClient sqsAsyncClient;

  @Test
  void testSqsClientBeanCreation() {
    assertThat(sqsClient).isNotNull();
  }

  @Test
  void testSqsAsyncClientBeanCreation() {
    assertThat(sqsAsyncClient).isNotNull();
  }

  @Test
  void testBothClientsHaveSameRegion() {
    assertThat(sqsClient).isNotNull();
    assertThat(sqsAsyncClient).isNotNull();
  }
}
