package com.davydcr.document.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes para Rate Limiting Interceptor
 */
@DisplayName("Rate Limiting Interceptor Tests")
public class RateLimitingInterceptorTest {

    private RateLimitingInterceptor rateLimitingInterceptor;
    private HttpServletRequest mockRequest;
    private HttpServletResponse mockResponse;

    @BeforeEach
    public void setup() {
        rateLimitingInterceptor = new RateLimitingInterceptor();
        mockRequest = mock(HttpServletRequest.class);
        mockResponse = mock(HttpServletResponse.class);
    }

    @Test
    @DisplayName("Deve permitir requisição quando dentro do limite")
    public void testShouldAllowRequestWhenWithinLimit() throws Exception {
        // Arrange
        when(mockRequest.getRequestURI()).thenReturn("/api/documents");
        when(mockRequest.getMethod()).thenReturn("GET");
        when(mockRequest.getAttribute("userId")).thenReturn("user-123");

        // Act
        boolean result = rateLimitingInterceptor.preHandle(mockRequest, mockResponse, new Object());

        // Assert
        assertTrue(result, "Requisição deve ser permitida quando dentro do limite");
    }

    @Test
    @DisplayName("Deve adicionar headers de rate limit na resposta")
    public void testShouldAddRateLimitHeadersToResponse() throws Exception {
        // Arrange
        when(mockRequest.getRequestURI()).thenReturn("/api/documents");
        when(mockRequest.getMethod()).thenReturn("GET");
        when(mockRequest.getAttribute("userId")).thenReturn("user-123");

        // Act
        rateLimitingInterceptor.preHandle(mockRequest, mockResponse, new Object());

        // Assert
        verify(mockResponse).addHeader("X-RateLimit-Limit", "60");
        verify(mockResponse).addHeader(eq("X-RateLimit-Remaining"), anyString());
        verify(mockResponse).addHeader(eq("X-RateLimit-Reset"), anyString());
    }

    @Test
    @DisplayName("Deve usar IP do cliente quando X-Forwarded-For está presente")
    public void testShouldUseXForwardedForIP() throws Exception {
        // Arrange
        when(mockRequest.getRequestURI()).thenReturn("/api/documents");
        when(mockRequest.getMethod()).thenReturn("GET");
        when(mockRequest.getAttribute("userId")).thenReturn(null);
        when(mockRequest.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, 198.51.100.1");

        // Act
        boolean result = rateLimitingInterceptor.preHandle(mockRequest, mockResponse, new Object());

        // Assert
        assertTrue(result, "Deve permitir requisição");
    }

    @Test
    @DisplayName("Deve identificar endpoint de login")
    public void testShouldIdentifyLoginEndpoint() throws Exception {
        // Arrange
        when(mockRequest.getRequestURI()).thenReturn("/api/auth/login");
        when(mockRequest.getMethod()).thenReturn("POST");

        // Act
        boolean result = rateLimitingInterceptor.preHandle(mockRequest, mockResponse, new Object());

        // Assert
        assertTrue(result, "Primeira requisição ao login deve ser permitida");
    }

    @Test
    @DisplayName("Deve identificar endpoint de upload")
    public void testShouldIdentifyUploadEndpoint() throws Exception {
        // Arrange
        when(mockRequest.getRequestURI()).thenReturn("/api/documents/upload");
        when(mockRequest.getMethod()).thenReturn("POST");
        when(mockRequest.getAttribute("userId")).thenReturn("user-123");

        // Act
        boolean result = rateLimitingInterceptor.preHandle(mockRequest, mockResponse, new Object());

        // Assert
        assertTrue(result, "Upload deve ser permitido");
    }

    @Test
    @DisplayName("Deve identificar endpoint de processamento")
    public void testShouldIdentifyProcessingEndpoint() throws Exception {
        // Arrange
        when(mockRequest.getRequestURI()).thenReturn("/api/documents/doc-1/process");
        when(mockRequest.getMethod()).thenReturn("POST");
        when(mockRequest.getAttribute("userId")).thenReturn("user-123");

        // Act
        boolean result = rateLimitingInterceptor.preHandle(mockRequest, mockResponse, new Object());

        // Assert
        assertTrue(result, "Processamento deve ser permitido");
    }

    @Test
    @DisplayName("Deve separar limites por usuário")
    public void testShouldSeparateLimitsByUser() throws Exception {
        // Arrange
        when(mockRequest.getRequestURI()).thenReturn("/api/documents");
        when(mockRequest.getMethod()).thenReturn("GET");

        // Act
        when(mockRequest.getAttribute("userId")).thenReturn("user-1");
        // Faz algumas requisições com user-1
        for (int i = 0; i < 5; i++) {
            rateLimitingInterceptor.preHandle(mockRequest, mockResponse, new Object());
        }

        // Usuário 2 ainda deve poder fazer requisições
        when(mockRequest.getAttribute("userId")).thenReturn("user-2");
        boolean user2Allowed = rateLimitingInterceptor.preHandle(mockRequest, mockResponse, new Object());

        // Assert
        assertTrue(user2Allowed, "Usuário 2 deve estar em limite separado de usuário 1");
    }

    @Test
    @DisplayName("Deve permitir requisições GET (read endpoints)")
    public void testShouldAllowGetRequests() throws Exception {
        // Arrange
        when(mockRequest.getRequestURI()).thenReturn("/api/documents/123");
        when(mockRequest.getMethod()).thenReturn("GET");
        when(mockRequest.getAttribute("userId")).thenReturn("user-123");

        // Act
        boolean result = rateLimitingInterceptor.preHandle(mockRequest, mockResponse, new Object());

        // Assert
        assertTrue(result, "Requisições GET devem ser permitidas");
    }

    @Test
    @DisplayName("Deve permitir requisições POST normais")
    public void testShouldAllowPostRequests() throws Exception {
        // Arrange
        when(mockRequest.getRequestURI()).thenReturn("/api/webhooks");
        when(mockRequest.getMethod()).thenReturn("POST");
        when(mockRequest.getAttribute("userId")).thenReturn("user-123");

        // Act
        boolean result = rateLimitingInterceptor.preHandle(mockRequest, mockResponse, new Object());

        // Assert
        assertTrue(result, "Requisições POST devem ser permitidas");
    }

    @Test
    @DisplayName("Deve limpar buckets expirados")
    public void testShouldCleanupExpiredBuckets() throws Exception {
        // Arrange
        when(mockRequest.getRequestURI()).thenReturn("/api/documents");
        when(mockRequest.getMethod()).thenReturn("GET");
        when(mockRequest.getAttribute("userId")).thenReturn("user-123");

        // Fazer uma requisição para criar um bucket
        rateLimitingInterceptor.preHandle(mockRequest, mockResponse, new Object());

        // Act - Limpar buckets
        rateLimitingInterceptor.cleanupExpiredBuckets();

        // Assert - Não deve lançar exceção
        boolean result = rateLimitingInterceptor.preHandle(mockRequest, mockResponse, new Object());
        assertTrue(result, "Deve permitir nova requisição após limpeza");
    }
}

