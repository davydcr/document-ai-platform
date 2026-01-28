package com.davydcr.document.infrastructure.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Interceptor para Rate Limiting de requisições HTTP.
 * 
 * Implementa diferentes estratégias de rate limiting:
 * - Login: 5 tentativas a cada 15 minutos (por email/IP)
 * - Upload: 10 uploads por hora (por usuário)
 * - Processing: 20 requisições por hora (por usuário)
 * - Read: 60 requisições por minuto (por usuário)
 * 
 * Retorna 429 (Too Many Requests) quando limite é excedido.
 */
@Component
public class RateLimitingInterceptor implements HandlerInterceptor {

  private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
  private final Map<String, Bucket> uploadBuckets = new ConcurrentHashMap<>();
  private final Map<String, Bucket> processingBuckets = new ConcurrentHashMap<>();
  private final Map<String, Bucket> readBuckets = new ConcurrentHashMap<>();

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    String requestUri = request.getRequestURI();
    String method = request.getMethod();

    // Login rate limiting: 5 tentativas a cada 15 minutos
    if (isLoginEndpoint(requestUri)) {
      return handleLoginRateLimit(request, response);
    }

    // Obter usuário do contexto
    String userId = getUserIdFromRequest(request);
    if (userId == null) {
      userId = getClientIp(request); // Fallback para IP se não autenticado
    }

    // Upload rate limiting: 10 uploads por hora
    if (isUploadEndpoint(requestUri)) {
      return handleUploadRateLimit(userId, request, response);
    }

    // Processing rate limiting: 20 requisições por hora
    if (isProcessingEndpoint(requestUri)) {
      return handleProcessingRateLimit(userId, request, response);
    }

    // Read endpoints: 60 requisições por minuto (menos restritivo)
    if ("GET".equals(method)) {
      return handleReadRateLimit(userId, request, response);
    }

    // Por padrão, permitir (não há limite específico)
    return true;
  }

  /**
   * Verifica e aplica rate limiting para login
   */
  private boolean handleLoginRateLimit(HttpServletRequest request, HttpServletResponse response) throws Exception {
    String identifier = getLoginIdentifier(request);
    
    Bucket bucket = loginBuckets.computeIfAbsent(identifier, key -> createLoginBucket());

    if (bucket.tryConsume(1)) {
      // Token consumido com sucesso
      response.addHeader("X-RateLimit-Limit", "5");
      response.addHeader("X-RateLimit-Remaining", String.valueOf(bucket.getAvailableTokens()));
      response.addHeader("X-RateLimit-Reset", String.valueOf(getResetTime()));
      return true;
    } else {
      // Limite excedido
      response.setStatus(429);
      response.setContentType("application/json");
      response.getWriter().write("{\"error\": \"Muitas tentativas de login. Tente novamente em 15 minutos.\"}");
      return false;
    }
  }

  /**
   * Verifica e aplica rate limiting para upload
   */
  private boolean handleUploadRateLimit(String userId, HttpServletRequest request, HttpServletResponse response) throws Exception {
    Bucket bucket = uploadBuckets.computeIfAbsent(userId, key -> createUploadBucket());

    if (bucket.tryConsume(1)) {
      response.addHeader("X-RateLimit-Limit", "10");
      response.addHeader("X-RateLimit-Remaining", String.valueOf(bucket.getAvailableTokens()));
      response.addHeader("X-RateLimit-Reset", String.valueOf(getResetTime()));
      return true;
    } else {
      response.setStatus(429);
      response.setContentType("application/json");
      response.getWriter().write("{\"error\": \"Limite de uploads excedido. Máximo: 10 por hora.\"}");
      return false;
    }
  }

  /**
   * Verifica e aplica rate limiting para processing
   */
  private boolean handleProcessingRateLimit(String userId, HttpServletRequest request, HttpServletResponse response) throws Exception {
    Bucket bucket = processingBuckets.computeIfAbsent(userId, key -> createProcessingBucket());

    if (bucket.tryConsume(1)) {
      response.addHeader("X-RateLimit-Limit", "20");
      response.addHeader("X-RateLimit-Remaining", String.valueOf(bucket.getAvailableTokens()));
      response.addHeader("X-RateLimit-Reset", String.valueOf(getResetTime()));
      return true;
    } else {
      response.setStatus(429);
      response.setContentType("application/json");
      response.getWriter().write("{\"error\": \"Limite de processamento excedido. Máximo: 20 por hora.\"}");
      return false;
    }
  }

  /**
   * Verifica e aplica rate limiting para leitura
   */
  private boolean handleReadRateLimit(String userId, HttpServletRequest request, HttpServletResponse response) throws Exception {
    Bucket bucket = readBuckets.computeIfAbsent(userId, key -> createReadBucket());

    if (bucket.tryConsume(1)) {
      response.addHeader("X-RateLimit-Limit", "60");
      response.addHeader("X-RateLimit-Remaining", String.valueOf(bucket.getAvailableTokens()));
      response.addHeader("X-RateLimit-Reset", String.valueOf(getResetTime()));
      return true;
    } else {
      response.setStatus(429);
      response.setContentType("application/json");
      response.getWriter().write("{\"error\": \"Limite de requisições excedido. Máximo: 60 por minuto.\"}");
      return false;
    }
  }

  /**
   * Identifica o endpoint de login (extrai email do corpo se possível)
   */
  private String getLoginIdentifier(HttpServletRequest request) {
    // Usar IP como identificador principal para login (proteção contra brute force por IP)
    String ip = getClientIp(request);
    return "login:" + ip;
  }

  /**
   * Cria bucket para login
   */
  private Bucket createLoginBucket() {
    Bandwidth limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(15)));
    return Bucket4j.builder().addLimit(limit).build();
  }

  /**
   * Cria bucket para upload
   */
  private Bucket createUploadBucket() {
    Bandwidth limit = Bandwidth.classic(10, Refill.intervally(10, Duration.ofHours(1)));
    return Bucket4j.builder().addLimit(limit).build();
  }

  /**
   * Cria bucket para processing
   */
  private Bucket createProcessingBucket() {
    Bandwidth limit = Bandwidth.classic(20, Refill.intervally(20, Duration.ofHours(1)));
    return Bucket4j.builder().addLimit(limit).build();
  }

  /**
   * Cria bucket para read
   */
  private Bucket createReadBucket() {
    Bandwidth limit = Bandwidth.classic(60, Refill.intervally(60, Duration.ofMinutes(1)));
    return Bucket4j.builder().addLimit(limit).build();
  }

  /**
   * Verifica se é endpoint de login
   */
  private boolean isLoginEndpoint(String uri) {
    return uri.contains("/api/auth/login");
  }

  /**
   * Verifica se é endpoint de upload
   */
  private boolean isUploadEndpoint(String uri) {
    return uri.contains("/api/documents/upload");
  }

  /**
   * Verifica se é endpoint de processing
   */
  private boolean isProcessingEndpoint(String uri) {
    return uri.contains("/process") || 
           uri.contains("/classify") || 
           uri.contains("/extract");
  }

  /**
   * Obtém userId do contexto de segurança
   */
  private String getUserIdFromRequest(HttpServletRequest request) {
    Object userId = request.getAttribute("userId");
    if (userId != null) {
      return userId.toString();
    }
    return null;
  }

  /**
   * Obtém IP do cliente (considerando proxies)
   */
  private String getClientIp(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
      return xForwardedFor.split(",")[0].trim();
    }
    String xRealIp = request.getHeader("X-Real-IP");
    if (xRealIp != null && !xRealIp.isEmpty()) {
      return xRealIp;
    }
    return request.getRemoteAddr();
  }

  /**
   * Calcula o tempo de reset (próximo intervalo)
   * Retorna em segundos para o cliente
   */
  private long getResetTime() {
    // Aproximadamente 1 minuto para a maioria dos endpoints
    return System.currentTimeMillis() / 1000 + 60;
  }

  /**
   * Limpa buckets expirados periodicamente (pode ser chamado via @Scheduled)
   */
  public void cleanupExpiredBuckets() {
    // Em produção, seria melhor usar Caffeine Cache com expiration
    // Por enquanto, limpeza simples
    loginBuckets.clear();
    uploadBuckets.clear();
    processingBuckets.clear();
    readBuckets.clear();
  }
}
