package com.fiap.billing_service.infrastructure.config.interceptor;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

@ExtendWith(MockitoExtension.class)
@DisplayName("CorrelationIdInterceptor Tests")
class CorrelationIdInterceptorTest {

  @Mock private HttpServletRequest request;

  @Mock private HttpServletResponse response;

  private CorrelationIdInterceptor interceptor;

  @BeforeEach
  void setUp() {
    interceptor = new CorrelationIdInterceptor();
    MDC.clear();
  }

  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  @Test
  @DisplayName("Should generate new correlation ID when header is not present")
  void testPreHandle_NoCorrelationIdHeader_GeneratesNew() {
    // Arrange
    when(request.getHeader("X-Correlation-Id")).thenReturn(null);

    // Act
    boolean result = interceptor.preHandle(request, response, null);

    // Assert
    assertThat(result).isTrue();
    assertThat(MDC.get("correlationId")).isNotNull();
    assertThat(MDC.get("correlationId")).isNotEmpty();
    verify(response).setHeader("X-Correlation-Id", MDC.get("correlationId"));
  }

  @Test
  @DisplayName("Should use provided correlation ID from request header")
  void testPreHandle_WithCorrelationIdHeader_UsesProvided() {
    // Arrange
    String providedId = "external-corr-id-12345";
    when(request.getHeader("X-Correlation-Id")).thenReturn(providedId);

    // Act
    boolean result = interceptor.preHandle(request, response, null);

    // Assert
    assertThat(result).isTrue();
    assertThat(MDC.get("correlationId")).isEqualTo(providedId);
    verify(response).setHeader("X-Correlation-Id", providedId);
  }

  @Test
  @DisplayName("Should generate new ID when header is empty string")
  void testPreHandle_EmptyCorrelationIdHeader_GeneratesNew() {
    // Arrange
    when(request.getHeader("X-Correlation-Id")).thenReturn("");

    // Act
    boolean result = interceptor.preHandle(request, response, null);

    // Assert
    assertThat(result).isTrue();
    assertThat(MDC.get("correlationId")).isNotNull();
    assertThat(MDC.get("correlationId")).isNotEmpty();
    verify(response).setHeader(eq("X-Correlation-Id"), any(String.class));
  }

  @Test
  @DisplayName("Should add correlation ID to response header")
  void testPreHandle_SetsResponseHeader() {
    // Arrange
    String providedId = "response-corr-id";
    when(request.getHeader("X-Correlation-Id")).thenReturn(providedId);

    // Act
    interceptor.preHandle(request, response, null);

    // Assert
    ArgumentCaptor<String> headerValueCaptor = ArgumentCaptor.forClass(String.class);
    verify(response).setHeader(eq("X-Correlation-Id"), headerValueCaptor.capture());
    assertThat(headerValueCaptor.getValue()).isEqualTo(providedId);
  }

  @Test
  @DisplayName("Should return true to continue request processing")
  void testPreHandle_ReturnsTrue() {
    // Arrange
    when(request.getHeader("X-Correlation-Id")).thenReturn("test-id");

    // Act
    boolean result = interceptor.preHandle(request, response, null);

    // Assert
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("Should clean up MDC after request completion")
  void testAfterCompletion_ClearsMDC() {
    // Arrange
    MDC.put("correlationId", "test-correlation-id");
    assertThat(MDC.get("correlationId")).isNotNull();

    // Act
    interceptor.afterCompletion(request, response, null, null);

    // Assert
    assertThat(MDC.get("correlationId")).isNull();
  }

  @Test
  @DisplayName("Should clean up MDC even when exception occurs")
  void testAfterCompletion_ClearsMDCWithException() {
    // Arrange
    MDC.put("correlationId", "test-correlation-id");
    Exception testException = new RuntimeException("Request failed");

    // Act
    interceptor.afterCompletion(request, response, null, testException);

    // Assert
    assertThat(MDC.get("correlationId")).isNull();
  }

  @Test
  @DisplayName("Should handle multiple requests with different correlation IDs")
  void testMultipleRequests_EachHasUniqueId() {
    // Arrange & Act
    when(request.getHeader("X-Correlation-Id")).thenReturn(null);

    // First request
    interceptor.preHandle(request, response, null);
    String firstCorrelationId = MDC.get("correlationId");
    interceptor.afterCompletion(request, response, null, null);

    // Second request
    when(request.getHeader("X-Correlation-Id")).thenReturn(null);
    interceptor.preHandle(request, response, null);
    String secondCorrelationId = MDC.get("correlationId");

    // Assert
    assertThat(firstCorrelationId).isNotEqualTo(secondCorrelationId);
  }

  @Test
  @DisplayName("Should preserve whitespace-trimmed correlation IDs")
  void testPreHandle_PreservesProvidedIdWithWhitespace() {
    // Arrange
    String providedId = "  spaces-corr-id  ";
    when(request.getHeader("X-Correlation-Id")).thenReturn(providedId);

    // Act
    interceptor.preHandle(request, response, null);

    // Assert
    assertThat(MDC.get("correlationId")).isEqualTo(providedId);
    verify(response).setHeader("X-Correlation-Id", providedId);
  }
}
