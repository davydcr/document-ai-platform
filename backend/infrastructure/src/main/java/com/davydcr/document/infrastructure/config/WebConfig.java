package com.davydcr.document.infrastructure.config;

import com.davydcr.document.infrastructure.observability.LoggingInterceptor;
import com.davydcr.document.infrastructure.security.RateLimitingInterceptor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

/**
 * Configuração de Web MVC para registrar interceptors e beans HTTP
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final LoggingInterceptor loggingInterceptor;
    private final RateLimitingInterceptor rateLimitingInterceptor;

    public WebConfig(LoggingInterceptor loggingInterceptor, RateLimitingInterceptor rateLimitingInterceptor) {
        this.loggingInterceptor = loggingInterceptor;
        this.rateLimitingInterceptor = rateLimitingInterceptor;
    }

    /**
     * Configura RestTemplate para chamadas HTTP externas (ex: Ollama)
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Rate Limiting interceptor (verificar limites antes de processar)
        registry.addInterceptor(rateLimitingInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/health/**",
                        "/api/metrics/**",
                        "/api/prometheus/**"
                );
        
        // Logging interceptor (registrar requisições)
        registry.addInterceptor(loggingInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/health/**",
                        "/api/metrics/**",
                        "/api/prometheus/**"
                );
    }
}
