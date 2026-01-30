# Build stage
FROM node:18-alpine AS builder

WORKDIR /app

# Copiar package files (contexto é a raiz do projeto)
COPY frontend/package*.json ./

# Instalar dependências (use npm install se não houver package-lock.json)
RUN npm install

# Copiar código fonte
COPY frontend/ .

# Build
RUN npm run build

# Runtime stage
FROM nginx:alpine

# Copiar configuração do nginx
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf

# Copiar build
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
