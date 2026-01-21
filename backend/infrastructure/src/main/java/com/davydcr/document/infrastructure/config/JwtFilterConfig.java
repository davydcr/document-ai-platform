package com.davydcr.document.infrastructure.config;

import com.davydcr.document.infrastructure.security.JwtFilter;
import com.davydcr.document.infrastructure.security.JwtProvider;
import jakarta.servlet.Filter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registra o JWT Filter como bean do Spring
 */
@Configuration
public class JwtFilterConfig {

    @Bean
    public FilterRegistrationBean<Filter> jwtFilterRegistration(JwtProvider jwtProvider) {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new JwtFilter(jwtProvider));
        registration.addUrlPatterns("/*");
        registration.setName("jwtFilter");
        registration.setOrder(1);
        return registration;
    }
}
