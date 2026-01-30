# Document AI Platform - Frontend React

Frontend moderno desenvolvido em React 18 com Vite, Tailwind CSS e integração completa com o backend.

## 🚀 Recursos

- ✅ Upload de documentos com drag & drop
- ✅ Polling em tempo real de status
- ✅ Webhook notifications
- ✅ Autenticação JWT
- ✅ Dashboard com métricas
- ✅ Admin panel
- ✅ Responsive design
- ✅ Hot module replacement (HMR)

## 📦 Tecnologias

- **React 18** - UI library
- **Vite** - Build tool
- **Tailwind CSS** - Utility-first CSS
- **React Router** - Client-side routing
- **Axios** - HTTP client
- **Recharts** - Charts library
- **Lucide React** - Icons
- **React Hot Toast** - Notifications
- **Date-fns** - Date utilities

## 🎯 Funcionalidades

### 1. Upload
- Drag & drop support
- Timeout customizável
- Progress bar
- Validação de arquivo
- Suporte: PDF, PNG, JPG, TIFF, TXT

### 2. Documentos
- Lista com polling automático (5s)
- Status em tempo real (PROCESSING, COMPLETED, FAILED)
- Visualização de detalhes
- Histórico de processamento

### 3. Dashboard
- Métricas em tempo real
- Circuit breaker status
- Taxa de sucesso
- Tempo médio de processamento

### 4. Segurança
- Autenticação JWT
- Refresh token automático
- Proteção de rotas
- CORS configurado

## 📋 Estrutura

```
src/
├── components/          # Componentes reutilizáveis
│   ├── UploadComponent.jsx
│   └── DocumentListComponent.jsx
├── pages/               # Páginas
│   ├── LoginPage.jsx
│   ├── HomePage.jsx
│   ├── DocumentDetailPage.jsx
│   ├── DashboardPage.jsx
│   └── AdminPage.jsx
├── services/            # Serviços de API
│   ├── api.js
│   └── documentService.js
├── hooks/               # Custom hooks
│   ├── useAuth.js
│   └── usePolling.js
├── context/             # Context API
│   └── AuthContext.jsx
├── App.jsx              # App root
├── main.jsx             # Entry point
└── index.css            # Global styles
```

## 🔧 Setup

### Pré-requisitos
- Node.js 16+
- npm ou yarn

### Instalação

```bash
# Instalar dependências
npm install

# Copiar arquivo de ambiente
cp .env.example .env.local

# Editar .env.local conforme necessário
```

### Desenvolvimento

```bash
# Iniciar dev server na porta 3000
npm run dev

# Acessar http://localhost:3000
```

O servidor será recarregado automaticamente ao salvar arquivos (HMR).

### Build

```bash
# Build para produção
npm run build

# Preview da build
npm run preview
```

## 🔐 Variáveis de Ambiente

```env
# URL base da API (proxy no Vite)
VITE_API_URL=http://localhost:8080/api

# Informações da aplicação
VITE_APP_NAME=Document AI Platform
VITE_APP_VERSION=1.0.0
```

## 📱 Páginas

| Rota | Descrição | Autenticação |
|------|-----------|--------------|
| `/login` | Tela de login | Pública |
| `/` | Home com upload e documentos | Protegido |
| `/documents/:id` | Detalhes do documento | Protegido |
| `/dashboard` | Dashboard com métricas | Protegido |
| `/admin` | Painel administrativo | Admin |

## 🌐 Integração com Backend

### Endpoints Utilizados

```
POST   /api/auth/login                          - Login
GET    /api/documents                            - Listar documentos
POST   /api/documents/async/upload               - Upload assíncrono
GET    /api/documents/async/{id}/status          - Status instantâneo
GET    /api/documents/async/{id}/status/polling  - Long polling
POST   /api/documents/async/{id}/webhook/register - Registrar webhook
GET    /api/documents/async/dashboard/metrics    - Métricas
```

## 🎨 Customização

### Tailwind Colors

Configurado em `tailwind.config.js`:
```javascript
primary: {
  50: '#f0f9ff',
  500: '#0ea5e9',
  600: '#0284c7',
  700: '#0369a1'
}
```

### Componentes

Todos os componentes usam Tailwind CSS. Customizações em `src/index.css`.

## 🚢 Deploy

### Vercel

```bash
vercel deploy
```

### Docker

```bash
docker build -t document-ai-frontend .
docker run -p 3000:3000 document-ai-frontend
```

### Nginx

```bash
npm run build

# Copiar dist/ para /var/www/html/
sudo cp -r dist/* /var/www/html/
```

## 📊 Performance

- ✅ Code splitting automático
- ✅ Tree shaking
- ✅ Image optimization
- ✅ Lazy loading de rotas
- ✅ Caching estratégico

## 🐛 Debug

Variáveis de ambiente para debug:
```env
VITE_DEBUG=true
```

## 📚 Recursos

- [React Documentation](https://react.dev)
- [Vite Documentation](https://vitejs.dev)
- [Tailwind CSS](https://tailwindcss.com)
- [React Router](https://reactrouter.com)

## 📝 Licença

MIT

## 👨‍💻 Autor

Document AI Team
