package com.davydcr.document.application.dto;

/**
 * Requisição de autenticação
 */
public record LoginRequest(
        String username,
        String password
) {}
