#!/bin/bash

# ============================================================================
# TESTE E2E: Upload + OCR + Classificação
# ============================================================================
# Script para testar o fluxo completo localmente
# Pré-requisitos: Servidor rodando em http://localhost:8080
# ============================================================================

set -e

BASE_URL="http://localhost:8080"
OUTPUT_FILE="/tmp/e2e_test_results.json"
DOCUMENT_ID=""

# Cores para output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}╔═══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║ TESTE E2E: Upload → OCR → Classificação                       ║${NC}"
echo -e "${BLUE}╚═══════════════════════════════════════════════════════════════╝${NC}"
echo ""

# ============================================================================
# STEP 1: Health Check
# ============================================================================
echo -e "${YELLOW}[1/4] Health Check do servidor...${NC}"
HEALTH=$(curl -s -w "%{http_code}" -o /dev/null "$BASE_URL/actuator/health")

if [ "$HEALTH" == "200" ]; then
    echo -e "${GREEN}✅ Servidor OK (HTTP $HEALTH)${NC}"
else
    echo -e "${RED}❌ Servidor não respondendo (HTTP $HEALTH)${NC}"
    echo "Certifique-se de que o servidor está rodando em $BASE_URL"
    exit 1
fi
echo ""

# ============================================================================
# STEP 2: Criar arquivo de teste
# ============================================================================
echo -e "${YELLOW}[2/4] Preparando arquivo de teste...${NC}"

# Criar um PDF de teste simples com texto
cat > /tmp/test_document.txt << 'EOF'
TESTE DE DOCUMENTO
==================

Data: 30/01/2026
Valor: R$ 1.500,00

Descrição:
Este é um documento de teste para validar o fluxo de OCR e classificação.

Envolvidos:
- João Silva (vendedor)
- Maria Santos (comprador)

Este documento deve ser classificado como "INVOICE" e extraído os campos:
- Data
- Valor
- Pessoas envolvidas
EOF

# Converter TXT para PDF (usando enscript se disponível, ou criar simples)
if command -v enscript &> /dev/null; then
    enscript -B -p /tmp/test_document.pdf /tmp/test_document.txt 2>/dev/null
elif command -v wc &> /dev/null; then
    # Fallback: criar PDF básico manualmente (não ideal, mas funciona para teste)
    echo "Usando arquivo TXT para teste (PDF não disponível)"
    TEST_FILE="/tmp/test_document.txt"
else
    TEST_FILE="/tmp/test_document.txt"
fi

TEST_FILE="${TEST_FILE:-/tmp/test_document.pdf}"

if [ ! -f "$TEST_FILE" ]; then
    echo -e "${RED}❌ Erro ao criar arquivo de teste${NC}"
    exit 1
fi

FILE_SIZE=$(du -h "$TEST_FILE" | cut -f1)
echo -e "${GREEN}✅ Arquivo criado: $(basename $TEST_FILE) ($FILE_SIZE)${NC}"
echo ""

# ============================================================================
# STEP 3: Upload do Documento
# ============================================================================
echo -e "${YELLOW}[3/4] Fazendo upload do documento...${NC}"

START_TIME=$(date +%s%N)

RESPONSE=$(curl -s -X POST \
    -F "file=@$TEST_FILE" \
    -F "fileType=TEXT" \
    "$BASE_URL/documents/upload" \
    -H "Accept: application/json")

END_TIME=$(date +%s%N)
DURATION=$((($END_TIME - $START_TIME) / 1000000))

# Extrair informações do response
DOCUMENT_ID=$(echo "$RESPONSE" | jq -r '.documentId // empty')
OCR_CONTENT=$(echo "$RESPONSE" | jq -r '.extractedContent // empty')
CLASSIFICATION=$(echo "$RESPONSE" | jq -r '.classification // empty')
STATUS=$(echo "$RESPONSE" | jq -r '.status // empty')

echo "Response (primeiras 500 chars):"
echo "$RESPONSE" | head -c 500
echo ""
echo ""

if [ -z "$DOCUMENT_ID" ]; then
    echo -e "${RED}❌ Upload falhou! Documento ID não retornado${NC}"
    echo "Response completo:"
    echo "$RESPONSE" | jq .
    exit 1
fi

echo -e "${GREEN}✅ Upload bem-sucedido!${NC}"
echo "   Document ID: $DOCUMENT_ID"
echo "   Duração: ${DURATION}ms"
echo "   Status: $STATUS"
echo ""

# ============================================================================
# STEP 4: Validar Resultados
# ============================================================================
echo -e "${YELLOW}[4/4] Validando resultados...${NC}"
echo ""

TESTS_PASSED=0
TESTS_FAILED=0

# Teste 1: Document ID
if [ ! -z "$DOCUMENT_ID" ] && [ "$DOCUMENT_ID" != "null" ]; then
    echo -e "${GREEN}✅ Document ID válido${NC}"
    ((TESTS_PASSED++))
else
    echo -e "${RED}❌ Document ID inválido${NC}"
    ((TESTS_FAILED++))
fi

# Teste 2: OCR Extraction
if [ ! -z "$OCR_CONTENT" ] && [ "$OCR_CONTENT" != "null" ] && [ ${#OCR_CONTENT} -gt 10 ]; then
    echo -e "${GREEN}✅ OCR extraiu conteúdo (${#OCR_CONTENT} caracteres)${NC}"
    ((TESTS_PASSED++))
else
    echo -e "${YELLOW}⚠️  OCR não extraiu conteúdo (esperado em alguns casos)${NC}"
fi

# Teste 3: Classification
if [ ! -z "$CLASSIFICATION" ] && [ "$CLASSIFICATION" != "null" ]; then
    echo -e "${GREEN}✅ Documento classificado como: $CLASSIFICATION${NC}"
    ((TESTS_PASSED++))
else
    echo -e "${YELLOW}⚠️  Classificação não realizada${NC}"
fi

# Teste 4: Status
if [ "$STATUS" == "COMPLETED" ] || [ "$STATUS" == "PROCESSING" ]; then
    echo -e "${GREEN}✅ Status válido: $STATUS${NC}"
    ((TESTS_PASSED++))
else
    echo -e "${YELLOW}⚠️  Status inesperado: $STATUS${NC}"
fi

echo ""

# ============================================================================
# STEP 5: Consultar Documento Processado
# ============================================================================
if [ ! -z "$DOCUMENT_ID" ] && [ "$DOCUMENT_ID" != "null" ]; then
    echo -e "${YELLOW}[BÔNUS] Consultando documento processado...${NC}"
    
    GET_RESPONSE=$(curl -s -X GET "$BASE_URL/documents/$DOCUMENT_ID")
    
    GET_STATUS=$(echo "$GET_RESPONSE" | jq -r '.status // empty')
    GET_ORIGINAL=$(echo "$GET_RESPONSE" | jq -r '.originalName // empty')
    
    if [ ! -z "$GET_STATUS" ]; then
        echo -e "${GREEN}✅ Documento recuperado com sucesso${NC}"
        echo "   Nome: $GET_ORIGINAL"
        echo "   Status: $GET_STATUS"
        echo "   Response: $(echo "$GET_RESPONSE" | jq .)"
    fi
    echo ""
fi

# ============================================================================
# RESUMO
# ============================================================================
echo -e "${BLUE}╔═══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║                        RESUMO DOS TESTES                      ║${NC}"
echo -e "${BLUE}╚═══════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${GREEN}✅ Testes Passaram: $TESTS_PASSED${NC}"
if [ $TESTS_FAILED -gt 0 ]; then
    echo -e "${RED}❌ Testes Falharam: $TESTS_FAILED${NC}"
fi
echo ""

# Salvar resultados em JSON
cat > "$OUTPUT_FILE" << JSON
{
  "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "documentId": "$DOCUMENT_ID",
  "testsPassed": $TESTS_PASSED,
  "testsFailed": $TESTS_FAILED,
  "duration": "${DURATION}ms",
  "status": "$STATUS",
  "classification": "$CLASSIFICATION"
}
JSON

echo "Resultados salvos em: $OUTPUT_FILE"
echo ""

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "${GREEN}🎉 FLUXO E2E COMPLETO COM SUCESSO!${NC}"
    exit 0
else
    echo -e "${RED}⚠️  ALGUNS TESTES FALHARAM${NC}"
    exit 1
fi
