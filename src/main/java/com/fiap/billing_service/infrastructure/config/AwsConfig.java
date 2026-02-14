package com.fiap.billing_service.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class AwsConfig {

  private static final Logger log = LoggerFactory.getLogger(AwsConfig.class);

  @Value("${aws.region}")
  private String region = "us-east-2";

  @Value("${aws.credentials.access-key}")
  private String accessKey;

  @Value("${aws.credentials.secret-key}")
  private String secretKey;

  @Bean
  @Primary
  public SqsClient sqsClient() {
    log.info("Creating SqsClient with region: {}", region);
    return SqsClient.builder()
        .region(Region.US_EAST_2)
        .credentialsProvider(
            StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
        .build();
  }

  @Bean
  @Primary
  public SqsAsyncClient sqsAsyncClient() {
    log.info("Creating SqsAsyncClient with region: {}", region);
    return SqsAsyncClient.builder()
        .region(Region.US_EAST_2)
        .credentialsProvider(
            StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
        .build();
  }
}
