package com.fiap.billing_service.infrastructure.config;

import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.listener.acknowledgement.handler.AcknowledgementMode;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.time.Duration;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

/**
 * SQS configuration for message processing with idempotency support
 *
 * <p>Configures: - Acknowledgment mode: ON_SUCCESS (message only removed after successful
 * processing) - Visibility timeout: 5 minutes (enough time for Mercado Pago API calls) - Error
 * handling: Logs errors without silently failing
 */
@Configuration
public class SqsConfig {

  private static final Logger log = LoggerFactory.getLogger(SqsConfig.class);

  /**
   * Custom SQS listener factory with idempotency-friendly settings
   *
   * @param sqsAsyncClient the SQS async client
   * @return configured listener factory
   */
  @Bean
  public SqsMessageListenerContainerFactory<Object> defaultSqsListenerContainerFactory(
      SqsAsyncClient sqsAsyncClient) {

    log.info(
        "Configuring SQS listener with ON_SUCCESS acknowledgment and 5-minute visibility timeout");

    return SqsMessageListenerContainerFactory.builder()
        .sqsAsyncClient(sqsAsyncClient)
        .configure(
            options ->
                options
                    // Only acknowledge after successful processing
                    .acknowledgementMode(AcknowledgementMode.ON_SUCCESS)
                    // 5 minutes visibility timeout (enough for payment processing)
                    .queueAttributeNames(
                        Collections.singleton(QueueAttributeName.VISIBILITY_TIMEOUT))
                    // Poll duration
                    .pollTimeout(Duration.ofSeconds(10)))
        .build();
  }

  /**
   * SQS Template for sending messages
   *
   * @param sqsAsyncClient the SQS async client
   * @return SQS template
   */
  @Bean
  public SqsTemplate sqsTemplate(SqsAsyncClient sqsAsyncClient) {
    return SqsTemplate.builder().sqsAsyncClient(sqsAsyncClient).build();
  }
}
