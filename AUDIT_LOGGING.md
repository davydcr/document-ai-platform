# 🔐 Auditoria & Logs de Autenticação

## Visão Geral

Sistema completo de auditoria para rastrear todos os eventos de autenticação, acesso e segurança na plataforma Document AI. Fornece:

- ✅ **Rastreamento de eventos** - Login, logout, refresh de token, falhas
- ✅ **Detecção de brute force** - Alertas automáticos de tentativas suspeitas
- ✅ **Análise de segurança** - Atividades anômalas por IP e usuário
- ✅ **Conformidade** - Auditoria completa para regulamentos (LGPD, GDPR)
- ✅ **Investigação** - Relatórios e filtros avançados

---

## 📊 Arquitetura

### Banco de Dados

```sql
-- Tabela audit_logs (V9)
CREATE TABLE audit_logs (
  id VARCHAR(36) PRIMARY KEY,                    -- UUID único
  user_id VARCHAR(36),                           -- FK para user_accounts
  email VARCHAR(255) NOT NULL,                   -- Email do usuário
  event_type VARCHAR(50) NOT NULL,               -- Tipo de evento (enum)
  endpoint VARCHAR(255) NOT NULL,                -- URI do endpoint
  method VARCHAR(10) NOT NULL,                   -- GET, POST, PUT, DELETE
  status_code INT,                               -- HTTP 200, 401, 403, 429, etc
  ip_address VARCHAR(45) NOT NULL,               -- IPv4 ou IPv6
  user_agent TEXT,                               -- Browser/Client info
  created_at TIMESTAMP NOT NULL,                 -- Timestamp do evento
  updated_at TIMESTAMP,                          -- Atualização
  error_message TEXT,                            -- Motivo de falha
  details TEXT,                                  -- JSON adicional
  
  FOREIGN KEY (user_id) REFERENCES user_accounts(id),
  
  -- Índices para performance
  INDEX idx_audit_logs_user_id (user_id),
  INDEX idx_audit_logs_event_type (event_type),
  INDEX idx_audit_logs_created_at (created_at DESC),
  INDEX idx_audit_logs_ip_address (ip_address),
  INDEX idx_audit_logs_user_event_date (user_id, event_type, created_at DESC),
  INDEX idx_audit_logs_ip_event_type (ip_address, event_type, created_at DESC)
);
```

### Componentes

#### 1. **AuditLogEntity** - JPA Mapping
Entidade que representa um log de auditoria no banco de dados.

```java
@Entity
@Table(name = "audit_logs")
public class AuditLogEntity {
  String id;              // UUID
  String userId;          // FK para user_accounts
  String email;           // Email do usuário
  String eventType;       // LOGIN_SUCCESS, LOGIN_FAILURE, etc
  String endpoint;        // /api/auth/login
  String method;          // POST
  Integer statusCode;     // 200, 401, 403, 429
  String ipAddress;       // 192.168.1.1
  String userAgent;       // Mozilla/5.0...
  LocalDateTime createdAt;
  String errorMessage;    // "Credenciais inválidas"
  String details;         // JSON personalizado
}
```

#### 2. **AuditEventType** - Enumeração
Tipos de eventos que podem ser auditados.

```java
public enum AuditEventType {
  // LOGIN
  LOGIN_SUCCESS,
  LOGIN_FAILURE,
  LOGIN_RATE_LIMIT_EXCEEDED,
  LOGIN_ACCOUNT_LOCKED,
  
  // LOGOUT
  LOGOUT_SUCCESS,
  LOGOUT_FORCED,
  
  // REFRESH TOKEN
  TOKEN_REFRESH_SUCCESS,
  TOKEN_REFRESH_FAILURE,
  TOKEN_REVOKED,
  
  // ACESSO
  ACCESS_GRANTED,
  ACCESS_UNAUTHORIZED,
  ACCESS_FORBIDDEN,
  ACCESS_NOT_FOUND,
  
  // CREDENCIAIS
  PASSWORD_CHANGED,
  PASSWORD_RESET_REQUESTED,
  EMAIL_CHANGED,
  TWO_FACTOR_ENABLED,
  
  // ADMIN
  USER_CREATED,
  USER_DELETED,
  ROLE_ASSIGNED,
  USER_LOCKED,
  
  // SISTEMA
  SYSTEM_ERROR,
  SUSPICIOUS_ACTIVITY,
  GENERIC
}
```

#### 3. **AuditLogRepository** - Data Access
Interface para acesso aos logs de auditoria com queries customizadas.

```java
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, String> {
  // Buscas
  Page<AuditLogEntity> findByUserIdOrderByCreatedAtDesc(String userId, Pageable);
  Page<AuditLogEntity> findByEmailOrderByCreatedAtDesc(String email, Pageable);
  Page<AuditLogEntity> findByEventTypeOrderByCreatedAtDesc(String eventType, Pageable);
  Page<AuditLogEntity> findByIpAddressOrderByCreatedAtDesc(String ipAddress, Pageable);
  Page<AuditLogEntity> findByDateRange(LocalDateTime start, LocalDateTime end, Pageable);
  
  // Detecção de brute force
  int countFailedLoginAttemptsInMinutes(String email, int minutes);
  int countFailedLoginAttemptsByIpInMinutes(String ipAddress, int minutes);
  List<AuditLogEntity> findFailedLoginAttempts(String email, LocalDateTime since);
  
  // Análise de segurança
  Page<AuditLogEntity> findSuspiciousEvents(LocalDateTime since, Pageable);
  Page<AuditLogEntity> findUserSessions(String userId, Pageable);
  List<AuditLogEntity> findAnomalousActivity();
  
  // Limpeza
  void deleteOldLogs(LocalDateTime cutoffDate);
}
```

#### 4. **AuditLogService** - Business Logic
Serviço que orquestra o logging de eventos de auditoria.

```java
@Service
public class AuditLogService {
  // Log de eventos
  AuditLogEntity logLoginSuccess(String userId, String email, String ip, String ua, String endpoint);
  AuditLogEntity logLoginFailure(String email, String ip, String ua, String endpoint, String reason);
  AuditLogEntity logLogout(String userId, String email, String ip, String ua, String endpoint);
  AuditLogEntity logTokenRefresh(String userId, String email, String ip, String ua, String endpoint);
  AuditLogEntity logResourceAccess(String userId, String email, String ip, String ua, String endpoint, String method);
  
  // Detecção de brute force
  boolean isBruteForceAttempt(String email);        // Threshold: 5 tentativas em 15 min
  boolean isBruteForceByIp(String ipAddress);       // Threshold: 5 tentativas em 15 min
  List<AuditLogEntity> getFailedLoginAttempts(String email, int minutesAgo);
  
  // Buscas
  Page<AuditLogEntity> getUserAuditLogs(String userId, Pageable);
  Page<AuditLogEntity> getEmailAuditLogs(String email, Pageable);
  Page<AuditLogEntity> getEventTypeLogs(String eventType, Pageable);
  Page<AuditLogEntity> getIpAddressLogs(String ipAddress, Pageable);
  Page<AuditLogEntity> getLogsByDateRange(LocalDateTime start, LocalDateTime end, Pageable);
  Page<AuditLogEntity> getSuspiciousActivity(int hoursAgo, Pageable);
  Page<AuditLogEntity> getUserSessions(String userId, Pageable);
  List<AuditLogEntity> getAnomalousActivity();
  
  // Relatórios
  SecurityReport generateSecurityReport();
  
  // Limpeza agendada (diariamente às 2:00 AM)
  @Scheduled(cron = "0 0 2 * * *")
  void cleanupOldLogs();  // Remove logs > 90 dias
}
```

#### 5. **AuthenticationEventListener** - Event Listener
Escuta eventos Spring Security e registra automaticamente.

```java
@Component
public class AuthenticationEventListener implements ApplicationListener<ApplicationEvent> {
  // Captura AuthenticationSuccessEvent
  // Captura AbstractAuthenticationFailureEvent
  // Registra no AuditLogService automaticamente
}
```

---

## 🔐 Integração com Autenticação

### AuthController - Login

```java
@PostMapping("/login")
public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request, 
                                          HttpServletRequest httpRequest) {
  try {
    // Verificar brute force
    if (auditLogService.isBruteForceAttempt(request.getEmail())) {
      auditLogService.logLoginFailure(request.getEmail(), getIp(httpRequest), 
                                      getUA(httpRequest), "/api/auth/login", 
                                      "Bloqueado por brute force");
      return ResponseEntity.status(429).build();
    }
    
    // Autenticar
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
    );
    
    // Log de sucesso
    UserDetails user = userDetailsService.loadUserByUsername(request.getEmail());
    auditLogService.logLoginSuccess(user.getId(), request.getEmail(), 
                                   getIp(httpRequest), getUA(httpRequest), 
                                   "/api/auth/login");
    
    // Gerar tokens
    String accessToken = jwtProvider.generateAccessToken(user);
    String refreshToken = jwtProvider.generateRefreshToken(user);
    
    return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken));
    
  } catch (BadCredentialsException e) {
    // Log de falha
    auditLogService.logLoginFailure(request.getEmail(), getIp(httpRequest), 
                                    getUA(httpRequest), "/api/auth/login", 
                                    e.getMessage());
    return ResponseEntity.status(401).build();
  }
}
```

### AuthController - Refresh Token

```java
@PostMapping("/refresh")
public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshTokenRequest request,
                                            HttpServletRequest httpRequest) {
  try {
    RefreshTokenEntity refreshToken = refreshTokenService.validateAndGetToken(request.getRefreshToken());
    String accessToken = jwtProvider.generateAccessToken(refreshToken.getUser());
    
    // Log de sucesso
    auditLogService.logTokenRefresh(refreshToken.getUser().getId(), 
                                   refreshToken.getUser().getEmail(),
                                   getIp(httpRequest), getUA(httpRequest), 
                                   "/api/auth/refresh");
    
    return ResponseEntity.ok(new AuthResponse(accessToken, request.getRefreshToken()));
    
  } catch (InvalidRefreshTokenException e) {
    // Log de falha
    auditLogService.logTokenRefreshFailure(request.getUserEmail(), 
                                           getIp(httpRequest), getUA(httpRequest), 
                                           "/api/auth/refresh");
    return ResponseEntity.status(401).build();
  }
}
```

### AuthController - Logout

```java
@PostMapping("/logout")
public ResponseEntity<Void> logout(@AuthenticationPrincipal String userId,
                                   HttpServletRequest httpRequest) {
  UserDetails user = userDetailsService.loadUserById(userId);
  
  // Revogar refresh tokens
  refreshTokenService.revokeAllTokens(userId);
  
  // Log de logout
  auditLogService.logLogout(userId, user.getEmail(), 
                           getIp(httpRequest), getUA(httpRequest), 
                           "/api/auth/logout");
  
  return ResponseEntity.ok().build();
}
```

---

## 📈 Relatórios e Análises

### Security Report

```java
// Gerar relatório das últimas 24 horas
AuditLogService.SecurityReport report = auditLogService.generateSecurityReport();

report.suspiciousEventCount;    // Número de eventos suspeitos
report.anomalousIpCount;        // IPs com comportamento anômalo
report.suspiciousEvents;        // Lista de eventos suspeitos
report.anomalousActivities;     // Lista de atividades anômalas
```

### Exemplos de Consulta

```java
// Logs de um usuário
Page<AuditLogEntity> userLogs = auditLogService.getUserAuditLogs(userId, pageRequest);

// Eventos de login
Page<AuditLogEntity> logins = auditLogService.getEventTypeLogs(
    AuditEventType.LOGIN_SUCCESS.getCode(), pageRequest);

// Tentativas falhadas recentes
List<AuditLogEntity> failures = auditLogService.getFailedLoginAttempts(email, 15);

// Atividades de um IP
Page<AuditLogEntity> ipActivity = auditLogService.getIpAddressLogs(ipAddress, pageRequest);

// Sessões de um usuário
Page<AuditLogEntity> sessions = auditLogService.getUserSessions(userId, pageRequest);

// Atividades suspeitas nas últimas 24 horas
Page<AuditLogEntity> suspicious = auditLogService.getSuspiciousActivity(24, pageRequest);
```

---

## 🚨 Detecção de Brute Force

### Thresholds

- **Email**: 5 tentativas de login falhadas em 15 minutos → Bloqueado
- **IP**: 5 tentativas de login falhadas em 15 minutos → Bloqueado
- **Resposta**: HTTP 429 (Too Many Requests)

### Fluxo de Detecção

```
1. Usuário tenta fazer login
   ↓
2. Verificar: isBruteForceAttempt(email)?
   ↓
3. SIM → Retornar 429, logar tentativa bloqueada
   ↓
4. NÃO → Continuar com autenticação
   ↓
5. Se falhar → Logar loginFailure, verificar novamente
   ↓
6. Após 5 falhas em 15 min → Próxima tentativa é bloqueada
```

### Exemplo de Integração

```java
// Em AuthController
if (auditLogService.isBruteForceAttempt(request.getEmail())) {
  auditLogService.logLoginFailure(request.getEmail(), ipAddress, userAgent, 
                                 "/api/auth/login", "Bloqueado por brute force");
  throw new RateLimitExceededException("Muitas tentativas de login");
}
```

---

## 🧹 Limpeza Automática

### Schedule

```
Cron: 0 0 2 * * *  (Diariamente às 2:00 AM)
```

### Configuração

```java
@Service
public class AuditLogService {
  private static final int LOG_RETENTION_DAYS = 90;
  
  @Scheduled(cron = "0 0 2 * * *")
  @Transactional
  public void cleanupOldLogs() {
    LocalDateTime cutoffDate = LocalDateTime.now().minusDays(LOG_RETENTION_DAYS);
    auditLogRepository.deleteOldLogs(cutoffDate);
    logger.info("Audit log cleanup completed - removed logs older than {}", cutoffDate);
  }
}
```

### Retention Policy

- **Logs normais**: 90 dias
- **Logs de segurança**: 90 dias
- **Relatórios**: Arquivados em backup

---

## 🔍 Endpoints de Admin (Futuro)

```
GET  /api/admin/audit-logs                    # Listar todos os logs
GET  /api/admin/audit-logs/user/{userId}      # Logs de um usuário
GET  /api/admin/audit-logs/email/{email}      # Logs de um email
GET  /api/admin/audit-logs/ip/{ipAddress}     # Logs de um IP
GET  /api/admin/audit-logs/events/{eventType} # Logs de um tipo de evento
GET  /api/admin/audit-logs/date-range         # Logs em intervalo de datas
GET  /api/admin/audit-logs/suspicious         # Atividades suspeitas
GET  /api/admin/security-report               # Relatório de segurança
GET  /api/admin/audit-logs/anomalous          # Atividades anômalas
POST /api/admin/audit-logs/export             # Exportar logs (CSV/JSON)
```

---

## 📋 Tipos de Eventos

### Autenticação
- `LOGIN_SUCCESS` - Login bem-sucedido
- `LOGIN_FAILURE` - Credenciais inválidas
- `LOGIN_RATE_LIMIT_EXCEEDED` - Muitas tentativas
- `LOGIN_ACCOUNT_LOCKED` - Conta bloqueada

### Sessão
- `LOGOUT_SUCCESS` - Logout bem-sucedido
- `LOGOUT_FORCED` - Sessão expirada/revogada
- `SESSION_EXPIRED` - Sessão expirou

### Token
- `TOKEN_REFRESH_SUCCESS` - Refresh bem-sucedido
- `TOKEN_REFRESH_FAILURE` - Refresh falhou
- `TOKEN_REVOKED` - Token revogado

### Acesso
- `ACCESS_GRANTED` - Acesso autorizado
- `ACCESS_UNAUTHORIZED` - Não autenticado
- `ACCESS_FORBIDDEN` - Sem permissão

### Credenciais
- `PASSWORD_CHANGED` - Senha alterada
- `PASSWORD_RESET_REQUESTED` - Reset solicitado
- `EMAIL_CHANGED` - Email alterado
- `TWO_FACTOR_ENABLED` - 2FA ativado

### Admin
- `USER_CREATED` - Usuário criado
- `USER_DELETED` - Usuário deletado
- `ROLE_ASSIGNED` - Role atribuída
- `USER_LOCKED` - Usuário bloqueado

### Sistema
- `SYSTEM_ERROR` - Erro de sistema
- `SUSPICIOUS_ACTIVITY` - Atividade suspeita

---

## 🧪 Testes

### AuditLogServiceTest

```bash
# Rodar testes de auditoria
mvn test -Dtest=AuditLogServiceTest

# Testes incluídos:
✓ testLogLoginSuccess
✓ testLogLoginFailure
✓ testLogLogout
✓ testLogTokenRefresh
✓ testLogTokenRefreshFailure
✓ testLogResourceAccess
✓ testDetectBruteForceByEmail
✓ testDetectBruteForceByIp
✓ testNoBruteForceWithLessThan5Attempts
✓ testGetFailedLoginAttempts
✓ testGetUserAuditLogs
✓ testGetEventTypeLogs
✓ testGetIpAddressLogs
✓ testGetLogsByDateRange
✓ testGetSuspiciousActivity
✓ testGetUserSessions
✓ testGenerateSecurityReport
✓ testCountByEventType
```

---

## 💾 Migração do Banco de Dados

A migração `V9__audit_logs.sql` cria a tabela automaticamente:

```bash
# Aplicar migração
mvn flyway:migrate -Dspring.datasource.url=jdbc:postgresql://localhost:5432/document_ai

# Limpar migração (desenvolvimento)
mvn flyway:clean -Dspring.datasource.url=jdbc:postgresql://localhost:5432/document_ai

# Info
mvn flyway:info -Dspring.datasource.url=jdbc:postgresql://localhost:5432/document_ai
```

---

## 📊 Queries Úteis

### SQL nativo para análises

```sql
-- Top 10 usuários com mais logins
SELECT email, COUNT(*) as login_count
FROM audit_logs
WHERE event_type = 'LOGIN_SUCCESS'
AND created_at > NOW() - INTERVAL 30 DAY
GROUP BY email
ORDER BY login_count DESC
LIMIT 10;

-- IPs suspeitos (5+ falhas em 15 min)
SELECT ip_address, COUNT(*) as failure_count, MAX(created_at) as last_attempt
FROM audit_logs
WHERE event_type = 'LOGIN_FAILURE'
AND created_at > NOW() - INTERVAL 1 DAY
GROUP BY ip_address
HAVING COUNT(*) >= 5
ORDER BY failure_count DESC;

-- Timeline de atividade por usuário
SELECT created_at, event_type, endpoint, status_code
FROM audit_logs
WHERE user_id = 'UUID_AQUI'
ORDER BY created_at DESC
LIMIT 100;

-- Eventos suspeitos nas últimas 24h
SELECT id, email, event_type, ip_address, created_at
FROM audit_logs
WHERE event_type IN ('LOGIN_FAILURE', 'UNAUTHORIZED_ACCESS', 'SUSPICIOUS_ACTIVITY')
AND created_at > NOW() - INTERVAL 1 DAY
ORDER BY created_at DESC;
```

---

## 🔒 Conformidade

### LGPD (Lei Geral de Proteção de Dados)

- ✅ Logs de acesso a dados pessoais
- ✅ Rastreamento de quem acessou o quê e quando
- ✅ Direito de acesso aos dados do usuário
- ✅ Deleção de logs após 90 dias
- ✅ Consentimento implícito via Terms of Service

### GDPR (General Data Protection Regulation)

- ✅ Auditoria de acesso a dados pessoais
- ✅ Proteção contra acesso não autorizado
- ✅ Resposta a incidentes de segurança
- ✅ Right to be forgotten (deleção de logs após 90 dias)
- ✅ Data retention policy

---

## 📈 Performance

### Índices

```
idx_audit_logs_user_id          - Busca por usuário
idx_audit_logs_event_type       - Busca por tipo de evento
idx_audit_logs_created_at       - Busca por data (ordenado DESC)
idx_audit_logs_ip_address       - Busca por IP
idx_audit_logs_user_event_date  - Composição para sessões
idx_audit_logs_ip_event_type    - Composição para detecção de brute force
```

### Tamanho Esperado

```
Usuários ativos: 10.000
Logins por usuário/dia: 5
Requisições por usuário/dia: 100
Retenção: 90 dias

Logs diários: ~1M registros
Logs totais: ~90M registros
Tamanho: ~50GB (bruto), ~20GB (comprimido)
```

---

## 🚀 Próximos Passos

- [ ] Dashboard de segurança em tempo real
- [ ] Alertas por email de atividades suspeitas
- [ ] 2FA (Two-Factor Authentication)
- [ ] Integração com SIEM (Splunk, ELK)
- [ ] Exportação de logs (CSV, JSON)
- [ ] Análise de comportamento (ML-based anomaly detection)
- [ ] Endpoints de admin para gerenciamento de logs

---

## 📞 Suporte

Para questões sobre auditoria:
- Documentação: [AUDIT_LOGGING.md](AUDIT_LOGGING.md)
- Código: `infrastructure/src/main/java/com/davydcr/document/infrastructure/security/`
- Testes: `infrastructure/src/test/java/com/davydcr/document/infrastructure/security/AuditLogServiceTest.java`
