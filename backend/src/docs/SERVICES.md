# Dịch Vụ Chuyên Nghiệp — Tài liệu Kỹ thuật

Tài liệu này mô tả tính năng **Dịch vụ chuyên nghiệp** của hệ thống AutoLux Store (Độ Đèn, Vệ Sinh Nội Thất, Dán Phim, Bọc Ghế Da, v.v.). Admin quản lý dịch vụ và **upload ảnh công trình thực tế** (lưu trên Cloudinary); khách xem danh sách + thư viện ảnh rồi liên hệ tư vấn.

---

## Mục lục

1. [Tổng quan](#1-tổng-quan)
2. [Kiến trúc](#2-kiến-trúc)
3. [Backend](#3-backend)
   - [Entities & Database](#31-entities--database)
   - [DTO](#32-dto)
   - [Repository](#33-repository)
   - [Service](#34-service)
   - [API Endpoints](#35-api-endpoints)
   - [Phân quyền & Security](#36-phân-quyền--security)
4. [Frontend](#4-frontend)
5. [Luồng dữ liệu](#5-luồng-dữ-liệu)
6. [Ghi chú kỹ thuật](#6-ghi-chú-kỹ-thuật)
7. [Hướng dẫn kiểm tra](#7-hướng-dẫn-kiểm-tra)
8. [Giới hạn & hướng mở rộng](#8-giới-hạn--hướng-mở-rộng)

---

## 1. Tổng quan

### Vấn đề
Cửa hàng cung cấp nhiều dịch vụ lắp đặt/độ xe, mỗi dịch vụ cần mô tả chi tiết và **ảnh công trình thật** để khách tin tưởng. Hard-code danh sách dịch vụ trên frontend khó cập nhật và không có thư viện ảnh.

### Giải pháp
Hệ thống CRUD dịch vụ hoàn chỉnh từ backend đến UI:
- Admin thêm/sửa/xoá dịch vụ tuỳ ý, mỗi dịch vụ có nhiều **tính năng (features)** và nhiều **ảnh**.
- Ảnh upload lên **Cloudinary**, chọn được **ảnh đại diện** (primary).
- Khách xem grid dịch vụ ở trang chủ và trang chi tiết có **carousel + lightbox** ảnh.

### Kết quả
- Admin tự quản lý nội dung dịch vụ, không cần deploy lại.
- Trang công khai hiển thị dữ liệu động kèm thư viện ảnh thực tế.
- Mỗi dịch vụ có URL thân thiện qua **slug**.

---

## 2. Kiến trúc

```
Khách (public)                 Backend (Spring Boot)              Admin
──────────────                 ─────────────────────             ─────
Services.tsx (trang chủ)
  └ GET /services ───────────► ServiceController
                                └ ProfessionalServiceService
                                   └ ServiceRepository (JOIN FETCH images) ──► MySQL
                                                                                services
ServiceDetailPage (/services/:slug)                                            service_image
  ├ GET /services/slug/{slug} ─► ServiceController
  └ GET /service-images/services/{id} ─► ServiceImageController
                                                                   ServiceManagement.tsx
                              ServiceController  ◄── POST/PUT/DELETE /services
                              ServiceImageController ◄── upload/set-primary /service-images
                                └ ServiceImageService
                                   └ CloudinaryService.uploadImage("store/services") ──► Cloudinary
```

**Công nghệ:** Spring Data JPA + MySQL, MapStruct mapper, Cloudinary (lưu ảnh), `features` lưu JSON trong cột TEXT.

---

## 3. Backend

### 3.1 Entities & Database

#### Bảng `professional_service`

File: `src/main/java/app/store/entity/ProfessionalService.java`

| Cột | Kiểu | Mô tả |
|-----|------|-------|
| `id` | BIGINT PK | Tự tăng (`BaseEntityLong`) |
| `name` | VARCHAR | Tên dịch vụ |
| `short_description` | TEXT | Mô tả ngắn (hiển thị ở card) |
| `full_description` | TEXT | Mô tả đầy đủ (trang chi tiết) |
| `features` | TEXT | Danh sách tính năng — **lưu dạng JSON string** |
| `slug` | VARCHAR | Định danh URL thân thiện, sinh tự động |
| `display_order` | INT | Thứ tự hiển thị |
| *(quan hệ)* | — | `@OneToMany` `images` (cascade ALL, orphanRemoval) |

#### Bảng `service_image`

File: `src/main/java/app/store/entity/ServiceImage.java`

| Cột | Kiểu | Mô tả |
|-----|------|-------|
| `id` | BIGINT PK | Tự tăng |
| `service_id` | FK | `@ManyToOne` đến `professional_service` |
| `image_url` | VARCHAR | URL Cloudinary |
| `alt_text` | VARCHAR | Caption / mô tả ảnh |
| `is_primary` | BOOLEAN | Ảnh đại diện |
| `sort_order` | INT | Thứ tự sắp xếp |

### 3.2 DTO

| DTO | Trường |
|-----|--------|
| `ServiceRequest` | `name`, `shortDescription`, `fullDescription`, `features` (`List<String>`), `displayOrder` |
| `ServiceResponse` | `id`, `name`, `shortDescription`, `fullDescription`, `features` (`List<String>`), `slug`, `displayOrder`, **`primaryImageUrl`** |
| `ServiceImageRequest` | `serviceId`, `altText`, `isPrimary`, `sortOrder` |
| `ServiceImageUpdateRequest` | metadata ảnh (altText, isPrimary, sortOrder) |
| `ServiceImageResponse` | thông tin ảnh trả về |

> `features` đi vào/ra dưới dạng `List<String>`; service layer tự **serialize/deserialize** sang JSON khi đọc/ghi DB. `primaryImageUrl` được **tính ở service layer**, không có cột DB riêng.

### 3.3 Repository

File `ServiceRepository.java`:

| Method | Mô tả |
|--------|-------|
| `findAllWithImages()` | `LEFT JOIN FETCH s.images ORDER BY displayOrder ASC NULLS LAST, createdAt ASC` — tránh **N+1 query** |
| `findBySlug(slug)` | Lấy dịch vụ theo slug |
| `existsBySlug(slug)` | Kiểm tra slug trùng (phục vụ sinh slug duy nhất) |

File `ServiceImageRepository.java`:

| Method | Mô tả |
|--------|-------|
| `findByServiceId(serviceId)` | Ảnh của 1 dịch vụ, sắp theo `sortOrder` |
| `resetAllPrimaryImagesForService(serviceId)` | `@Modifying`: đặt `is_primary=false` cho mọi ảnh của dịch vụ |
| `setNewPrimaryImage(imageId, serviceId)` | `@Modifying`: đặt `is_primary=true` cho 1 ảnh |

### 3.4 Service

#### `ProfessionalServiceService.java`

| Method | Auth | Mô tả |
|--------|------|-------|
| `getAllServices()` | Public | Lấy tất cả (JOIN FETCH ảnh), tính `primaryImageUrl`, parse features |
| `getServiceById(id)` | Public | Theo id |
| `getServiceBySlug(slug)` | Public | Theo slug (cho routing FE) |
| `createService(request)` | `SERVICE_CREATE` | Tạo mới + sinh slug duy nhất từ tên |
| `updateService(id, request)` | `SERVICE_UPDATE` | Cập nhật; **đổi tên thì sinh lại slug** |
| `deleteService(id)` | `SERVICE_DELETE` | Xoá (cascade ảnh) |

**Tính `primaryImageUrl`** (logic dùng chung): lấy ảnh có `isPrimary=true`; nếu không có → fallback **ảnh đầu tiên**.

**Slug:** `slugUtil.toSlug(name)` → `createUniqueSlug(base, existsBySlug)`. Khi update, chỉ sinh lại nếu `name` đổi và loại trừ slug hiện tại khỏi kiểm tra trùng.

#### `ServiceImageService.java`

| Method | Auth | Mô tả |
|--------|------|-------|
| `createServiceImage(file, request)` | `SERVICE_IMAGE_CREATE` | Upload ảnh lên Cloudinary folder `store/services`, lưu URL |
| `getImagesByServiceId(serviceId)` | Public | Danh sách ảnh của dịch vụ |
| `updateServiceImage(imageId, request)` | `SERVICE_IMAGE_UPDATE` | Cập nhật metadata ảnh |
| `deleteServiceImage(imageId)` | `SERVICE_IMAGE_DELETE` | Xoá ảnh trên Cloudinary **và** DB |
| `setPrimaryImage(serviceId, imageId)` | `SERVICE_IMAGE_SET_PRIMARY` | Reset primary cũ → set primary mới (kiểm tra ảnh thuộc đúng dịch vụ) |

### 3.5 API Endpoints

**Dịch vụ** — `ServiceController.java`, base path `/services`:

| Method | URL | Auth | Mô tả |
|--------|-----|------|-------|
| `GET` | `/services` | Public | Tất cả, sắp theo `displayOrder` |
| `GET` | `/services/{id}` | Public | Theo id |
| `GET` | `/services/slug/{slug}` | Public | Theo slug |
| `POST` | `/services` | `SERVICE_CREATE` | Tạo mới (`ServiceRequest`) |
| `PUT` | `/services/{id}` | `SERVICE_UPDATE` | Cập nhật |
| `DELETE` | `/services/{id}` | `SERVICE_DELETE` | Xoá |

**Ảnh dịch vụ** — `ServiceImageController.java`, base path `/service-images`:

| Method | URL | Auth | Mô tả |
|--------|-----|------|-------|
| `GET` | `/service-images/services/{serviceId}` | Public | Ảnh theo dịch vụ |
| `POST` | `/service-images` (multipart) | `SERVICE_IMAGE_CREATE` | Upload: params `file`, `serviceId`, `isPrimary` (default false) |
| `PUT` | `/service-images/{imageId}` | `SERVICE_IMAGE_UPDATE` | Cập nhật metadata |
| `DELETE` | `/service-images/{imageId}` | `SERVICE_IMAGE_DELETE` | Xoá ảnh |
| `POST` | `/service-images/services/{serviceId}/images/{imageId}/set-primary` | `SERVICE_IMAGE_SET_PRIMARY` | Đặt ảnh đại diện |

**Response mẫu `GET /services`:**
```json
{
  "code": 1000,
  "result": [
    {
      "id": 1,
      "name": "Độ Đèn Bi LED",
      "shortDescription": "Tăng sáng, an toàn khi lái đêm",
      "fullDescription": "...",
      "features": ["Bảo hành 12 tháng", "Lắp đặt tận nơi"],
      "slug": "do-den-bi-led",
      "displayOrder": 1,
      "primaryImageUrl": "https://res.cloudinary.com/.../store/services/abc.jpg"
    }
  ]
}
```

### 3.6 Phân quyền & Security

- **`SecurityConfig.java`**: `GET /services/**` và `GET /service-images/**` là **public**; các thao tác ghi yêu cầu authentication + authority.
- **`ErrorCode.java`**: `SERVICE_NOT_EXISTED` (7001), `SERVICE_IMAGE_NOT_EXISTED` (7002).
- **`SeedRolePerms.java`**: 7 permission cấp cho role `ADMIN` — `SERVICE_CREATE/UPDATE/DELETE`, `SERVICE_IMAGE_CREATE/UPDATE/DELETE/SET_PRIMARY` (kiểm soát qua `@PreAuthorize`).

---

## 4. Frontend

### Cấu trúc thư mục (`store-fe/src/features/services/`)

```
features/services/
├── api/
│   ├── services.ts          # getAll, getById, getBySlug, create, update, delete
│   └── serviceImages.ts     # getByServiceId, create (multipart), update, delete, setPrimary
├── components/
│   ├── ServiceManagement.tsx       # Admin: bảng CRUD + form tạo/sửa
│   └── ServiceImageManagement.tsx  # Admin: dialog upload/quản lý ảnh
├── pages/
│   └── ServiceDetailPage.tsx       # Trang chi tiết công khai
└── types/
    ├── service.ts
    └── service-image.ts
```

| File sửa đổi | Thay đổi |
|--------------|---------|
| `src/components/Services.tsx` | Hiển thị dữ liệu động từ API, skeleton loading, grid card có ảnh đại diện, nút "Xem Chi Tiết" |
| `src/pages/AdminPage.tsx` | Thêm sidebar "Dịch vụ" + tab `ServiceManagement` |
| `src/routes/index.tsx` | Route `/services/:slug` (public) và `/admin/services` (admin) |

### Giao diện

- **Trang chủ (`Services.tsx`):** grid responsive 1→2→3 cột; mỗi card có ảnh đại diện + tên + mô tả ngắn + nút "Xem Chi Tiết"; skeleton khi tải; placeholder icon nếu chưa có ảnh.
- **Trang chi tiết (`/services/:slug`):** Embla Carousel trượt ảnh (prev/next + dot), caption dưới ảnh, click → **lightbox fullscreen**; cột phải hiển thị mô tả đầy đủ, danh sách features, nút "Liên Hệ Tư Vấn".
- **Admin (`/admin/services`):** bảng danh sách (thumbnail, tên, mô tả, thứ tự, actions); form tạo/sửa với features thêm/xoá động; dialog "Quản lý ảnh" (upload, set-primary, xoá, sửa caption).

---

## 5. Luồng dữ liệu

```
Admin upload ảnh
  → POST /service-images (multipart: file, serviceId, isPrimary)
  → ServiceImageService → CloudinaryService.uploadImage("store/services")
  → lưu image_url vào bảng service_image

Trang chủ
  → GET /services
  → ProfessionalServiceService.getAllServices() (JOIN FETCH images)
  → tính primaryImageUrl, parse features → trả List<ServiceResponse>
  → FE render grid card

Trang chi tiết /services/:slug
  → GET /services/slug/{slug}  +  GET /service-images/services/{id}
  → FE sort ảnh (primary trước) → đưa vào Embla carousel
```

---

## 6. Ghi chú kỹ thuật

- **features** lưu JSON string trong cột TEXT; service layer serialize/deserialize bằng Jackson `ObjectMapper` (lỗi parse → trả list rỗng, có log warn).
- **slug** sinh tự động từ tên qua `SlugUtil.createUniqueSlug()`; cập nhật lại khi đổi tên.
- **N+1 queries** tránh bằng `LEFT JOIN FETCH` trong `findAllWithImages()`.
- **primaryImageUrl** tính ở service layer: ảnh `isPrimary=true`, fallback ảnh đầu tiên.
- **set-primary** dùng 2 query `@Modifying` (reset tất cả → set 1) trong cùng `@Transactional`.
- **Ảnh** lưu Cloudinary folder `store/services`; xoá dịch vụ → cascade xoá bản ghi ảnh (lưu ý phần dọn file trên Cloudinary — xem mục 8).

---

## 7. Hướng dẫn kiểm tra

### Khởi động
```bash
cd auto_accessories_store-be && mvn spring-boot:run   # http://localhost:8080/api/v1
cd store-fe && npm run dev                            # http://localhost:3000
```

### Kiểm tra end-to-end
1. Đăng nhập admin → `/admin/services`.
2. Tạo dịch vụ mới (tên, mô tả, vài features) → kiểm tra slug tự sinh.
3. Mở dialog "Quản lý ảnh" → upload vài ảnh → set một ảnh làm primary.
4. Mở trang chủ → thấy card dịch vụ với ảnh đại diện.
5. Bấm "Xem Chi Tiết" → `/services/:slug` → carousel + lightbox hoạt động.

### Kiểm tra API nhanh
```bash
curl http://localhost:8080/api/v1/services
curl http://localhost:8080/api/v1/services/slug/do-den-bi-led
curl http://localhost:8080/api/v1/service-images/services/1
```

### Kiểm tra DB
```sql
SELECT id, name, slug, display_order FROM professional_service ORDER BY display_order;
SELECT id, service_id, is_primary, sort_order FROM service_image WHERE service_id = 1;
```

---

## 8. Giới hạn & hướng mở rộng

- **Xoá ảnh trên Cloudinary khi xoá dịch vụ:** xoá dịch vụ cascade xoá bản ghi `service_image` trong DB, nhưng **file ảnh trên Cloudinary có thể còn sót** (chỉ `deleteServiceImage` mới gọi `cloudinaryService.deleteImage`). Nên dọn ảnh Cloudinary trong luồng xoá dịch vụ.
- **Chưa có phân trang:** `GET /services` trả toàn bộ; nếu số lượng lớn nên thêm phân trang.
- **Không có trường giá:** phiên bản hiện tại đã bỏ `price`/`priceLabel`; nếu cần hiển thị giá phải bổ sung lại entity/DTO.
- **set-primary chạy 2 UPDATE:** chấp nhận được ở quy mô nhỏ; có thể gộp logic nếu cần tối ưu.

---

*Tài liệu cập nhật lần cuối: 2026-06-05*
