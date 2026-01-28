# 🔐 Rate Limiting - Documentação

## 📋 Visão Geral

Sistema de Rate Limiting implementado com **Bucket4j** para proteger a API contra abuso e ataques de brute force.

**Status**: ✅ **IMPLEMENTADO E TESTADO**

---

## 🎯 Objetivos

- ✅ Proteção contra brute force em login
- ✅ Proteção de recursos custosos (upload, processamento)
- ✅ Limitação de requisições de leitura
- ✅ Isolamento por usuário/IP
- ✅ Headers padrão HTTP para cliente

---

## 📊 Limites Implementados

### **1. Login Rate Limiting**
```
5 tentativas a cada 15 minutos
Identificador: IP do cliente
Proteção: Brute force em credenciais
Status 429: Muitas tentativas de login
```

### **2. Upload Rate Limiting**
```
10 uploads por hora
Identificador: Usuário autenticado
Proteção: Abuso de armazenamento
Status 429: Limite de uploads excedido
```

### **3. Processing Rate Limiting**
```
20 requisições por hora
Identificador: Usuário autenticado
Proteção: Recursos custosos (OCR, classification)
Endpoints: /process, /classify, /extract
Status 429: Limite de processamento excedido
```

### **4. Read Rate Limiting**
```
60 requisições por minuto
Identificador: Usuário autenticado
Proteção: Requisições GET (menos restritivo)
Status 429: Limite de requisições excedido
```

---

## 🏗️ Arquitetura

### **RateLimitingInterceptor**
- Intercepta todas as requisições `/api/**`
- Verifica limite antes de processar
- Identifica tipo de endpoint
- Obtém usuário do contexto ou IP
- Retorna 429 se limite excedido

```java
// Fluxo de execução
1. preHandle() intercepta requisição
2. Identifica endpoint (login, upload, processing, read)
3. Obtém usuário ou IP do cliente
4. Busca/cria bucket para usuário
5. Tenta consumir 1 token
   ├─ Sucesso → addHeaders e continua
   └─ Falha → status 429 e bloqueia
```

### **RateLimitingConfig**
- Beans de configuração para cada tipo de limite
- Propriedades centralizadas
- Maps para cache de buckets por usuário

### **RateLimitExceededException**
- Custom exception para 429
- Armazenam retryAfter e limitType
- Mapeada para HTTP 429 no GlobalExceptionHandler

---

## 📁 Arquivos Criados

| Arquivo | Tipo | Descrição |
|---------|------|-----------|
| RateLimitingConfig.java | Config | Configuração de limites e beans |
| RateLimitingInterceptor.java | Security | Interceptor de requisições |
| RateLimitExceededException.java | Exception | Custom exception para 429 |
| RateLimitingInterceptorTest.java | Test | 10 testes unitários |
| pom.xml | Dependency | bucket4j:7.6.0 |
| WebConfig.java | Config | Registra interceptor (modificado) |
| GlobalExceptionHandler.java | Exception | Handler para 429 (modificado) |

---

## 🧪 Testes

### **Testes Implementados**
```
✅ testShouldAllowRequestWhenWithinLimit
✅ testShouldAddRateLimitHeadersToResponse
✅ testShouldUseXForwardedForIP
✅ testShouldIdentifyLoginEndpoint
✅ testShouldIdentifyUploadEndpoint
✅ testShouldIdentifyProcessingEndpoint
✅ testShouldSeparateLimitsByUser
✅ testShouldAllowGetRequests
✅ testShouldAllowPostRequests
✅ testShouldCleanupExpiredBuckets
```

**Total de testes**: 91/91 ✅

---

## 🔬 Validação em Operação

### **Teste: Login Rate Limiting**

```bash
# 5 tentativas bem-sucedidas
✅ Tentativa 1: 200 OK
✅ Tentativa 2: 200 OK
✅ Tentativa 3: 200 OK
✅ Tentativa 4: 200 OK
✅ Tentativa 5: 200 OK

# 6ª tentativa bloqueada
❌ Tentativa 6: 429 Too Many Requests
❌ Tentativa 7: 429 Too Many Requests
```

### **Response do Bloqueio**
```json
{
  "timestamp": "2026-01-27T...",
  "status": 429,
  "error": "Rate Limit Exceeded",
  "message": "Muitas tentativas de login. Tente novamente em 15 minutos.",
  "limitType": "LOGIN",
  "retryAfter": 900
}
```

### **Headers na Resposta**
```
X-RateLimit-Limit: 5
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1707424200
Retry-After: 900
```

---

## 🔌 Integração

### **WebConfig - Ordem de Interceptors**
```java
// 1. RateLimitingInterceptor (primeiro - verifica limites)
registry.addInterceptor(rateLimitingInterceptor)
  .addPathPatterns("/api/**")
  .excludePathPatterns("/api/health/**", "/api/metrics/**");

// 2. LoggingInterceptor (depois - registra requisições)
registry.addInterceptor(loggingInterceptor)
  .addPathPatterns("/api/**")
  .excludePathPatterns("/api/health/**", "/api/metrics/**");
```

### **Exclusões**
```
/api/health/**
/api/metrics/**
/api/prometheus/**
```

---

## 📈 Características

### **Identificação de Usuário**
```
1. Obtém userId do request attribute (setado pelo JwtAuthenticationFilter)
2. Se não houver, usa IP do cliente como fallback
3. Suporta proxies: X-Forwarded-For, X-Real-IP
```

### **Bucket Management**
```
- Buckets em ConcurrentHashMap (thread-safe)
- Um bucket por usuário/IP por tipo de endpoint
- Refill intervally para precisão temporal
- Cleanup manual via cleanupExpiredBuckets()
```

### **Headers HTTP**
```
X-RateLimit-Limit:     Limite máximo
X-RateLimit-Remaining: Tokens disponíveis
X-RateLimit-Reset:     Timestamp de reset
Retry-After:           Segundos até retry (429)
```

---

## 💾 Configuração

### **application.yml**
```yaml
# Não há configuração específica de rate limiting
# Todos os limites são definidos em RateLimitingConfig.java
# Podem ser externalizados para application.yml no futuro
```

### **Limites Centralizados**
```java
public static class RateLimitingProperties {
  public static final int LOGIN_ATTEMPTS = 5;
  public static final int LOGIN_DURATION_MINUTES = 15;
  
  public static final int AUTH_REQUESTS = 10;
  public static final int AUTH_DURATION_MINUTES = 1;
  
  public static final int UPLOAD_REQUESTS = 10;
  public static final int UPLOAD_DURATION_HOURS = 1;
  
  public static final int PROCESSING_REQUESTS = 20;
  public static final int PROCESSING_DURATION_HOURS = 1;
  
  public static final int READ_REQUESTS = 60;
  public static final int READ_DURATION_MINUTES = 1;
}
```

---

## 🔒 Segurança

### **Proteções Implementadas**

1. **Brute Force Protection**
   - Login limitado a 5 tentativas por IP
   - Window: 15 minutos
   - Força maior delay entre tentativas

2. **Resource Protection**
   - Upload: 10 por hora (espaço em disco)
   - Processing: 20 por hora (CPU/GPU)
   - Previne abuso de recursos

3. **Read Protection**
   - 60 requisições por minuto (menos restritivo)
   - Permite bulk operations mas limita
   - DDoS mitigation mínimo

4. **Isolation**
   - Limites separados por usuário/IP
   - Um usuário não afeta outro
   - Fair resource allocation

5. **Proxy Support**
   - Detecta IP real via X-Forwarded-For
   - Suporta múltiplos proxies
   - Não usa IP falso diretamente

---

## 🧩 Extensibilidade

### **Adicionar Novo Endpoint com Limite**

```java
// 1. Adicionar identificação no RateLimitingInterceptor
private boolean isMyEndpoint(String uri) {
  return uri.contains("/api/my/endpoint");
}

// 2. Adicionar handler no preHandle()
if (isMyEndpoint(requestUri)) {
  return handleMyRateLimit(userId, request, response);
}

// 3. Criar bucket e handler
private boolean handleMyRateLimit(...) {
  Bucket bucket = myBuckets.computeIfAbsent(userId, 
    key -> createMyBucket());
  
  if (bucket.tryConsume(1)) {
    response.addHeader("X-RateLimit-Limit", "X");
    return true;
  } else {
    response.setStatus(429);
    response.getWriter().write("{\"error\":\"...\"}");
    return false;
  }
}

// 4. Criar bucket
private Bucket createMyBucket() {
  Bandwidth limit = Bandwidth.classic(
    10, Refill.intervally(10, Duration.ofHours(1)));
  return Bucket4j.builder().addLimit(limit).build();
}
```

---

## 🚀 Melhorias Futuras

### **Próximas Iterações**

1. **Redis Distribution**
   - Distribuir rate limiting entre instâncias
   - Importante para load-balanced deployments

2. **Dynamic Configuration**
   - Limites no banco de dados
   - Ajustes sem restart

3. **Analytics**
   - Registrar eventos de rate limit
   - Análise de padrões de ataque

4. **Whitelisting**
   - IPs confiáveis
   - Usuários premium com limites maiores

5. **Gradual Backoff**
   - Aumentar delay com múltiplas violações
   - Proteção mais agressiva

6. **CAPTCHA Integration**
   - Após N tentativas de login
   - Bypass com CAPTCHA

---

## 📚 Referências

- **Bucket4j**: https://github.com/vladimir-bukhtoyarov/bucket4j
- **RFC 6585**: HTTP Status 429
- **OWASP**: Rate Limiting
- **Spring**: HandlerInterceptor & WebMvcConfigurer

---

## 📝 Resumo das Mudanças

| Arquivo | Tipo | Status |
|---------|------|--------|
| pom.xml | Dependency | ➕ bucket4j |
| RateLimitingConfig.java | NEW | ✅ |
| RateLimitingInterceptor.java | NEW | ✅ |
| RateLimitExceededException.java | NEW | ✅ |
| RateLimitingInterceptorTest.java | NEW | ✅ 10 testes |
| WebConfig.java | MODIFIED | ✅ Registra interceptor |
| GlobalExceptionHandler.java | MODIFIED | ✅ Handler 429 |

**Testes**: 91/91 ✅  
**Docker**: Rebuild sucesso ✅  
**Validação**: Rate limit funcionando ✅  

---

## ✨ Conclusão

Sistema de Rate Limiting robusto e testado, protegendo a API contra:
- ✅ Brute force
- ✅ Resource exhaustion
- ✅ Abuso
- ✅ DDoS simples

Pronto para produção com possibilidade de evolução para Redis e análise avançada.
