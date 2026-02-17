package com.fiap.billing_service.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class AwsConfig {

  private static final Logger log = LoggerFactory.getLogger(AwsConfig.class);

  @Value("${aws.region:us-east-2}")
  private String region;

  @Value("${aws.credentials.access-key:#{null}}")
  private String accessKey;

  @Value("${aws.credentials.secret-key:#{null}}")
  private String secretKey;

  /**
   * Cria o provider de credenciais.
   * Se accessKey e secretKey estiverem definidos, usa credenciais estáticas.
   * Caso contrário, usa DefaultCredentialsProvider (IRSA, environment vars, etc).
   */
  private AwsCredentialsProvider credentialsProvider() {
    if (accessKey != null && !accessKey.isEmpty() && 
        secretKey != null && !secretKey.isEmpty()) {
      log.info("Using static AWS credentials (access key: {}...)", 
          accessKey.substring(0, Math.min(4, accessKey.length())));
      return StaticCredentialsProvider.create(
          AwsBasicCredentials.create(accessKey, secretKey)
      );
    } else {
      log.info("Using DefaultCredentialsProvider (IRSA, IAM Role, or environment variables)");
      return DefaultCredentialsProvider.create();
    }
  }

  @Bean
  @Primary
  public SqsClient sqsClient() {
    log.info("Creating SqsClient with region: {}", region);
    return SqsClient.builder()
        .region(Region.of(region))
        .credentialsProvider(credentialsProvider())
        .build();
  }

  @Bean
  @Primary
  public SqsAsyncClient sqsAsyncClient() {
    log.info("Creating SqsAsyncClient with region: {}", region);
    return SqsAsyncClient.builder()
        .region(Region.of(region))
        .credentialsProvider(credentialsProvider())
        .build();
  }
}
