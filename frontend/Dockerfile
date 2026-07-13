# ---- Stage 1: Build ----
FROM node:22-alpine AS builder
WORKDIR /app

# Copy dependency files first to cache installs
COPY package.json package-lock.json ./
RUN npm ci

# Copy source and build
COPY . .
RUN VITE_API_BASE_URL=/api/v1 \
    VITE_GOOGLE_REDIRECT_URI=https://trungduongauto.store/auth/google/callback \
    npm run build

# ---- Stage 2: Serve ----
FROM alpine:3.20

# Install nginx
RUN apk add --no-cache nginx

# Remove default nginx content
RUN rm -rf /usr/share/nginx/html/*

# Copy built assets
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy nginx config (Alpine uses http.d instead of conf.d)
COPY nginx.conf /etc/nginx/http.d/default.conf

# Setup required directories and permissions for non-root user
RUN mkdir -p /var/log/nginx /var/lib/nginx /run/nginx && \
    chown -R nginx:nginx /usr/share/nginx/html /var/log/nginx /var/lib/nginx /run/nginx

USER nginx

EXPOSE 3000
CMD ["nginx", "-g", "daemon off;"]
