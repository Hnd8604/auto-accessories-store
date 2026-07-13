# Đánh giá & Việc cần sửa — Frontend (store-fe)

> Ngày review: 2026-06-04
> Stack: React 18 + TypeScript + Vite + shadcn/ui, kiến trúc feature-based.

## Tổng quan

Nền tảng tốt, kiến trúc feature-based rõ ràng:
`features/<domain>/{api, components, pages, types, hooks}` + barrel `index.ts`,
có `components/shared`, axios client với interceptor + token refresh + queue.

Dưới đây là các việc cần sửa, sắp theo độ ưu tiên.

---

## 🔴 Nghiêm trọng — sửa ngay

### 1. Lộ secret Cloudinary trong source code
**File:** `src/constants/config.ts`

```ts
CLOUDINARY_CONFIG = {
  apiKey: "991924558367536",
  apiSecret: "HR2QU9_z_Bzvk4lxaoAR_U69dTQ",  // ⚠️ bị bundle vào JS gửi về browser
}
```

- `apiSecret` nằm trong frontend → ai cũng đọc được trong bundle → có thể xoá/sửa tài sản Cloudinary.
- **Việc cần làm:**
  - [ ] Bỏ hoàn toàn `apiSecret` (và cân nhắc `apiKey`) khỏi frontend.
  - [ ] Chỉ upload bằng `uploadPreset` unsigned, hoặc ký chữ ký qua backend.
  - [ ] **Rotate secret này ngay** (coi như đã lộ).

### 2. Logic refresh token bị "chết"
**File:** `src/services/axios.ts` (~dòng 104)

```ts
private async refreshTokenIfNeeded() {
  const currentToken = this.getAccessToken();
  if (currentToken) return currentToken;   // ⚠️ token hết hạn vẫn nằm trong localStorage
  ...
}
```

- Khi gặp 401, token cũ **vẫn còn** trong localStorage (chỉ bị server từ chối).
  Hàm thấy `currentToken` tồn tại → trả về token cũ → retry bằng token hết hạn → fail tiếp.
  **=> Refresh thực tế không bao giờ chạy.**
- **Việc cần làm:**
  - [ ] Trong nhánh xử lý 401, clear access token (hoặc truyền token bị lỗi) trước khi gọi refresh.
  - [ ] Đảm bảo `refreshTokenIfNeeded` thực sự gọi `doRefresh` khi token hiện tại đã bị từ chối.
  - [ ] Test lại flow: access token hết hạn → tự refresh → retry request thành công.

---

## 🟡 Dọn dẹp & nhất quán

### 3. Abstraction `CRUDManagement` gần như không được dùng
Bộ shared rất tốt nhưng chưa áp dụng:

- `CRUDManagement` chỉ được dùng bởi `BrandManagement.new.tsx` — mà file này **không được import ở đâu** (code chết).
- `DataTable` và `PageHeader`: **0 lần** dùng trong features.
- Các file management tự viết lại `<Table>` thủ công:

| File | Số dòng | Dùng DataTable? |
|---|---|---|
| `features/products/components/ProductManagement.tsx` | 1078 | ❌ |
| `features/users/components/UserManagement.tsx` | 961 | ❌ |
| `features/orders/components/OrderManagement.tsx` | 934 | ❌ |
| `features/banners/components/BannerManagement.tsx` | 834 | ❌ |
| `features/categories/components/CategoryManagement.tsx` | 556 | ❌ |

- **Việc cần làm:**
  - [ ] Migrate dần các file management sang `CRUDManagement` / `DataTable` (tham khảo mẫu `BrandManagement.new.tsx`).
  - [ ] Kỳ vọng: các file 800–1000 dòng rút xuống còn ~200–300 dòng.
  - [ ] Làm từng feature một, test kỹ sau mỗi lần migrate.

### 4. File trùng / code chết
- [ ] `features/brands/components/BrandManagement.new.tsx` (228 dòng) không ai import.
  → Hoặc dùng nó thay cho `BrandManagement.tsx` (456 dòng), hoặc xoá. Không để cả hai.

### 5. Feature `inbox` & `notifications` không theo convention chung
- [ ] `inbox/components/InboxPage.tsx` → chuyển sang `inbox/pages/`.
- [ ] Thêm barrel `index.ts` cho `inbox/api`, `inbox/components`, `notifications/*`.
- [ ] Đổi tên `inbox/api/InboxApi.ts` → `inbox.ts` (lowercase, đồng bộ với `orders.ts`, `notifications.ts`).

### 6. Routes lặp `ProtectedRoute` 12+ lần
**File:** `src/routes/index.tsx`

13 route `/admin/*` đều bọc cùng `<ProtectedRoute requireAuth requireAdmin>` và render `<AdminPage />`.
Gom bằng layout route:

```tsx
<Route element={<ProtectedRoute requireAuth requireAdmin />}>
  <Route path="/admin" element={<AdminPage />} />
  <Route path="/admin/orders" element={<AdminPage />} />
  <Route path="/admin/posts/new" element={<PostEditorPage />} />
  {/* ... */}
</Route>
```

- **Việc cần làm:**
  - [ ] Đổi `ProtectedRoute` dùng `<Outlet />`.
  - [ ] Gom các route admin vào 1 layout route.

---

## 🟢 Nhỏ / chất lượng code

- [ ] **35 `console.log`** còn sót → bỏ hoặc thay bằng logger tắt được ở production.
- [ ] **42 chỗ dùng `any`** → khai báo `ImportMetaEnv` trong `vite-env.d.ts` để bỏ `(import.meta as any).env`.
- [ ] `AdminPage` suy ra tab bằng `path.includes(...)` — dễ vỡ.
      Kiểm tra: `/admin/post-categories` có bị nhận nhầm thành tab `posts` không (vì `includes("/posts")` đứng trước). Nên so khớp path chính xác.
- [ ] Chuỗi tiếng Việt hardcode khắp nơi (toast, label). Nếu sau này cần đa ngôn ngữ thì gom lại một chỗ.

---

## Thứ tự đề xuất

1. #1 Bỏ `apiSecret` Cloudinary + rotate (bảo mật)
2. #2 Sửa logic refresh token (bug chức năng)
3. #4 Xoá `BrandManagement.new.tsx`, rồi #3 migrate management → `CRUDManagement`/`DataTable`
4. #5 Chuẩn hoá feature `inbox` / `notifications`
5. #6 Gom route admin bằng layout route
6. #7 Dọn `console.log` + `any`
