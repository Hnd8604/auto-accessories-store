export const API_BASE_URL: string =
  import.meta.env.VITE_API_BASE_URL || "/api/v1";

export const WS_URL: string =
  import.meta.env.VITE_WS_URL ||
  (/^https?:\/\//.test(API_BASE_URL)
    ? `${API_BASE_URL}/ws`
    : `${window.location.origin}${API_BASE_URL}/ws`);

export const ACCESS_TOKEN_KEY = "access_token";
export const REFRESH_TOKEN_KEY = "refresh_token";

// Cảnh báo khi thiếu biến bắt buộc. Biến VITE_* được nhúng cứng lúc build, nên
// thiếu là thiếu vĩnh viễn trong bundle — báo sớm ở console còn hơn để người
// dùng bấm nút rồi nhận trang lỗi khó hiểu từ phía Google.
function required(name: string, value: string | undefined): string {
  if (!value) {
    console.error(
      `[config] Thiếu biến môi trường ${name}. Dev: thêm vào frontend/.env. ` +
        `Prod: đặt build-arg trong .github/workflows/cd.yml (Settings → Variables).`
    );
    return "";
  }
  return value;
}

// Google OAuth2 Configuration
//
// Client ID KHÔNG còn giá trị mặc định hardcode. Giá trị cũ nằm thẳng trong
// source nghĩa là dev và prod buộc phải dùng chung một OAuth client, và đổi
// client thì phải sửa code rồi build lại.
export const GOOGLE_CLIENT_ID: string = required(
  "VITE_GOOGLE_CLIENT_ID",
  import.meta.env.VITE_GOOGLE_CLIENT_ID
);
export const GOOGLE_REDIRECT_URI: string =
  import.meta.env.VITE_GOOGLE_REDIRECT_URI ||
  `${window.location.origin}/auth/google/callback`;

// Cloudinary: frontend không còn cấu hình gì cả. Upload đi qua backend
// (POST /images/upload) và backend trả về URL đầy đủ, nên trình duyệt không
// cần cloud name, api key hay secret nữa.

export type HttpHeaders = Record<string, string>;

export interface RequestOptions {
  method?: string;
  headers?: HttpHeaders;
  body?: any;
  signal?: AbortSignal;
}
