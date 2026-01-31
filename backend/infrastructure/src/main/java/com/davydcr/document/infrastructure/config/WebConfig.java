package com.davydcr.document.infrastructure.config;

import com.davydcr.document.infrastructure.observability.LoggingInterceptor;
import com.davydcr.document.infrastructure.security.RateLimitingInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuração de Web MVC para registrar interceptors e CORS
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final LoggingInterceptor loggingInterceptor;
    private final RateLimitingInterceptor rateLimitingInterceptor;

    public WebConfig(LoggingInterceptor loggingInterceptor, RateLimitingInterceptor rateLimitingInterceptor) {
        this.loggingInterceptor = loggingInterceptor;
        this.rateLimitingInterceptor = rateLimitingInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .maxAge(3600);
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
