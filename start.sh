#!/bin/bash

# Document AI Platform - Start Script
# Inicia a aplicação completa com PostgreSQL e Backend

set -e

echo "🚀 Document AI Platform - Startup Script"
echo "=========================================="
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Check if Docker is running
echo "📦 Verificando Docker..."
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker não está instalado!${NC}"
    exit 1
fi

if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}❌ Docker não está rodando!${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Docker OK${NC}"
echo ""

# Build and start services
echo "🏗️  Construindo e iniciando serviços..."
docker-compose up -d --build

echo ""
echo "⏳ Aguardando serviços iniciarem..."
sleep 10

# Check health
echo ""
echo "🏥 Verificando saúde dos serviços..."

# PostgreSQL
if docker-compose exec -T postgres pg_isready -U document_ai -d document_ai_platform > /dev/null 2>&1; then
    echo -e "${GREEN}✓ PostgreSQL está rodando${NC}"
else
    echo -e "${RED}❌ PostgreSQL não está respondendo${NC}"
    exit 1
fi

# Backend
BACKEND_READY=0
for i in {1..30}; do
    if curl -sf http://localhost:8080/api/documents > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Backend está rodando${NC}"
        BACKEND_READY=1
        break
    fi
    echo "  Tentativa $i/30..."
    sleep 2
done

if [ $BACKEND_READY -eq 0 ]; then
    echo -e "${RED}❌ Backend não iniciou corretamente${NC}"
    echo "Logs:"
    docker-compose logs backend
    exit 1
fi

echo ""
echo "=========================================="
echo -e "${GREEN}✅ Aplicação iniciada com sucesso!${NC}"
echo "=========================================="
echo ""
echo "📊 Serviços disponíveis:"
echo "  📱 API REST:        http://localhost:8080/api"
echo "  📚 Swagger UI:      http://localhost:8080/api/swagger-ui.html"
echo "  📖 OpenAPI JSON:    http://localhost:8080/api/v3/api-docs"
echo "  🗄️  PostgreSQL:      localhost:5432"
echo ""
echo "💡 Comandos úteis:"
echo "  Logs:               docker-compose logs -f backend"
echo "  Parar:              docker-compose down"
echo "  Parar e remover:    docker-compose down -v"
echo ""
echo "🧪 Testar upload de documento:"
echo "  curl -F 'file=@documento.pdf' http://localhost:8080/api/documents/upload"
echo ""
