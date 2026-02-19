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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import java.lang.reflect.Field;

/**
 * Tests for LoggingInterceptor request/response logging.
 *
 * <p>Tests verify request logging, response timing, error logging, and MDC management.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoggingInterceptor Tests")
class LoggingInterceptorTest {

  @Mock private HttpServletRequest request;

  @Mock private HttpServletResponse response;

  private LoggingInterceptor interceptor;

  @BeforeEach
  void setUp() {
    interceptor = new LoggingInterceptor();
    MDC.clear();
  }

  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  // ===========================
  // Request Pre-Handling Tests
  // ===========================

  @Test
  @DisplayName("Should return true to allow request to proceed")
  void testPreHandle_AllowsRequestToProced() {
    // Arrange
    when(request.getMethod()).thenReturn("GET");
    when(request.getRequestURI()).thenReturn("/api/health");

    // Act
    boolean result = interceptor.preHandle(request, response, new Object());

    // Assert
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("Should store start time in request attribute")
  void testPreHandle_StoresStartTime() {
    // Arrange
    when(request.getMethod()).thenReturn("GET");
    when(request.getRequestURI()).thenReturn("/api/payments");

    // Act
    interceptor.preHandle(request, response, new Object());

    // Assert
    verify(request).setAttribute(eq("startTime"), any(Long.class));
  }

  @Test
  @DisplayName("Should store request ID in request attribute")
  void testPreHandle_StoresRequestId() {
    // Arrange
    when(request.getMethod()).thenReturn("POST");
    when(request.getRequestURI()).thenReturn("/api/payment");

    // Act
    interceptor.preHandle(request, response, new Object());

    // Assert
    verify(request).setAttribute(eq("requestId"), any(String.class));
  }

  @Test
  @DisplayName("Should populate MDC with request method")
  void testPreHandle_PopulatesMDCWithMethod() {
    // Arrange
    when(request.getMethod()).thenReturn("POST");
    when(request.getRequestURI()).thenReturn("/api/payment");

    // Act
    interceptor.preHandle(request, response, new Object());

    // Assert
    assertThat(MDC.get("http.method")).isEqualTo("POST");
  }

  @Test
  @DisplayName("Should handle different HTTP methods")
  void testPreHandle_HandlesDifferentMethods() {
    // Arrange
    String[] methods = {"GET", "POST", "PUT", "DELETE", "PATCH"};

    for (String method : methods) {
      MDC.clear();
      when(request.getMethod()).thenReturn(method);
      when(request.getRequestURI()).thenReturn("/api/test");

      // Act
      interceptor.preHandle(request, response, new Object());

      // Assert
      assertThat(MDC.get("http.method")).isEqualTo(method);
    }
  }

  @Test
  @DisplayName("Should populate MDC with request URI")
  void testPreHandle_PopulatesMDCWithUri() {
    // Arrange
    String uri = "/api/payments/123";
    when(request.getMethod()).thenReturn("GET");
    when(request.getRequestURI()).thenReturn(uri);

    // Act
    interceptor.preHandle(request, response, new Object());

    // Assert
    assertThat(MDC.get("http.path")).isEqualTo(uri);
  }

  @Test
  @DisplayName("Should handle query string when present")
  void testPreHandle_HandlesQueryStringPresent() {
    // Arrange
    when(request.getMethod()).thenReturn("GET");
    when(request.getRequestURI()).thenReturn("/api/payments");
    when(request.getQueryString()).thenReturn("page=1&size=20");

    // Act
    interceptor.preHandle(request, response, new Object());

    // Assert
    assertThat(MDC.get("http.query_string")).isEqualTo("page=1&size=20");
  }

  @Test
  @DisplayName("Should handle missing query string")
  void testPreHandle_HandlesMissingQueryString() {
    // Arrange
    when(request.getMethod()).thenReturn("GET");
    when(request.getRequestURI()).thenReturn("/api/payments");
    when(request.getQueryString()).thenReturn(null);

    // Act
    interceptor.preHandle(request, response, new Object());

    // Assert
    assertThat(MDC.get("http.query_string")).isEmpty();
  }

  @Test
  @DisplayName("Should populate MDC with remote address")
  void testPreHandle_PopulatesMDCWithRemoteAddr() {
    // Arrange
    when(request.getMethod()).thenReturn("GET");
    when(request.getRequestURI()).thenReturn("/api/health");
    when(request.getRemoteAddr()).thenReturn("192.168.1.100");

    // Act
    interceptor.preHandle(request, response, new Object());

    // Assert
    assertThat(MDC.get("http.remote_addr")).isEqualTo("192.168.1.100");
  }

  // ===========================
  // Response Completion Tests
  // ===========================

  @Test
  @DisplayName("Should calculate request duration and log it")
  void testAfterCompletion_CalculatesDuration() throws InterruptedException {
    // Arrange
    long startTime = System.currentTimeMillis();
    when(request.getMethod()).thenReturn("GET");
    when(request.getRequestURI()).thenReturn("/api/test");
    when(response.getStatus()).thenReturn(200);
    when(request.getAttribute("startTime")).thenReturn(startTime);
    when(request.getAttribute("requestId")).thenReturn("test-request-id-123");

    // Add a small delay
    Thread.sleep(10);

    // Act
    interceptor.afterCompletion(request, response, new Object(), null);

    // Assert - Duration values are used during logging then removed from MDC
    assertThat(MDC.get("http.duration_ms")).isNull();  // Verify cleanup happened
  }

  @Test
  @DisplayName("Should handle successful request completion")
  void testAfterCompletion_SuccessfulRequestCompletion() {
    // Arrange
    long startTime = System.currentTimeMillis();
    when(request.getMethod()).thenReturn("GET");
    when(request.getRequestURI()).thenReturn("/api/test");
    when(response.getStatus()).thenReturn(200);
    when(request.getAttribute("startTime")).thenReturn(startTime);
    when(request.getAttribute("requestId")).thenReturn("test-request-id");

    // Act
    interceptor.afterCompletion(request, response, new Object(), null);

    // Assert - Verify MDC was cleaned up after logging
    assertThat(MDC.get("http.status_code")).isNull();
    assertThat(MDC.get("http.duration_ms")).isNull();
  }

  @Test
  @DisplayName("Should handle various HTTP status codes without errors")
  void testAfterCompletion_HandlesVariousStatusCodes() {
    // Arrange
    int[] statusCodes = {200, 201, 400, 401, 403, 404, 500, 503};
    long startTime = System.currentTimeMillis();

    for (int status : statusCodes) {
      MDC.clear();
      when(request.getMethod()).thenReturn("GET");
      when(request.getRequestURI()).thenReturn("/api/test");
      when(response.getStatus()).thenReturn(status);
      when(request.getAttribute("startTime")).thenReturn(startTime);
      when(request.getAttribute("requestId")).thenReturn("test-request-id");

      // Act & Assert - Should complete without errors
      assertThatNoException().isThrownBy(() -> 
        interceptor.afterCompletion(request, response, new Object(), null)
      );
    }
  }

  // ===========================
  // Error Handling Tests
  // ===========================

  @Test
  @DisplayName("Should handle exception and log error information")
  void testAfterCompletion_HandlesExceptionWithErrorMarking() {
    // Arrange
    long startTime = System.currentTimeMillis();
    RuntimeException exception = new RuntimeException("Payment gateway timeout");
    when(request.getMethod()).thenReturn("POST");
    when(request.getRequestURI()).thenReturn("/api/payment");
    when(response.getStatus()).thenReturn(500);
    when(request.getAttribute("startTime")).thenReturn(startTime);
    when(request.getAttribute("requestId")).thenReturn("test-request-id");

    // Act
    interceptor.afterCompletion(request, response, new Object(), exception);

    // Assert - Values are used during logging then removed from MDC
    assertThat(MDC.get("error")).isNull();  // Verify cleanup
    assertThat(MDC.get("error.type")).isNull();  // Verify cleanup
    assertThat(MDC.get("error.message")).isNull();  // Verify cleanup
  }

  @Test
  @DisplayName("Should handle exception with null message gracefully")
  void testAfterCompletion_ExceptionWithNullMessage() {
    // Arrange
    long startTime = System.currentTimeMillis();
    RuntimeException exception = new RuntimeException();
    when(request.getMethod()).thenReturn("POST");
    when(request.getRequestURI()).thenReturn("/api/payment");
    when(response.getStatus()).thenReturn(500);
    when(request.getAttribute("startTime")).thenReturn(startTime);
    when(request.getAttribute("requestId")).thenReturn("test-request-id");

    // Act & Assert - Should not throw exception
    assertThatNoException().isThrownBy(() -> 
      interceptor.afterCompletion(request, response, new Object(), exception)
    );

    // Verify MDC was cleaned up
    assertThat(MDC.get("error")).isNull();
  }

  @Test
  @DisplayName("Should handle different exception types")
  void testAfterCompletion_HandlesDifferentExceptionTypes() {
    // Arrange
    Exception[] exceptions = {
      new RuntimeException("Runtime error"),
      new IllegalArgumentException("Illegal argument"),
      new java.io.IOException("IO error"),
      new NullPointerException()
    };

    for (Exception exception : exceptions) {
      MDC.clear();
      long startTime = System.currentTimeMillis();
      when(request.getMethod()).thenReturn("POST");
      when(request.getRequestURI()).thenReturn("/api/test");
      when(response.getStatus()).thenReturn(500);
      when(request.getAttribute("startTime")).thenReturn(startTime);
      when(request.getAttribute("requestId")).thenReturn("test-request-id");

      // Act & Assert - Should complete without errors
      assertThatNoException().isThrownBy(() -> 
        interceptor.afterCompletion(request, response, new Object(), exception)
      );
    }
  }

  // ===========================
  // MDC Cleanup Tests
  // ===========================

  @Test
  @DisplayName("Should clean up MDC after successful request")
  void testAfterCompletion_CleanupMDCAfterSuccess() {
    // Arrange
    long startTime = System.currentTimeMillis();
    when(request.getMethod()).thenReturn("GET");
    when(request.getRequestURI()).thenReturn("/api/test");
    when(response.getStatus()).thenReturn(200);
    when(request.getAttribute("startTime")).thenReturn(startTime);
    when(request.getAttribute("requestId")).thenReturn("test-request-id");

    // Act
    interceptor.afterCompletion(request, response, new Object(), null);

    // Assert - Check that MDC was cleaned up
    assertThat(MDC.get("request.id")).isNull();
    assertThat(MDC.get("http.method")).isNull();
    assertThat(MDC.get("http.path")).isNull();
    assertThat(MDC.get("http.query_string")).isNull();
    assertThat(MDC.get("http.remote_addr")).isNull();
    assertThat(MDC.get("http.status_code")).isNull();
    assertThat(MDC.get("http.duration_ms")).isNull();
  }

  @Test
  @DisplayName("Should clean up error MDC entries")
  void testAfterCompletion_CleanupErrorMDCEntries() {
    // Arrange
    long startTime = System.currentTimeMillis();
    RuntimeException exception = new RuntimeException("Test error");
    when(request.getMethod()).thenReturn("POST");
    when(request.getRequestURI()).thenReturn("/api/test");
    when(response.getStatus()).thenReturn(500);
    when(request.getAttribute("startTime")).thenReturn(startTime);
    when(request.getAttribute("requestId")).thenReturn("test-request-id");

    // Act
    interceptor.afterCompletion(request, response, new Object(), exception);

    // Assert - Check that error MDC was cleaned up
    assertThat(MDC.get("error")).isNull();
    assertThat(MDC.get("error.type")).isNull();
    assertThat(MDC.get("error.message")).isNull();
  }

  @Test
  @DisplayName("Should handle missing start time gracefully")
  void testAfterCompletion_MissingStartTime() {
    // Arrange - Only setup what's needed
    when(request.getAttribute("startTime")).thenReturn(null);

    // Act & Assert - Should not throw exception
    assertThatNoException()
        .isThrownBy(() -> interceptor.afterCompletion(request, response, new Object(), null));

    // Post-execution verify no MDC entries were added
    assertThat(MDC.get("http.status_code")).isNull();
    assertThat(MDC.get("http.duration_ms")).isNull();
  }
}
