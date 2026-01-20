package com.davydcr.document.infrastructure.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;

/**
 * Interceptor para logging distribuído
 * Adiciona traceId a todas as requisições para rastreamento
 */
@Component
public class LoggingInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(LoggingInterceptor.class);
    private static final String TRACE_ID_HEADER = "X-Trace-ID";
    private static final String REQUEST_START_TIME = "REQUEST_START_TIME";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            // Gerar ou recuperar traceId
            String traceId = request.getHeader(TRACE_ID_HEADER);
            if (traceId == null || traceId.isEmpty()) {
                traceId = UUID.randomUUID().toString();
            }

            // Adicionar ao MDC (Mapped Diagnostic Context) para ser incluído em todos os logs
            MDC.put("traceId", traceId);
            MDC.put("requestId", UUID.randomUUID().toString());

            // Adicionar header na resposta
            response.addHeader(TRACE_ID_HEADER, traceId);

            // Registrar tempo inicial
            request.setAttribute(REQUEST_START_TIME, System.currentTimeMillis());

            logger.info("→ {} {} - TraceId: {}", request.getMethod(), request.getRequestURI(), traceId);

            return true;
        } catch (Exception e) {
            logger.error("Erro ao processar requisição", e);
            return true;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                               Object handler, Exception ex) {
        try {
            long startTime = (Long) request.getAttribute(REQUEST_START_TIME);
            long duration = System.currentTimeMillis() - startTime;

            String traceId = MDC.get("traceId");
            int statusCode = response.getStatus();

            if (statusCode >= 400) {
                logger.warn("← {} {} - Status: {} - Duration: {}ms - TraceId: {}",
                        request.getMethod(), request.getRequestURI(), statusCode, duration, traceId);
            } else {
                logger.info("← {} {} - Status: {} - Duration: {}ms - TraceId: {}",
                        request.getMethod(), request.getRequestURI(), statusCode, duration, traceId);
            }

            if (ex != null) {
                logger.error("Erro na requisição", ex);
            }
        } finally {
            // Limpar MDC
            MDC.clear();
        }
    }
}
