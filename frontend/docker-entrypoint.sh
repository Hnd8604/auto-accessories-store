#!/bin/sh
# ==============================================================
# Entrypoint cho frontend container (production).
#
# 1. Thay thế ${APP_DOMAIN} trong nginx template bằng giá trị thật
# 2. Nếu chưa có chứng chỉ SSL (lần deploy đầu), tạo self-signed tạm
#    để nginx khởi động được → certbot sẽ thay bằng cert thật sau đó
# 3. Khởi động nginx
# ==============================================================
set -e

DOMAIN="${APP_DOMAIN:?APP_DOMAIN environment variable is required}"
CERT_DIR="/etc/letsencrypt/live/$DOMAIN"

# --- Thay biến môi trường vào nginx config ---
envsubst '${APP_DOMAIN}' < /etc/nginx/templates/default.conf.template > /etc/nginx/conf.d/default.conf

# --- Tạo self-signed cert tạm nếu chưa có cert thật ---
if [ ! -f "$CERT_DIR/fullchain.pem" ]; then
    echo "==> Chưa có chứng chỉ cho $DOMAIN, tạo self-signed tạm..."
    mkdir -p "$CERT_DIR"
    openssl req -x509 -nodes -days 1 -newkey rsa:2048 \
        -keyout "$CERT_DIR/privkey.pem" \
        -out "$CERT_DIR/fullchain.pem" \
        -subj "/CN=$DOMAIN" 2>/dev/null
    echo "==> Self-signed cert tạm đã tạo. Chạy certbot để lấy cert thật."
fi

# --- Tạo file SSL config nếu chưa có (certbot thường tạo sẵn) ---
SSL_CONF="/etc/letsencrypt/options-ssl-nginx.conf"
if [ ! -f "$SSL_CONF" ]; then
    cat > "$SSL_CONF" <<'EOF'
ssl_session_cache shared:le_nginx_SSL:10m;
ssl_session_timeout 1440m;
ssl_session_tickets off;
ssl_protocols TLSv1.2 TLSv1.3;
ssl_prefer_server_ciphers off;
ssl_ciphers "ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-CHACHA20-POLY1305:ECDHE-RSA-CHACHA20-POLY1305:DHE-RSA-AES128-GCM-SHA256:DHE-RSA-AES256-GCM-SHA384";
EOF
fi

DH_PARAM="/etc/letsencrypt/ssl-dhparams.pem"

# Cả hai URL trả về CÙNG một group ffdhe2048 (RFC 7919), 424 byte.
# Certbot đã chuyển sang layout src/ nên đường dẫn cũ
# (certbot/certbot/ssl-dhparams.pem) giờ trả 404 — giữ URL thứ hai làm dự phòng
# cho lần đổi cấu trúc tiếp theo.
DH_URLS="https://raw.githubusercontent.com/certbot/certbot/main/certbot/src/certbot/ssl-dhparams.pem
https://ssl-config.mozilla.org/ffdhe2048.txt"

if [ ! -f "$DH_PARAM" ]; then
    # Tải vào file tạm rồi mới mv: KHÔNG BAO GIỜ để nginx thấy một $DH_PARAM
    # dở dang hoặc chứa rác. Hai cái bẫy đã từng làm nginx không load được:
    #   • thiếu -f  → HTTP 404 vẫn exit 0, body "404: Not Found" ghi vào file
    #   • không validate → file hỏng chỉ lộ ra lúc nginx load, quá muộn
    DH_TMP="$DH_PARAM.tmp.$$"
    for url in $DH_URLS; do
        if curl -fsSL "$url" -o "$DH_TMP" \
           && openssl dhparam -in "$DH_TMP" -check -noout >/dev/null 2>&1; then
            mv "$DH_TMP" "$DH_PARAM"
            echo "==> Đã tải ssl-dhparams.pem từ $url"
            break
        fi
        echo "==> Nguồn không dùng được: $url"
        rm -f "$DH_TMP"
    done

    if [ ! -f "$DH_PARAM" ]; then
        echo "==> Không tải được ssl-dhparams.pem hợp lệ, tự generate (mất ~1-2 phút)..."
        openssl dhparam -out "$DH_TMP" 2048 2>/dev/null
        mv "$DH_TMP" "$DH_PARAM"
    fi
fi

echo "==> Khởi động nginx cho $DOMAIN"
exec nginx -g "daemon off;"
