package com.fiap.billing_service.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("Deferred: Full Spring context loading issues")
class StructuredLoggingConfigTest {

  @Autowired
  private StructuredLoggingConfig structuredLoggingConfig;

  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  @Test
  void testComponentCreated() {
    assertThat(structuredLoggingConfig).isNotNull();
  }

  @Test
  void testAddContextWithKeyValue() {
    structuredLoggingConfig.addContext("test.key", "test.value");
    assertThat(MDC.get("test.key")).isEqualTo("test.value");
  }

  @Test
  void testAddContextWithNullKey() {
    structuredLoggingConfig.addContext(null, "test.value");
    // Should not add anything
    assertThat(MDC.get(null)).isNull();
  }

  @Test
  void testAddContextWithMap() {
    Map<String, String> context = new HashMap<>();
    context.put("key1", "value1");
    context.put("key2", "value2");
    
    structuredLoggingConfig.addContext(context);
    
    assertThat(MDC.get("key1")).isEqualTo("value1");
    assertThat(MDC.get("key2")).isEqualTo("value2");
  }

  @Test
  void testRemoveContext() {
    structuredLoggingConfig.addContext("test.key", "test.value");
    assertThat(MDC.get("test.key")).isEqualTo("test.value");
    
    structuredLoggingConfig.removeContext("test.key");
    assertThat(MDC.get("test.key")).isNull();
  }

  @Test
  void testClearContext() {
    structuredLoggingConfig.addContext("key1", "value1");
    structuredLoggingConfig.addContext("key2", "value2");
    
    structuredLoggingConfig.clearContext();
    
    assertThat(MDC.get("key1")).isNull();
    assertThat(MDC.get("key2")).isNull();
  }

  @Test
  void testAddOperationContext() {
    structuredLoggingConfig.addOperationContext("CREATE", "entity-123");
    
    assertThat(MDC.get("operation.type")).isEqualTo("CREATE");
    assertThat(MDC.get("operation.entity_id")).isEqualTo("entity-123");
  }

  @Test
  void testAddUserContext() {
    structuredLoggingConfig.addUserContext("user-456", "user@example.com");
    
    assertThat(MDC.get("user.id")).isEqualTo("user-456");
    assertThat(MDC.get("user.email")).isEqualTo("user@example.com");
  }

  @Test
  void testAddHttpContext() {
    structuredLoggingConfig.addHttpContext("GET", "/api/test", "200");
    
    assertThat(MDC.get("http.method")).isEqualTo("GET");
    assertThat(MDC.get("http.path")).isEqualTo("/api/test");
    assertThat(MDC.get("http.status_code")).isEqualTo("200");
  }
}
