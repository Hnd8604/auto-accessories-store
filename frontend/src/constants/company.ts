// Thông tin liên hệ của cửa hàng — nội dung tĩnh, KHÔNG phải biến môi trường.
//
// Trước đây 4 giá trị này đi qua VITE_COMPANY_* : .env → build-arg Dockerfile →
// GitHub Variables. Chúng không phải bí mật (in ngay trên footer cho khách xem)
// và không đổi theo môi trường (địa chỉ ở dev với prod là cùng một địa chỉ),
// nên toàn bộ đường ống đó chỉ tạo thêm chỗ để quên set biến — mà quên thì
// header render ra đúng chữ "undefined".
//
// Sửa thông tin: đổi thẳng ở đây rồi build lại (VITE_* cũng phải build lại y
// hệt, nên không mất gì).
//
// Khi nào cần chuyển tiếp: nếu chủ shop muốn tự đổi hotline/địa chỉ mà không
// cần deploy, lời giải là API backend (bảng settings + trang admin) chứ không
// phải quay lại biến môi trường.
export const COMPANY = {
  hotline: "0123 456 789",
  email: "info@autolux.vn",
  address: "123 Đường ABC, Quận 1, TP.HCM",
  hours: "8:00 - 18:00 (Thứ 2 - Chủ Nhật)",
} as const;

/** Hotline đã bỏ khoảng trắng, dùng cho `href="tel:..."`. */
export const COMPANY_HOTLINE_TEL = COMPANY.hotline.replace(/\s/g, "");
