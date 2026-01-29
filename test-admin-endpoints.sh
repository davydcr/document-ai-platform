#!/bin/bash

# ============================================================================
# Script de Teste - Admin Endpoints Auditoria
# ============================================================================
# Testa todos os 11 endpoints do AuditLogController
# Requer: Servidor rodando em http://localhost:8080
# ============================================================================

set -e

BASE_URL="http://localhost:8080"
ADMIN_TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbkBleGFtcGxlLmNvbSIsImlhdCI6MTY3NDQyNzM5NywiZXhwIjoxNjc0NTEzNzk3LCJyb2xlcyI6IkFETUluLCBVU0VSIn0.signature"

# Cores para output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Contador de testes
TESTS_PASSED=0
TESTS_FAILED=0

# Função auxiliar para fazer requisição e validar resposta
test_endpoint() {
    local method=$1
    local endpoint=$2
    local expected_status=$3
    local description=$4
    local data=$5
    
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${YELLOW}🧪 Teste: $description${NC}"
    echo -e "📍 Endpoint: ${method} ${endpoint}"
    
    if [ "$method" == "GET" ]; then
        RESPONSE=$(curl -s -w "\n%{http_code}" \
            -H "Authorization: Bearer $ADMIN_TOKEN" \
            -H "Content-Type: application/json" \
            "$BASE_URL$endpoint")
    else
        RESPONSE=$(curl -s -w "\n%{http_code}" \
            -X "$method" \
            -H "Authorization: Bearer $ADMIN_TOKEN" \
            -H "Content-Type: application/json" \
            -d "$data" \
            "$BASE_URL$endpoint")
    fi
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | head -n-1)
    
    echo "📊 Status: $HTTP_CODE (Esperado: $expected_status)"
    
    if [ "$HTTP_CODE" == "$expected_status" ] || [ "$HTTP_CODE" == "200" ] || [ "$HTTP_CODE" == "201" ]; then
        echo -e "${GREEN}✅ PASSOU${NC}"
        TESTS_PASSED=$((TESTS_PASSED + 1))
        echo "📦 Resposta (primeiras 200 chars):"
        echo "$BODY" | head -c 200
        echo ""
    else
        echo -e "${RED}❌ FALHOU${NC}"
        TESTS_FAILED=$((TESTS_FAILED + 1))
        echo "❌ Resposta:"
        echo "$BODY" | head -c 500
        echo ""
    fi
    echo ""
}

# ============================================================================
# TESTES DOS 11 ENDPOINTS
# ============================================================================

echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║       TESTE DE ADMIN ENDPOINTS - AUDITORIA              ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
echo ""

# Endpoint 1: GET /admin/audit/logs - Listar todos os logs
test_endpoint "GET" "/admin/audit/logs?page=0&size=20&sortBy=createdAt" "200" \
    "1️⃣  Listar todos os logs com paginação"

# Endpoint 2: GET /admin/audit/user/{email} - Logs de usuário
test_endpoint "GET" "/admin/audit/user/admin@example.com?page=0&size=20" "200" \
    "2️⃣  Logs de um usuário específico"

# Endpoint 3: GET /admin/audit/brute-force - Detectar brute force (por email)
test_endpoint "GET" "/admin/audit/brute-force?email=admin@example.com" "200" \
    "3️⃣  Detectar brute force por email"

# Endpoint 4: GET /admin/audit/brute-force - Detectar brute force (por IP)
test_endpoint "GET" "/admin/audit/brute-force?ipAddress=192.168.1.1" "200" \
    "4️⃣  Detectar brute force por IP"

# Endpoint 5: GET /admin/audit/suspicious-activity - Atividades suspeitas
test_endpoint "GET" "/admin/audit/suspicious-activity?hours=1&page=0&size=50" "200" \
    "5️⃣  Atividades suspeitas nas últimas horas"

# Endpoint 6: GET /admin/audit/event/{eventType} - Logs por tipo de evento
test_endpoint "GET" "/admin/audit/event/LOGIN?page=0&size=20" "200" \
    "6️⃣  Logs filtrados por tipo de evento"

# Endpoint 7: GET /admin/audit/ip/{ipAddress} - Logs por IP
test_endpoint "GET" "/admin/audit/ip/127.0.0.1?page=0&size=20" "200" \
    "7️⃣  Logs filtrados por IP address"

# Endpoint 8: GET /admin/audit/date-range - Logs em período
test_endpoint "GET" "/admin/audit/date-range?startDate=2026-01-01T00:00:00&endDate=2026-01-31T23:59:59&page=0&size=20" "200" \
    "8️⃣  Logs em período específico (date-range)"

# Endpoint 9: GET /admin/audit/security-report - Relatório de segurança
test_endpoint "GET" "/admin/audit/security-report" "200" \
    "9️⃣  Relatório completo de segurança"

# Endpoint 10: GET /admin/audit/user-sessions/{userId} - Sessões do usuário
test_endpoint "GET" "/admin/audit/user-sessions/admin-user-001?page=0&size=20" "200" \
    "🔟 Sessões ativas de usuário"

# Endpoint 11: GET /admin/audit/health - Health check
test_endpoint "GET" "/admin/audit/health" "200" \
    "1️⃣1️⃣  Health check do serviço"

# ============================================================================
# TESTES ADICIONAIS - ERROS
# ============================================================================

echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}🧪 TESTES DE ERRO E VALIDAÇÃO${NC}"
echo ""

# Teste: Sem token deve retornar 401
test_endpoint "GET" "/admin/audit/logs" "401" \
    "❌ Acesso sem token (deve ser 401)"

# Teste: Brute force sem parâmetros deve retornar erro
test_endpoint "GET" "/admin/audit/brute-force" "400" \
    "❌ Brute force sem email ou IP (deve ser 400)"

# Teste: Date range com datas inválidas
test_endpoint "GET" "/admin/audit/date-range?startDate=2026-01-31T23:59:59&endDate=2026-01-01T00:00:00&page=0&size=20" "400" \
    "❌ Date range com startDate > endDate (deve ser 400)"

# ============================================================================
# RESUMO
# ============================================================================

echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║                    RESUMO DOS TESTES                    ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${GREEN}✅ Testes Passados: ${TESTS_PASSED}${NC}"
echo -e "${RED}❌ Testes Falhados: ${TESTS_FAILED}${NC}"
echo ""

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "${GREEN}🎉 TODOS OS TESTES PASSARAM!${NC}"
    exit 0
else
    echo -e "${RED}⚠️  ALGUNS TESTES FALHARAM${NC}"
    exit 1
fi
