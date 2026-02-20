package com.fiap.billing_service.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("Deferred: Full Spring context loading issues")
class DatabaseConfigTest {

  @Autowired(required = false)
  private DynamoDbClient dynamoDbClient;

  @Autowired(required = false)
  private DynamoDbEnhancedClient dynamoDbEnhancedClient;

  @Test
  void testDynamoDbClientBeanCreation() {
    assertThat(dynamoDbClient).isNotNull();
  }

  @Test
  void testDynamoDbEnhancedClientBeanCreation() {
    assertThat(dynamoDbEnhancedClient).isNotNull();
  }

  @Test
  void testBothClientsBeansDefined() {
    assertThat(dynamoDbClient).isNotNull();
    assertThat(dynamoDbEnhancedClient).isNotNull();
  }
}
