# Deploy lên VPS

Hướng dẫn đưa Auto Accessories Store lên một VPS trống (Ubuntu/Debian).
Image được build sẵn trên GitHub Actions và đẩy lên `ghcr.io`, nên trên
server chỉ `pull` rồi `up` — không build tại chỗ, deploy chỉ mất vài giây.

Kiến trúc khi chạy:

```
Internet :80/:443
      |
 [ frontend ]     <- nginx: TLS (Let's Encrypt) + file tĩnh + proxy /api/v1
      |
 [ backend ]      <- Spring Boot :8080
      |
 [ postgres ] [ redis ]
      
 [ certbot ]      <- tự động gia hạn chứng chỉ mỗi 12 giờ
```

Chỉ container `frontend` (nginx) mở cổng ra internet. Các service còn lại
nằm trong network nội bộ, không truy cập trực tiếp từ ngoài được.

---

## 1. Chuẩn bị trên VPS

Yêu cầu tối thiểu: **2 vCPU / 2GB RAM** (backend JVM + Postgres).

```bash
# Cài Docker + Compose plugin
curl -fsSL https://get.docker.com | sh

# Mở firewall. Port 80 BẮT BUỘC mở kể cả khi chỉ dùng HTTPS,
# vì Let's Encrypt cần nó để xác thực quyền sở hữu domain.
sudo ufw allow 22,80,443/tcp && sudo ufw enable
```

## 2. Trỏ domain

Tạo bản ghi **A** cho domain trỏ về IP VPS, và một bản ghi A cho `www` nếu
muốn dùng. Kiểm tra đã ăn chưa trước khi qua bước sau:

```bash
dig +short trungduongauto.store    # phải ra đúng IP VPS
```

> Nếu domain chưa trỏ đúng mà đã chạy certbot, sẽ xin chứng chỉ thất bại
> nhiều lần và dính **rate limit của Let's Encrypt (5 lần/domain/tuần)**.
> Luôn kiểm tra DNS trước.

## 3. Đưa file lên server

Server chỉ cần đúng **2 file**:

```bash
sudo mkdir -p /opt/auto_accessories_store && cd /opt/auto_accessories_store

# Lấy từ repo (hoặc scp lên)
curl -O https://raw.githubusercontent.com/hnd8604/auto_accessories_store/main/docker-compose.prod.yml
curl -o .env https://raw.githubusercontent.com/hnd8604/auto_accessories_store/main/.env.prod.example
```

## 4. Điền `.env`

```bash
nano .env
```

Bắt buộc phải sửa:

| Biến | Ghi chú |
|---|---|
| `APP_DOMAIN` | Domain đã trỏ ở bước 2 |
| `ACME_EMAIL` | Email nhận cảnh báo chứng chỉ sắp hết hạn |
| `POSTGRES_PASSWORD`, `REDIS_PASSWORD` | Đặt mật khẩu mạnh |
| `JWT_SIGNER_KEY` | Sinh bằng `openssl rand -base64 48` |
| `MAIL_*`, `CLOUDINARY_*`, `SEPAY_*`, `GOOGLE_*` | Giá trị thật của bạn |
| `GOOGLE_REDIRECT_URI` | Phải khớp **chính xác** với Authorized redirect URI khai trong Google Cloud Console |
| `ADMIN_PASSWORD` | **Bắt buộc ở lần deploy đầu tiên**, thiếu thì backend không khởi động |

`ADMIN_PASSWORD` chỉ dùng khi database còn rỗng. Từ lần deploy sau tài khoản
admin đã tồn tại nên biến này bị bỏ qua.

## 5. Khởi động + lấy chứng chỉ TLS

```bash
# Đăng nhập ghcr.io để pull image (dùng GitHub Personal Access Token
# có scope read:packages). Bỏ qua nếu package để public.
echo "<GITHUB_TOKEN>" | docker login ghcr.io -u <github-user> --password-stdin

docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
```

Lần đầu nginx khởi động với **self-signed cert tạm** (tự tạo bởi entrypoint).
Site truy cập được ngay qua HTTPS nhưng trình duyệt sẽ cảnh báo "không an toàn".

### Lấy chứng chỉ Let's Encrypt thật

```bash
# Xin chứng chỉ (thay domain và email cho đúng)
docker compose -f docker-compose.prod.yml run --rm certbot certonly \
  --webroot -w /var/www/certbot \
  -d trungduongauto.store -d www.trungduongauto.store \
  --email you@example.com --agree-tos --no-eff-email

# Reload nginx để dùng chứng chỉ mới
docker compose -f docker-compose.prod.yml exec frontend nginx -s reload
```

Từ đây certbot container sẽ **tự động gia hạn** mỗi 12 giờ. Sau khi gia hạn
thành công, chạy lệnh reload nginx để áp dụng cert mới:

```bash
# Thêm cronjob reload nginx sau khi certbot renew (chạy 1 lần trên server)
(crontab -l 2>/dev/null; echo "0 5 * * * docker exec store-frontend nginx -s reload") | crontab -
```

Kiểm tra:

```bash
docker compose -f docker-compose.prod.yml ps      # tất cả phải là healthy
curl https://<domain>/api/v1/actuator/health       # {"status":"UP"}
```

---

## 6. Deploy tự động qua GitHub Actions

Workflow [.github/workflows/cd.yml](../.github/workflows/cd.yml) build image
và deploy qua SSH. Khai báo trong **Settings → Secrets and variables → Actions**:

**Secrets**

| Tên | Giá trị |
|---|---|
| `SSH_HOST` | IP VPS |
| `SSH_USER` | User SSH |
| `SSH_KEY` | Private key (toàn bộ nội dung, kể cả dòng BEGIN/END) |
| `SSH_PORT` | Tuỳ chọn, mặc định 22 |

**Variables**

| Tên | Giá trị |
|---|---|
| `DEPLOY_PATH` | `/opt/auto_accessories_store` |
| `APP_DOMAIN` | Domain — dùng cho cả health check lẫn build frontend |
| `GOOGLE_CLIENT_ID` | Nhúng vào bundle frontend lúc build |
| `COMPANY_HOTLINE`, `COMPANY_EMAIL`, `COMPANY_ADDRESS`, `COMPANY_HOURS` | Thông tin hiển thị ở footer |

> Biến `VITE_*` bị **nhúng cứng vào bundle JavaScript lúc build**, không đọc
> lúc chạy container. Đổi domain hay hotline thì phải build lại image frontend,
> sửa `.env` trên server không có tác dụng.

Deploy chạy khi push tag `v*`, hoặc bấm **Run workflow** với `deploy = true`.
Chỉ push lên `main` thì chỉ build image, không deploy.

---

## 7. Vận hành

```bash
# Xem log
docker compose -f docker-compose.prod.yml logs -f backend
docker compose -f docker-compose.prod.yml logs -f frontend

# Cập nhật lên image mới nhất
docker compose -f docker-compose.prod.yml pull && \
docker compose -f docker-compose.prod.yml up -d && \
docker image prune -f

# Backup database (chạy định kỳ bằng cron)
docker exec postgres pg_dump -U store_user store | gzip > backup-$(date +%F).sql.gz

# Restore
gunzip -c backup-2026-07-31.sql.gz | docker exec -i postgres psql -U store_user store
```

### Xử lý sự cố

| Triệu chứng | Nguyên nhân thường gặp |
|---|---|
| Certbot không xin được chứng chỉ | DNS chưa trỏ đúng, hoặc port 80 bị firewall chặn |
| Trình duyệt báo cert không an toàn | Chưa chạy `certbot certonly` (vẫn dùng self-signed tạm) |
| `backend` unhealthy | Xem `logs backend`. Lần đầu thường do thiếu `ADMIN_PASSWORD` |
| Backend không khởi động, log báo Flyway | Schema lệch. Xem [SchemaMigrationTest](../backend/src/test/java/app/store/SchemaMigrationTest.java) |
| Đăng nhập Google lỗi `redirect_uri_mismatch` | `GOOGLE_REDIRECT_URI` không khớp Google Cloud Console, hoặc frontend build với domain khác |
| Upload ảnh lỗi 413 | Ảnh vượt 10MB (giới hạn ở cả nginx lẫn Spring) |

> **Không bao giờ chạy `docker compose down -v` trên server** — cờ `-v` xoá
> luôn volume, mất sạch database và chứng chỉ đã cấp.
