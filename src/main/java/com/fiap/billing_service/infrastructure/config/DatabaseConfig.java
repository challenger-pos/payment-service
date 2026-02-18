package com.fiap.billing_service.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import java.net.URI;

/**
 * DynamoDB Configuration for billing-service.
 * 
 * Configures connections to AWS DynamoDB or local DynamoDB emulator for development.
 * The endpoint configuration is environment-dependent:
 * - Production: Uses AWS managed DynamoDB region endpoint
 * - Development: Can point to local DynamoDB container (http://localhost:8000)
 * - Test: Points to test DynamoDB local container
 */
@Configuration
public class DatabaseConfig {

  @Value("${aws.region:us-east-1}")
  private String awsRegion;

  @Value("${aws.dynamodb.endpoint:#{null}}")
  private String dynamoDbEndpoint;

  @Value("${aws.access.key:#{null}}")
  private String awsAccessKey;

  @Value("${aws.secret.key:#{null}}")
  private String awsSecretKey;

  /**
   * Creates a DynamoDB client configured for the current environment.
   * 
   * In production, credentials are obtained from IAM roles attached to the Pod/Lambda.
   * In development/test, endpoint can be overridden to point to local DynamoDB.
   * 
   * @return Configured DynamoDbClient
   */
  @Bean
  public DynamoDbClient dynamoDbClient() {
    DynamoDbClientBuilder builder = DynamoDbClient.builder()
        .region(Region.of(awsRegion));

    // Configure endpoint override for local development/testing
    if (dynamoDbEndpoint != null && !dynamoDbEndpoint.isBlank()) {
      builder.endpointOverride(URI.create(dynamoDbEndpoint));
    }

    // Configure credentials for local development (in production, use IAM roles)
    if (awsAccessKey != null && awsSecretKey != null && 
        !awsAccessKey.isBlank() && !awsSecretKey.isBlank()) {
      AwsBasicCredentials credentials = AwsBasicCredentials.create(awsAccessKey, awsSecretKey);
      builder.credentialsProvider(StaticCredentialsProvider.create(credentials));
    }

    return builder.build();
  }

  /**
   * Creates the Enhanced DynamoDB client which simplifies working with DynamoDB.
   * The Enhanced client wraps DynamoDbClient with high-level abstractions.
   * 
   * Features:
   * - Automatic serialization/deserialization of Java objects
   * - Support for @DynamoDbBean annotated classes
   * - Simplified API for common operations
   * 
   * @param dynamoDbClient The configured DynamoDB client
   * @return Configured DynamoDbEnhancedClient
   */
  @Bean
  public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
    return DynamoDbEnhancedClient.builder()
        .dynamoDbClient(dynamoDbClient)
        .build();
  }
}
