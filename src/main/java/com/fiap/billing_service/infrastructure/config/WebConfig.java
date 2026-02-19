package com.fiap.billing_service.infrastructure.config;

import com.fiap.billing_service.infrastructure.config.interceptor.CorrelationIdInterceptor;
import com.fiap.billing_service.infrastructure.config.interceptor.LoggingInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final CorrelationIdInterceptor correlationIdInterceptor;
    private final LoggingInterceptor loggingInterceptor;

    public WebConfig(CorrelationIdInterceptor correlationIdInterceptor, LoggingInterceptor loggingInterceptor) {
        this.correlationIdInterceptor = correlationIdInterceptor;
        this.loggingInterceptor = loggingInterceptor;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(correlationIdInterceptor)
                .addPathPatterns("/api/**");
        registry.addInterceptor(loggingInterceptor)
                .addPathPatterns("/api/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("*")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .exposedHeaders("X-Correlation-Id")
            .maxAge(3600);
    }
}
