#!/bin/bash

#############################################################################
# Document AI Platform - Startup Script
# 
# Inicia todos os serviços da plataforma usando Docker Compose
# Uso: ./start.sh [comando]
#   - start:     Inicia os containers (padrão)
#   - stop:      Para os containers
#   - restart:   Reinicia os containers
#   - logs:      Mostra os logs em tempo real
#   - status:    Mostra status dos containers
#   - down:      Para e remove os containers
#   - build:     Rebuild das imagens
#############################################################################

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DOCKER_COMPOSE_FILE="${SCRIPT_DIR}/docker-compose.yml"
PROJECT_NAME="document-ai-platform"

# Cores para output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Funções de log
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Verifica se Docker está instalado
check_docker() {
    if ! command -v docker &> /dev/null; then
        log_error "Docker não está instalado. Por favor, instale Docker."
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        log_error "Docker Compose não está instalado. Por favor, instale Docker Compose."
        exit 1
    fi
}

# Valida docker-compose.yml
validate_compose() {
    if [ ! -f "$DOCKER_COMPOSE_FILE" ]; then
        log_error "Arquivo docker-compose.yml não encontrado em: $DOCKER_COMPOSE_FILE"
        exit 1
    fi
    
    log_info "Validando docker-compose.yml..."
    docker-compose -f "$DOCKER_COMPOSE_FILE" config > /dev/null || {
        log_error "docker-compose.yml inválido"
        exit 1
    }
}

# Inicia os containers
start_services() {
    log_info "🚀 Iniciando serviços de Document AI Platform..."
    
    docker-compose -f "$DOCKER_COMPOSE_FILE" up -d
    
    log_info "Aguardando serviços ficarem saudáveis..."
    sleep 5
    
    show_status
    
    log_info "✅ Serviços iniciados com sucesso!"
    echo ""
    echo "═══════════════════════════════════════════════════════════"
    echo "📊 Endpoints disponíveis:"
    echo "═══════════════════════════════════════════════════════════"
    echo "  🌐 API REST:           http://localhost:8080/api"
    echo "  📚 Swagger UI:         http://localhost:8080/api/swagger-ui.html"
    echo "  📖 OpenAPI JSON:       http://localhost:8080/api/v3/api-docs"
    echo "  📊 Prometheus:         http://localhost:8080/api/prometheus"
    echo "  ❤️  Health:            http://localhost:8080/api/actuator/health"
    echo "  🤖 Ollama:             http://localhost:11434"
    echo "  🐰 RabbitMQ UI:        http://localhost:15672 (guest:guest)"
    echo "  🐘 PostgreSQL:         localhost:5432"
    echo "═══════════════════════════════════════════════════════════"
    echo ""
}

# Para os containers
stop_services() {
    log_info "🛑 Parando serviços..."
    docker-compose -f "$DOCKER_COMPOSE_FILE" stop
    log_info "✅ Serviços parados"
}

# Reinicia os containers
restart_services() {
    log_info "🔄 Reiniciando serviços..."
    docker-compose -f "$DOCKER_COMPOSE_FILE" restart
    log_info "✅ Serviços reiniciados"
    sleep 3
    show_status
}

# Mostra status dos containers
show_status() {
    echo ""
    log_info "📋 Status dos containers:"
    echo "─────────────────────────────────────────────────────────"
    docker-compose -f "$DOCKER_COMPOSE_FILE" ps
    echo "─────────────────────────────────────────────────────────"
}

# Mostra logs
show_logs() {
    log_info "📜 Exibindo logs (Ctrl+C para sair)..."
    docker-compose -f "$DOCKER_COMPOSE_FILE" logs -f
}

# Remove containers
down_services() {
    log_warn "⚠️  Isso vai remover os containers (dados serão preservados)"
    read -p "Continuar? (s/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Ss]$ ]]; then
        docker-compose -f "$DOCKER_COMPOSE_FILE" down
        log_info "✅ Containers removidos"
    else
        log_info "Operação cancelada"
    fi
}

# Rebuild das imagens
rebuild_images() {
    log_info "🔨 Fazendo rebuild das imagens..."
    docker-compose -f "$DOCKER_COMPOSE_FILE" build --no-cache
    log_info "✅ Build completo"
}

# Limpa volumes (CUIDADO!)
cleanup_volumes() {
    log_warn "⚠️  AVISO: Isso vai DELETAR todos os dados (volumes)"
    read -p "Digite 'sim' para confirmar: " confirm
    if [ "$confirm" = "sim" ]; then
        log_warn "Limpando volumes..."
        docker-compose -f "$DOCKER_COMPOSE_FILE" down -v
        log_info "✅ Volumes removidos"
    else
        log_info "Operação cancelada"
    fi
}

# Exibe ajuda
show_help() {
    echo "Usage: $0 [command]"
    echo ""
    echo "Commands:"
    echo "  start       - Inicia os containers (padrão)"
    echo "  stop        - Para os containers"
    echo "  restart     - Reinicia os containers"
    echo "  logs        - Mostra logs em tempo real"
    echo "  status      - Mostra status dos containers"
    echo "  down        - Para e remove os containers"
    echo "  build       - Faz rebuild das imagens"
    echo "  clean       - Remove volumes (⚠️  Deleta dados!)"
    echo "  help        - Mostra esta mensagem"
    echo ""
    echo "Examples:"
    echo "  ./start.sh              # Inicia serviços"
    echo "  ./start.sh logs         # Mostra logs"
    echo "  ./start.sh restart      # Reinicia serviços"
}

# ============================================================
# Main
# ============================================================

check_docker
validate_compose

COMMAND="${1:-start}"

case "$COMMAND" in
    start)
        start_services
        ;;
    stop)
        stop_services
        ;;
    restart)
        restart_services
        ;;
    logs)
        show_logs
        ;;
    status)
        show_status
        ;;
    down)
        down_services
        ;;
    build)
        rebuild_images
        ;;
    clean)
        cleanup_volumes
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        log_error "Comando desconhecido: $COMMAND"
        echo ""
        show_help
        exit 1
        ;;
esac

exit 0
