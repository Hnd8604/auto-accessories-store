# Cấu trúc frontend

Ứng dụng dùng React, TypeScript, Vite, React Query và React Context. Mã được chia theo tính năng để mỗi phần quản lý giao diện, API, hook và kiểu dữ liệu của chính nó.

```text
src/
  App.tsx                   # Ghép provider và router
  app/
    components/             # Layout, header, footer và phần dùng trên toàn cửa hàng
    pages/                  # Trang chủ và trang 404
  components/ui/            # Thành phần giao diện cơ bản
  features/<feature>/
    api/                    # Lời gọi API thuộc tính năng
    components/             # Giao diện thuộc tính năng
    context/                # Provider/context khi cần chia sẻ trạng thái
    hooks/                  # Hook thuộc tính năng
    pages/                  # Trang thuộc tính năng
    types/                  # Kiểu dữ liệu thuộc tính năng
  routes/                   # Khai báo đường dẫn và bảo vệ quyền truy cập
  services/                 # HTTP client và dịch vụ dùng chung
  types/common/             # Kiểu phản hồi API và phân trang
  utils/                    # Hàm tiện ích không phụ thuộc tính năng
```

## Quy tắc phụ thuộc

- Component và hook gọi API qua `features/<feature>/api`, không gọi `axios` trực tiếp.
- API xác thực dùng `services/http.ts`; API giỏ hàng phiên dùng `simpleHttp` trong `services/axios.ts`.
- React Query giữ dữ liệu từ máy chủ. Context chỉ cung cấp phiên đăng nhập, giỏ hàng, thông báo và thao tác cần chia sẻ. Không sao chép kết quả query vào local state chỉ để hiển thị.
- Provider, định nghĩa context và hook đọc context ở các file riêng. Kiểu dữ liệu nghiệp vụ thuộc tính năng sở hữu chúng; kiểu phân trang và phản hồi API nằm trong `types/common`.
- `services/`, `types/common/` và `components/ui/` không nhập API hoặc state từ một tính năng cụ thể. `app/components/` được phép ghép nhiều tính năng.
- Phần giao diện lặp lại hoặc có trách nhiệm riêng được tách thành component; logic dữ liệu dùng lại nằm trong hook của tính năng. Ví dụ: `ProductCard`, `CategoryProductsRow`, `FilterContent`, `useProductPrimaryImages` và `useProductImageUploads`.
- Trang được tải theo đường dẫn bằng `React.lazy`; từng mục quản trị cũng được tải khi mở. Route quản trị dùng chung phần kiểm tra quyền.
- Trang cửa hàng dùng `StoreLayout`; danh mục sản phẩm ở `/products`. Luồng đặt hàng dùng giỏ hàng và `Checkout`; `/order` cũ chuyển về `/cart`.

## Kiểm tra

Trong thư mục `frontend/`, chạy `npm run typecheck`, `npm run lint` và `npm run build` trước khi bàn giao thay đổi lớn.
