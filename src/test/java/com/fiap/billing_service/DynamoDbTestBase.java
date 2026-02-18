package com.fiap.billing_service;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.DynamoDBLocalContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base test class for integration tests using DynamoDB Local.
 *
 * This class sets up a DynamoDB Local container using TestContainers for testing.
 * Subclasses should extend this class to have automatic DynamoDB setup for their tests.
 *
 * Features:
 * - Automatic DynamoDB Local container startup/shutdown
 * - Dynamic property configuration for Spring Test context
 * - AWS SDK v2 compatible DynamoDB behavior
 *
 * Usage:
 * @SpringBootTest
 * class MyIntegrationTest extends DynamoDbTestBase {
 *     // Test methods...
 * }
 */
@Testcontainers
@SpringBootTest
public abstract class DynamoDbTestBase {

  // DynamoDB Local container - automatically started and stopped by testcontainers
  @Container
  static final DynamoDBLocalContainer dynamodb = new DynamoDBLocalContainer(
      "amazon/dynamodb-local:latest"
  );

  /**
   * Configure dynamic properties for the test context.
   * This method is called by Spring to override configuration properties
   * during test execution, pointing to the local DynamoDB instead of AWS.
   *
   * @param registry The registry for dynamic properties
   */
  @DynamicPropertySource
  static void configureDynamoDbProperties(DynamicPropertyRegistry registry) {
    registry.add("aws.dynamodb.endpoint", () -> dynamodb.getEndpointOverride(
        software.amazon.awssdk.core.client.config.HttpProtocol.HTTP
    ).toString());
    registry.add("aws.region", () -> "us-east-1");
    registry.add("aws.access.key", () -> "test-key");
    registry.add("aws.secret.key", () -> "test-secret");
    registry.add("dynamodb.table-name", () -> "payments-test");
  }
}
