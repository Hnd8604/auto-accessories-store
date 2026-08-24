# syntax=docker/dockerfile:1

# ==============================================================
# Frontend image — build Vite rồi serve bằng nginx.
# Nginx đảm nhiệm toàn bộ: TLS, serve file tĩnh, proxy /api/v1.
#
# Build tại thư mục frontend/:
#   docker build -t store-frontend \
#     --build-arg VITE_GOOGLE_CLIENT_ID=xxx.apps.googleusercontent.com .
# ==============================================================

# ---- Stage 1: Build ----
FROM node:22-alpine AS builder
WORKDIR /app

# Copy dependency files first to cache installs
COPY package.json package-lock.json ./
RUN npm ci

# Biến VITE_* được nhúng thẳng vào bundle LÚC BUILD -> phải truyền qua
# --build-arg. Đặt chúng lúc `docker run` sẽ không có tác dụng gì.
#
# KHÔNG có VITE_GOOGLE_REDIRECT_URI ở đây (trước là hardcode domain thật).
# src/constants/config.ts tự suy ra `${window.location.origin}/auth/google/callback`,
# nên bundle không cần biết domain nào cả -> cùng một image chạy được trên mọi
# domain, và đổi tên miền không phải build lại frontend.
ARG VITE_API_BASE_URL=/api/v1
ARG VITE_GOOGLE_CLIENT_ID=
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL \
    VITE_GOOGLE_CLIENT_ID=$VITE_GOOGLE_CLIENT_ID



# Copy source and build
COPY . .
RUN npm run build

# ---- Stage 2: Serve ----
FROM nginx:1.27-alpine

# Cài openssl (để tạo self-signed cert tạm khi chưa có cert thật)
# và curl (để tải ssl-dhparams.pem nếu cần).
# nginx:alpine chỉ có libssl, KHÔNG có CLI openssl → phải cài thêm.
RUN apk add --no-cache openssl curl

# nginx.conf chứa biến ${APP_DOMAIN} -> copy vào templates/, entrypoint
# sẽ envsubst rồi ghi ra conf.d/default.conf.
COPY nginx.conf /etc/nginx/templates/default.conf.template
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

COPY --from=builder /app/dist /usr/share/nginx/html

# Nginx cần chạy root để bind port 80/443. Worker processes vẫn chạy
# dưới user nginx (cấu hình mặc định của image nginx:alpine).

EXPOSE 80 443

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget -qO- http://127.0.0.1:80/healthz | grep -q ok || exit 1

ENTRYPOINT ["/docker-entrypoint.sh"]
