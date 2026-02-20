package com.fiap.billing_service.infrastructure.config;

import com.timgroup.statsd.NonBlockingStatsDClientBuilder;
import com.timgroup.statsd.StatsDClient;
import datadog.trace.api.GlobalTracer;
import datadog.trace.api.Tracer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatadogConfig {

    @Value("${datadog.statsd.host:localhost}")
    private String statsdHost;

    @Value("${datadog.statsd.port:8125}")
    private int statsdPort;

    @Bean
    public Tracer tracer() {
        return GlobalTracer.get();
    }

    @Bean
    public StatsDClient statsDClient() {
        return new NonBlockingStatsDClientBuilder()
                .prefix("payment")
                .hostname(statsdHost)
                .port(statsdPort)
                .build();
    }
}
