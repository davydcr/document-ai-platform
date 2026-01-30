# Frontend - Document AI Platform

Frontend React pronto para produção com integração total com backend.

## 🚀 Quick Start

```bash
# 1. Instalar dependências
npm install

# 2. Copiar .env
cp .env.example .env

# 3. Iniciar dev server
npm run dev

# 4. Abrir http://localhost:3000
```

## 📦 Estrutura

```
frontend/
├── index.html                    # HTML raiz
├── package.json                  # Dependências
├── vite.config.js               # Configuração Vite
├── tailwind.config.js           # Tailwind CSS
├── postcss.config.js            # PostCSS
├── .env.example                 # Variáveis de exemplo
└── src/
    ├── main.jsx                 # Entry point
    ├── App.jsx                  # Router raiz
    ├── index.css                # Estilos globais
    ├── pages/                   # Páginas (rotas)
    ├── components/              # Componentes
    ├── services/                # Serviços HTTP
    ├── context/                 # Context API
    └── hooks/                   # Custom hooks
```

## 🎯 Funcionalidades

- ✅ Upload com drag & drop
- ✅ Status de documentos com polling
- ✅ Webhooks
- ✅ Dashboard em tempo real
- ✅ Admin panel
- ✅ Autenticação JWT
- ✅ Responsive design

## 📱 Páginas

- **/login** - Login
- **/** - Home (upload + documentos)
- **/documents/:id** - Detalhes do documento
- **/dashboard** - Métricas
- **/admin** - Admin panel

## 🔌 Endpoints Utilizados

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | /api/auth/login | Login |
| GET | /api/documents | Listar docs |
| POST | /api/documents/async/upload | Upload |
| GET | /api/documents/async/{id}/status | Status |
| GET | /api/documents/async/{id}/status/polling | Polling |
| POST | /api/documents/async/{id}/webhook/register | Webhook |
| GET | /api/documents/async/dashboard/metrics | Métricas |

## 📝 Variáveis de Ambiente

```env
VITE_API_URL=http://localhost:8080/api
VITE_APP_NAME=Document AI Platform
```

## 🔐 Autenticação

- JWT no localStorage
- Refresh token automático
- Proteção de rotas
- Logout seguro

## 🎨 Estilo

- Tailwind CSS
- Responsive design
- Dark mode ready
- Componentes customizáveis

## 🚢 Build & Deploy

```bash
# Build
npm run build

# Preview
npm run preview

# Lint
npm run lint
```

## 📚 Docs

- [README.md](./README.md) - Documentação completa
- [Vite](https://vitejs.dev)
- [React](https://react.dev)
- [Tailwind](https://tailwindcss.com)
