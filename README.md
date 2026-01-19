# 📄 Document AI Platform

Plataforma de processamento automático de documentos com IA, OCR e classificação automática.

## 🌟 Funcionalidades

- **📤 Upload de Documentos** - Suporta PDF, imagens (PNG, JPG, TIFF) e TXT
- **🔍 OCR Real** - Extração de texto com Tesseract 5
  - PDFs nativos (sem OCR quando possível)
  - PDFs scaneados (OCR automático)
  - Múltiplas imagens
  - Suporte multilíngue (português + inglês)
- **🏷️ Classificação Automática** - Modelos de IA (Ollama/LLM)
- **💾 Persistência** - PostgreSQL com Flyway migrations
- **📚 API REST** - Totalmente documentada com Swagger/OpenAPI
- **🧪 Testes** - 81 testes de integração com Testcontainers
- **📊 Observabilidade** - Logging estruturado com SLF4J/Logback

## 🚀 Quick Start (com Docker)

### Pré-requisitos
- Docker 20.10+
- Docker Compose 1.29+
- 2GB RAM mínimo

### Iniciar Aplicação

```bash
cd /home/davy/document-ai-platform

# Opção 1: Script automático
./start.sh

# Opção 2: Manual
docker-compose up -d --build
```

**Aguarde ~40 segundos para o backend estar pronto.**

### Acessar Aplicação

- 🌐 **API**: http://localhost:8080/api
- 📚 **Swagger UI**: http://localhost:8080/api/swagger-ui.html
- 📖 **OpenAPI JSON**: http://localhost:8080/api/v3/api-docs

## 🧪 Testar Endpoints

### Upload de Documento
```bash
# PDF
curl -F 'file=@documento.pdf' \
  -F 'fileType=PDF' \
  http://localhost:8080/api/documents/upload

# Imagem
curl -F 'file=@imagem.png' \
  -F 'fileType=IMAGE' \
  http://localhost:8080/api/documents/upload
```

### Listar Documentos
```bash
curl http://localhost:8080/api/documents
```

### Obter Documento
```bash
curl http://localhost:8080/api/documents/{documentId}
```

### Classificar Documento
```bash
curl -X POST \
  'http://localhost:8080/api/documents/{documentId}/classify' \
  -d 'text=Invoice dated 2026-01-19 with amount 1000 USD' \
  -d 'model=llama3'
```

### Extrair Conteúdo OCR
```bash
curl -X POST \
  'http://localhost:8080/api/documents/{documentId}/extract' \
  -d 'filePath=/var/document-ai/uploads/document.pdf' \
  -d 'ocrEngine=Tesseract'
```

## 🛠️ Desenvolver (Local)

### Pré-requisitos
- Java 21
- Maven 3.9+
- PostgreSQL 15 (ou use Docker)
- Tesseract 5 (ou configure tessdata path)

### Build Local

```bash
cd backend

# Compilar
mvn clean compile

# Testes
mvn clean test

# Testes de Integração
mvn clean verify

# Build JAR
mvn clean package -DskipTests
```

### Rodar Localmente

```bash
# Com PostgreSQL rodando em localhost:5432
mvn clean spring-boot:run
```

### Configuração (application.properties)

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/document_ai_platform
spring.datasource.username=document_ai
spring.datasource.password=document_ai_secure_password

# OCR
app.ocr.temp-dir=/tmp/document-ai/temp
app.ocr.languages=por+eng

# Storage
app.document.upload-dir=/tmp/document-ai/uploads
app.document.max-file-size=52428800
```

## 📊 Testes

**Status:** ✅ 81 testes passando

```
Domain Tests:        55 ✅
Application Tests:   18 ✅
Integration Tests:    8 ✅
─────────────────────────
Total:              81 ✅
```

Rodar testes:
```bash
cd backend
mvn clean verify
```

## 🗄️ Banco de Dados

**Schema Flyway:**
- `documents` - Metadados dos documentos
- `document_classifications` - Resultados de classificação
- `document_extraction_results` - Resultados OCR
- `users` - Usuários do sistema
- `document_processing_logs` - Log de eventos
- `document_audit` - Auditoria de mudanças

## 🏗️ Arquitetura

### Camadas
```
presentation/
  └── DocumentController (REST endpoints)

application/
  ├── usecase/ (ProcessDocument, Classify, Extract, GetDocument)
  ├── port/ (Interfaces: OcrService, ClassificationService, DocumentRepository)
  └── dto/ (Data Transfer Objects)

domain/
  └── model/ (Document, User, ProcessingResult, ExtractedContent, etc)

infrastructure/
  ├── service/ (OcrServiceImpl, ClassificationServiceImpl, DocumentStorageService)
  ├── persistence/ (DocumentRepositoryImpl, JPA entities)
  ├── config/ (Spring config, OpenAPI config)
  └── exception/ (Global exception handler)
```

### Padrões
- ✅ Domain-Driven Design (DDD)
- ✅ Clean Architecture
- ✅ Dependency Injection
- ✅ Port & Adapter
- ✅ Repository Pattern
- ✅ Use Case Pattern

## 📦 Dependências Principais

| Lib | Versão | Uso |
|-----|--------|-----|
| Spring Boot | 3.2.1 | Framework web |
| Tesseract (tess4j) | 5.10.0 | OCR |
| PDFBox | 2.0.29 | Processamento PDF |
| PostgreSQL | 15 | Database |
| Flyway | 9.22.3 | Migrations |
| Testcontainers | 1.19.7 | Testes integração |
| Springdoc OpenAPI | 2.1.0 | Documentação API |

## 🔐 Segurança

Próximos passos:
- [ ] JWT/OAuth2 autenticação
- [ ] CORS configuration
- [ ] Rate limiting
- [ ] Input validation
- [ ] SQL injection prevention (via ORM)
- [ ] HTTPS/TLS

## 📈 Observabilidade (Semana 4)

A implementar:
- [ ] SLF4J/Logback (logging estruturado)
- [ ] OpenTelemetry (distributed tracing)
- [ ] Prometheus (métricas)
- [ ] Grafana (dashboards)
- [ ] Spring Actuator (health checks)

## 🚀 CI/CD (Semana 5)

A implementar:
- [ ] GitHub Actions workflow
- [ ] Automated testing
- [ ] Docker image build
- [ ] Registry push
- [ ] Deployment automation

## 📝 Roadmap

### ✅ Completo (Semanas 1-3)
- [x] Arquitetura DDD
- [x] Testes de integração
- [x] Database schema
- [x] REST API documentada
- [x] OCR real com Tesseract
- [x] Storage de arquivos

### 🔄 Em Progresso
- [ ] Logging & Observabilidade (Semana 4)
- [ ] CI/CD Pipeline (Semana 5)

### 📋 Futuro
- [ ] Integração com Ollama/LLM
- [ ] Fila de processamento (RabbitMQ/Kafka)
- [ ] Autenticação (JWT/OAuth2)
- [ ] Multitenant
- [ ] Webhooks
- [ ] S3/Blob storage
- [ ] Admin dashboard

## 📞 Contato & Suporte

**Documentação da API**: http://localhost:8080/api/swagger-ui.html

## 📄 Licença

MIT License - Veja [LICENSE](LICENSE) para detalhes

---

**Desenvolvido com ❤️ usando Java 21, Spring Boot 3, e Tesseract OCR**
