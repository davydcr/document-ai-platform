package com.davydcr.document.infrastructure.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuração de Rate Limiting para a API usando Bucket4j.
 * 
 * Define limites de requisições por diferentes endpoints e estratégias.
 * Suporta diferentes limites para diferentes tipos de endpoints.
 */
@Configuration
public class RateLimitingConfig {

  /**
   * Bucket para login: 5 tentativas a cada 15 minutos
   * Proteção contra brute force
   */
  @Bean
  public Bucket loginBucket() {
    Bandwidth limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(15)));
    return Bucket4j.builder()
        .addLimit(limit)
        .build();
  }

  /**
   * Bucket para endpoints de autenticação geral: 10 requisições por minuto
   * Inclui refresh, logout, validate
   */
  @Bean
  public Bucket authBucket() {
    Bandwidth limit = Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1)));
    return Bucket4j.builder()
        .addLimit(limit)
        .build();
  }

  /**
   * Bucket para endpoints de upload: 10 uploads por hora por usuário
   * Evita abuso de armazenamento
   */
  @Bean
  public Bucket uploadBucket() {
    Bandwidth limit = Bandwidth.classic(10, Refill.intervally(10, Duration.ofHours(1)));
    return Bucket4j.builder()
        .addLimit(limit)
        .build();
  }

  /**
   * Bucket para endpoints de processamento: 20 requisições por hora
   * Processamento é mais custoso
   */
  @Bean
  public Bucket processingBucket() {
    Bandwidth limit = Bandwidth.classic(20, Refill.intervally(20, Duration.ofHours(1)));
    return Bucket4j.builder()
        .addLimit(limit)
        .build();
  }

  /**
   * Bucket para endpoints de leitura (GET): 60 requisições por minuto
   * Menos restritivo para operações de leitura
   */
  @Bean
  public Bucket readBucket() {
    Bandwidth limit = Bandwidth.classic(60, Refill.intervally(60, Duration.ofMinutes(1)));
    return Bucket4j.builder()
        .addLimit(limit)
        .build();
  }

  /**
   * Cache de buckets por usuário para manter estado de rate limiting
   * Um bucket separado para cada usuário
   */
  @Bean
  public Map<String, Bucket> loginAttemptBuckets() {
    return new ConcurrentHashMap<>();
  }

  /**
   * Cache de buckets por usuário para upload
   * Um bucket separado para cada usuário
   */
  @Bean
  public Map<String, Bucket> uploadAttemptBuckets() {
    return new ConcurrentHashMap<>();
  }

  /**
   * Cache de buckets por usuário para processamento
   * Um bucket separado para cada usuário
   */
  @Bean
  public Map<String, Bucket> processingAttemptBuckets() {
    return new ConcurrentHashMap<>();
  }

  /**
   * Limites por endpoint (nome do endpoint -> limites)
   * Configuração centralizada e facilmente customizável
   */
  @Bean
  public RateLimitingProperties rateLimitingProperties() {
    return new RateLimitingProperties();
  }

  /**
   * Classe helper para armazenar propriedades de rate limiting
   */
  public static class RateLimitingProperties {
    // Login: 5 tentativas a cada 15 minutos por IP/email
    public static final int LOGIN_ATTEMPTS = 5;
    public static final int LOGIN_DURATION_MINUTES = 15;

    // Auth geral: 10 requisições por minuto por usuário
    public static final int AUTH_REQUESTS = 10;
    public static final int AUTH_DURATION_MINUTES = 1;

    // Upload: 10 uploads por hora por usuário
    public static final int UPLOAD_REQUESTS = 10;
    public static final int UPLOAD_DURATION_HOURS = 1;

    // Processing: 20 requisições por hora por usuário
    public static final int PROCESSING_REQUESTS = 20;
    public static final int PROCESSING_DURATION_HOURS = 1;

    // Read: 60 requisições por minuto por usuário
    public static final int READ_REQUESTS = 60;
    public static final int READ_DURATION_MINUTES = 1;
  }
}
