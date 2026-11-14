# Stage 1: Build Frontend Assets
FROM node:22-alpine AS builder
WORKDIR /app

ARG VITE_API_BASE_URL
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

# Stage 2: Serve with lightweight Nginx web server
FROM nginx:1.27-alpine
COPY --from=builder /app/dist /usr/share/nginx/html

# Custom nginx configuration for SPA routing on port 80
RUN printf 'server {\n\
    listen 80;\n\
    listen [::]:80;\n\
    server_name _;\n\
\n\
    location / {\n\
        root /usr/share/nginx/html;\n\
        index index.html;\n\
        try_files $uri $uri/ /index.html;\n\
    }\n\
}\n' > /etc/nginx/conf.d/default.conf

# Inject runtime VITE_API_BASE_URL into config.js via official Nginx entrypoint hook
RUN printf '#!/bin/sh\n\
if [ -n "$VITE_API_BASE_URL" ] || [ -n "$API_BASE_URL" ]; then\n\
  TARGET_URL="${VITE_API_BASE_URL:-$API_BASE_URL}"\n\
  echo "window.__BORROWHUB_CONFIG__ = { API_BASE_URL: \\x27$TARGET_URL\\x27 };" > /usr/share/nginx/html/config.js\n\
fi\n' > /docker-entrypoint.d/99-borrowhub-config.sh && \
    chmod +x /docker-entrypoint.d/99-borrowhub-config.sh

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
