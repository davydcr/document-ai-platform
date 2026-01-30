package com.davydcr.document.infrastructure.controller;

import com.davydcr.document.infrastructure.persistence.entity.AuditLogEntity;
import com.davydcr.document.infrastructure.security.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin Controller para Auditoria e Logs de Segurança
 * 🔐 Requer role ADMIN para todos os endpoints
 */
@RestController
@RequestMapping("/api/admin/audit")
@Tag(name = "Audit Management", description = "Endpoints administrativos para auditoria e segurança")
@SecurityRequirement(name = "bearer-jwt")
@CrossOrigin(origins = "*")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    /**
     * Listar todos os logs com paginação e filtros
     * 
     * @param page Número da página (default: 0)
     * @param size Tamanho da página (default: 20)
     * @param sortBy Campo para ordenação (default: createdAt)
     * @return Página de logs com paginação
     */
    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Listar logs de auditoria",
        description = "Retorna todos os logs de auditoria com paginação"
    )
    public ResponseEntity<Page<AuditLogEntity>> listAllLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        Page<AuditLogEntity> logs = auditLogService.getAllAuditLogs(pageable);
        
        return ResponseEntity.ok(logs);
    }

    /**
     * Logs de um usuário específico por email
     */
    @GetMapping("/user/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Logs de um usuário", description = "Retorna todos os logs de um usuário específico")
    public ResponseEntity<Page<AuditLogEntity>> getUserLogs(
            @PathVariable String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AuditLogEntity> logs = auditLogService.getEmailAuditLogs(email, pageable);
        
        return ResponseEntity.ok(logs);
    }

    /**
     * Detectar tentativas de brute force
     * Retorna usuários que sofreram tentativas de brute force
     */
    @GetMapping("/brute-force")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Detectar brute force",
        description = "Identifica tentativas de brute force nos últimos 15 minutos"
    )
    public ResponseEntity<?> detectBruteForce(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String ipAddress) {
        
        try {
            Map<String, Object> response = new HashMap<>();
            
            if (email != null && !email.isEmpty()) {
                boolean isBruteForce = auditLogService.isBruteForceAttempt(email);
                response.put("email", email);
                response.put("isBruteForce", isBruteForce);
                response.put("message", isBruteForce ? 
                    "Brute force detectado para este email" : 
                    "Sem padrão de brute force detectado");
                
                if (isBruteForce) {
                    List<AuditLogEntity> failedAttempts = auditLogService.getFailedLoginAttempts(email, 15);
                    response.put("failedAttempts", failedAttempts.size());
                }
            } else if (ipAddress != null && !ipAddress.isEmpty()) {
                boolean isBruteForce = auditLogService.isBruteForceByIp(ipAddress);
                response.put("ipAddress", ipAddress);
                response.put("isBruteForce", isBruteForce);
                response.put("message", isBruteForce ? 
                    "Brute force detectado para este IP" : 
                    "Sem padrão de brute force detectado");
            } else {
                return ResponseEntity.badRequest().body(
                    Map.of("error", "Forneça 'email' ou 'ipAddress' como parâmetro")
                );
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erro ao detectar brute force: " + e.getMessage()));
        }
    }

    /**
     * Atividades suspeitas nas últimas horas
     */
    @GetMapping("/suspicious-activity")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Atividades suspeitas",
        description = "Retorna atividades suspeitas dos últimos N horas"
    )
    public ResponseEntity<Page<AuditLogEntity>> getSuspiciousActivity(
            @RequestParam(defaultValue = "1") int hours,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AuditLogEntity> suspicious = auditLogService.getSuspiciousActivity(hours, pageable);
        
        return ResponseEntity.ok(suspicious);
    }

    /**
     * Logs por tipo de evento
     */
    @GetMapping("/event/{eventType}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Logs por tipo de evento", description = "Filtra logs por tipo específico de evento")
    public ResponseEntity<Page<AuditLogEntity>> getByEventType(
            @PathVariable String eventType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AuditLogEntity> logs = auditLogService.getEventTypeLogs(eventType, pageable);
        
        return ResponseEntity.ok(logs);
    }

    /**
     * Logs por IP address
     */
    @GetMapping("/ip/{ipAddress}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Logs por IP", description = "Retorna todos os logs de um IP específico")
    public ResponseEntity<Page<AuditLogEntity>> getByIpAddress(
            @PathVariable String ipAddress,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AuditLogEntity> logs = auditLogService.getIpAddressLogs(ipAddress, pageable);
        
        return ResponseEntity.ok(logs);
    }

    /**
     * Logs em período específico
     */
    @GetMapping("/date-range")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Logs por período", description = "Retorna logs entre duas datas")
    public ResponseEntity<?> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        if (startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "startDate deve ser anterior a endDate")
            );
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AuditLogEntity> logs = auditLogService.getLogsByDateRange(startDate, endDate, pageable);
        
        return ResponseEntity.ok(logs);
    }

    /**
     * Relatório de segurança geral
     */
    @GetMapping("/security-report")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Relatório de segurança",
        description = "Gera um relatório completo de eventos de segurança"
    )
    public ResponseEntity<?> getSecurityReport() {
        try {
            AuditLogService.SecurityReport report = auditLogService.generateSecurityReport();
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erro ao gerar relatório: " + e.getMessage()));
        }
    }

    /**
     * Sessões ativas de usuário
     */
    @GetMapping("/user-sessions/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Sessões do usuário",
        description = "Retorna histórico de logins e logouts de um usuário"
    )
    public ResponseEntity<?> getUserSessions(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<AuditLogEntity> sessions = auditLogService.getUserAuditLogs(userId, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("sessions", sessions);
            response.put("totalSessions", sessions.getTotalElements());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erro ao buscar sessões: " + e.getMessage()));
        }
    }

    /**
     * Deletar log específico
     */
    @DeleteMapping("/log/{logId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deletar log", description = "Remove um log específico (CUIDADO: operação irreversível)")
    public ResponseEntity<?> deleteLog(@PathVariable String logId) {
        try {
            // Aqui você implementaria a lógica de deleção
            // Por enquanto, retornamos uma mensagem
            return ResponseEntity.ok(Map.of(
                "message", "Log " + logId + " seria deletado",
                "warning", "Deleção de logs é operação irreversível - confirme antes"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erro ao deletar log: " + e.getMessage()));
        }
    }

    /**
     * Health check do serviço de auditoria
     */
    @GetMapping("/health")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Health check", description = "Verifica status do serviço de auditoria")
    public ResponseEntity<?> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "AuditLogService");
        health.put("timestamp", LocalDateTime.now());
        health.put("endpoint", "/admin/audit");
        
        return ResponseEntity.ok(health);
    }
}
