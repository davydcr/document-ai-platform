package com.davydcr.document.infrastructure.exception;

/**
 * Exceção lançada quando o limite de rate limiting é excedido.
 * Será mapeada para HTTP 429 (Too Many Requests)
 */
public class RateLimitExceededException extends RuntimeException {

  private final int retryAfterSeconds;
  private final String limitType;

  public RateLimitExceededException(String message, int retryAfterSeconds, String limitType) {
    super(message);
    this.retryAfterSeconds = retryAfterSeconds;
    this.limitType = limitType;
  }

  public RateLimitExceededException(String message) {
    this(message, 60, "UNKNOWN");
  }

  public int getRetryAfterSeconds() {
    return retryAfterSeconds;
  }

  public String getLimitType() {
    return limitType;
  }
}
