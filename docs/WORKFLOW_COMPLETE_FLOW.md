# 🎯 Fluxo Completo: Upload → OCR → Classificação

## Visão Geral

O Document AI Platform implementa um fluxo end-to-end para processamento de documentos com Reconhecimento Óptico de Caracteres (OCR) e classificação automática usando Machine Learning.

## 1. Arquitetura do Fluxo

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          CLIENT (Frontend/API)                           │
└────────────────────────────┬────────────────────────────────────────────┘
                             │
                    ┌────────▼────────┐
                    │   HTTP Upload   │
                    │ POST /documents │
                    │     /upload     │
                    └────────┬────────┘
                             │
┌────────────────────────────▼────────────────────────────────────────────┐
│                    Infrastructure Layer (REST API)                       │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │  DocumentController                                              │   │
│  │  • uploadDocument(file, fileType)                                │   │
│  │  • getDocument(documentId)                                       │   │
│  │  • listDocuments()                                               │   │
│  │  • deleteDocument(documentId)                                    │   │
│  └─────────────────┬────────────────────────────────────────────────┘   │
└────────────────────▼────────────────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────────────────────┐
│                    Application Layer (Use Cases)                         │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │  ProcessDocumentUseCase                                          │   │
│  │  ┌────────────────────────────────────────────────────────────┐  │   │
│  │  │ executeWithDocumentCreation(input, fileName)              │  │   │
│  │  │ 1. Cria DocumentId único (UUID)                           │  │   │
│  │  │ 2. Cria entidade Document                                 │  │   │
│  │  │ 3. Persiste no repositório                                │  │   │
│  │  │ 4. Extrai conteúdo via OCR                                │  │   │
│  │  │ 5. Classifica documento                                   │  │   │
│  │  │ 6. Persiste resultados                                    │  │   │
│  │  └────────────────────────────────────────────────────────────┘  │   │
│  └─────────────────┬────────────────────────────────────────────────┘   │
│                    │                                                     │
│  ┌─────────────────▼────────────────────────────────────────────────┐   │
│  │  OcrService (Port/Interface)                                    │   │
│  │  • extractText(document): ExtractedContent                      │   │
│  └─────────────────┬────────────────────────────────────────────────┘   │
│                    │                                                     │
│  ┌─────────────────▼────────────────────────────────────────────────┐   │
│  │  ClassificationService (Port/Interface)                         │   │
│  │  • classifyDocument(document): Classification                   │   │
│  └─────────────────┬────────────────────────────────────────────────┘   │
└────────────────────▼────────────────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────────────────────┐
│                   Infrastructure Layer (Adapters)                        │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │  OcrServiceImpl (Tesseract)                                       │   │
│  │  • Executa OCR com Tesseract 5.x                                │   │
│  │  • Suporta: PDF, IMAGE (PNG/JPG)                                │   │
│  │  • Idiomas: Portuguese, English                                 │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │  ClassificationServiceImpl (Ollama/LLM)                          │   │
│  │  • Classifica usando LLM (via Ollama)                           │   │
│  │  • Categorias: Invoice, Receipt, Report, etc.                   │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │  DocumentRepository (JPA/PostgreSQL)                            │   │
│  │  • CRUD de documentos                                           │   │
│  │  • Consultas especializadas                                     │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │  DocumentStorageService (FileSystem)                            │   │
│  │  • Armazena arquivos originais                                  │   │
│  │  • Localização: /tmp/document-ai/uploads/                       │   │
│  └──────────────────────────────────────────────────────────────────┘   │
└────────────────────┬────────────────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────────────────────┐
│                         Domain Layer (Entities)                          │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │  Document                                                        │   │
│  │  • id: DocumentId (UUID)                                        │   │
│  │  • status: DocumentStatus (RECEIVED → PROCESSING → COMPLETED)   │   │
│  │  • type: DocumentType (PDF, IMAGE, TXT)                         │   │
│  │  • extractedText: String                                        │   │
│  │  • classification: String (label)                               │   │
│  │  • classificationConfidence: Integer (0-100%)                   │   │
│  │  • userId: String                                              │   │
│  │  • createdAt: ZonedDateTime                                     │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │  ExtractedContent (Value Object)                                │   │
│  │  • content: String                                              │   │
│  │  • language: String                                             │   │
│  │  • confidence: Integer                                          │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │  ProcessingResult (Value Object)                                │   │
│  │  • status: ProcessingStatus                                     │   │
│  │  • classification: String                                       │   │
│  │  • confidence: Integer                                          │   │
│  └──────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
```

## 2. Sequência do Fluxo Completo

### 2.1 Upload e Inicial Processamento

```
Cliente                          API                              DB
  │                              │                               │
  ├─ POST /documents/upload ────►│                               │
  │  (file + fileType)           │                               │
  │                              ├─ Validar arquivo              │
  │                              ├─ Gerar DocumentId (UUID)      │
  │                              ├─ Criar Document entity        │
  │                              ├─ Salvar no DB ───────────────►│
  │                              │                               │
```

### 2.2 Execução de OCR

```
API                          OcrService             FileSystem         Tesseract
 │                                 │                    │                  │
 ├─ extractText(document) ────────►│                    │                  │
 │                                 ├─ Copiar arquivo ──►│                  │
 │                                 ├─ Chamar tesseract ─┼─────────────────►│
 │                                 │                    │                  │
 │                                 │                    │     <extracted>  │
 │                                 │◄─────────────────────────────────────┤
 │◄─ ExtractedContent ─────────────┤                    │                  │
 │  (text, language, confidence)   │                    │                  │
```

### 2.3 Classificação Automática

```
API                      ClassificationService         LLM (Ollama)
 │                              │                            │
 ├─ classify(document) ────────►│                            │
 │  (extracted text)            ├─ Montar prompt ───────────►│
 │                              │                            │
 │                              │  <classification>          │
 │                              │◄────────────────────────────┤
 │◄─ Classification ────────────┤                            │
 │  (label, confidence)         │                            │
```

### 2.4 Persistência e Resposta

```
API                          DB                         Cliente
 │                            │                            │
 ├─ Update Document ─────────►│                            │
 │  (extractedText,           │                            │
 │   classification,          │                            │
 │   status=COMPLETED)        │                            │
 │                            │                            │
 ├─ Retornar resultado ──────────────────────────────────►│
 │  (documentId, status,       │                            │
 │   extractedContent,         │                            │
 │   classification, conf)     │                            │
```

## 3. DTOs e Response

### 3.1 Entrada (ProcessDocumentInput)

```json
{
  "file": "<binary file content>",
  "fileType": "PDF | IMAGE | TXT",
  "metadata": {
    "originalName": "documento.pdf",
    "source": "api|webhook|batch"
  }
}
```

### 3.2 Saída (ProcessDocumentOutput)

```json
{
  "documentId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED | PROCESSING | FAILED",
  "extractedTextPreview": "DOCUMENTO DE TESTE\nData: 30/01/2026...",
  "classification": "INVOICE",
  "confidencePercentage": 95
}
```

### 3.3 Entidade Document (Banco de Dados)

```sql
CREATE TABLE documents (
  id VARCHAR(255) PRIMARY KEY,
  user_id VARCHAR(255) NOT NULL,
  original_name VARCHAR(255) NOT NULL,
  type VARCHAR(10) CHECK (type IN ('PDF', 'IMAGE', 'TXT')),
  status VARCHAR(20) CHECK (status IN ('RECEIVED', 'PROCESSING', 'COMPLETED', 'FAILED')),
  extracted_text TEXT,
  classification_label VARCHAR(255),
  classification_confidence INTEGER,
  created_at TIMESTAMP NOT NULL,
  FOREIGN KEY (user_id) REFERENCES users(id)
);
```

## 4. Tipos de Arquivo Suportados

| Tipo | Extensão | OCR Support | Classificação | Exemplo |
|------|----------|-------------|---------------|---------|
| PDF | `.pdf` | ✅ Tesseract | ✅ LLM | invoice.pdf |
| IMAGE | `.png, .jpg, .jpeg` | ✅ Tesseract | ✅ LLM | scan.jpg |
| TEXT | `.txt` | ⚠️ Sem OCR | ✅ LLM | document.txt |

## 5. Status do Documento

```
RECEIVED ────► PROCESSING ────► COMPLETED
               (OCR + Classification)
                    │
                    └───────► FAILED (erro no processamento)
```

## 6. Endpoints da API

### Upload e Processamento

```bash
# Upload com processamento automático
POST /documents/upload
Content-Type: multipart/form-data
Authorization: Bearer <token>

Form Data:
  file: <binary file>
  fileType: PDF | IMAGE | TXT

Response (201 Created):
{
  "documentId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED",
  "extractedTextPreview": "...",
  "classification": "INVOICE",
  "confidencePercentage": 95
}
```

### Recuperação de Documento

```bash
# Obter documento processado
GET /documents/{documentId}
Authorization: Bearer <token>

Response (200 OK):
{
  "documentId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED",
  "extractedTextPreview": "...",
  "classification": "INVOICE",
  "confidencePercentage": 95
}
```

### Listar Documentos

```bash
GET /documents?page=0&size=10
Authorization: Bearer <token>

Response (200 OK):
{
  "content": [...],
  "totalElements": 42,
  "totalPages": 5,
  "currentPage": 0
}
```

## 7. Exemplo de Uso Completo

### CLI com curl

```bash
#!/bin/bash

# 1. Autenticar
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user@example.com","password":"pass"}' | jq -r '.token')

# 2. Upload de documento
RESPONSE=$(curl -s -X POST http://localhost:8080/documents/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@invoice.pdf" \
  -F "fileType=PDF")

DOCUMENT_ID=$(echo $RESPONSE | jq -r '.documentId')
echo "Document ID: $DOCUMENT_ID"

# 3. Aguardar processamento (se async)
sleep 2

# 4. Recuperar resultado
curl -s http://localhost:8080/documents/$DOCUMENT_ID \
  -H "Authorization: Bearer $TOKEN" | jq '.'
```

### Python

```python
import requests

# Configurar
BASE_URL = "http://localhost:8080"
TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGc..."

# Upload
with open("invoice.pdf", "rb") as f:
    files = {"file": f}
    data = {"fileType": "PDF"}
    headers = {"Authorization": f"Bearer {TOKEN}"}
    
    response = requests.post(
        f"{BASE_URL}/documents/upload",
        files=files,
        data=data,
        headers=headers
    )

result = response.json()
document_id = result["documentId"]
print(f"Classification: {result['classification']}")
print(f"Confidence: {result['confidencePercentage']}%")
```

## 8. Tratamento de Erros

| Erro | Status | Causa | Solução |
|------|--------|-------|---------|
| Empty file | 400 | Arquivo vazio | Verificar arquivo antes de enviar |
| Invalid type | 400 | Tipo não suportado | Usar PDF, IMAGE ou TXT |
| File too large | 413 | Tamanho > 50MB | Reduzir tamanho do arquivo |
| OCR Timeout | 504 | OCR demorou > 30s | Tentar novamente ou usar arquivo menor |
| Unauthorized | 401 | Token inválido/expirado | Fazer novo login |
| Not found | 404 | Document não existe | Verificar documentId |

## 9. Performance e SLAs

| Métrica | Alvo |
|---------|------|
| Upload | < 2s |
| OCR (1 página PDF) | < 10s |
| Classificação | < 3s |
| Total (end-to-end) | < 20s |
| Throughput | 100 docs/min |
| Disponibilidade | 99.9% |

## 10. Fluxo de Teste E2E

```java
// 1. Setup: criar usuário e token
UserAccountEntity user = createTestUser();
String token = jwtProvider.generateToken(user.getId(), user.getEmail(), roles);

// 2. Upload
MockMultipartFile file = new MockMultipartFile("file", "test.pdf", 
    "application/pdf", pdfContent);
MvcResult uploadResult = mockMvc.perform(multipart("/documents/upload")
    .file(file)
    .param("fileType", "PDF")
    .header("Authorization", "Bearer " + token))
    .andExpect(status().isCreated())
    .andReturn();

ProcessDocumentOutput output = objectMapper.readValue(
    uploadResult.getResponse().getContentAsString(), 
    ProcessDocumentOutput.class
);

// 3. Verificar resultados
assert output.getDocumentId() != null;
assert output.getStatus().equals("COMPLETED");
assert output.getClassification() != null;

// 4. Recuperar documento
MvcResult getResult = mockMvc.perform(get("/documents/" + output.getDocumentId())
    .header("Authorization", "Bearer " + token))
    .andExpect(status().isOk())
    .andReturn();

ProcessDocumentOutput retrieved = objectMapper.readValue(
    getResult.getResponse().getContentAsString(), 
    ProcessDocumentOutput.class
);

assert retrieved.getDocumentId().equals(output.getDocumentId());
```

## 11. Dependências Críticas

- **Tesseract 5.x**: OCR engine
- **Ollama**: LLM inference (local)
- **PostgreSQL 15**: Database
- **Spring Boot 3.2.1**: Framework
- **Java 21**: Runtime

## 12. Próximos Passos

1. ✅ **Validado**: Fluxo completo está implementado
2. 🔄 **Em Progresso**: Melhorar tratamento de erros e timeouts
3. ⏳ **Pendente**: Implementar summarization
4. ⏳ **Pendente**: Implementar semantic search
5. ⏳ **Pendente**: Criar frontend
