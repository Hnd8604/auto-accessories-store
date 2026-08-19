import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react-swc";
import path from "path";
import { componentTagger } from "lovable-tagger";

const rootDir = path.resolve(__dirname, "..");

/**
 * Nạp biến VITE_* từ .env.dev ở THƯ MỤC GỐC — cùng file mà docker-compose.yml
 * và backend đang dùng. Trước đây frontend có .env riêng, khiến GOOGLE_CLIENT_ID
 * và CLOUDINARY_* tồn tại ở hai nơi và lệch nhau.
 *
 * Vì sao không dùng `envDir` cho gọn: Vite chỉ tìm `.env.<mode>`, mà mode mặc
 * định là "development" -> nó sẽ bỏ qua `.env.dev`. Còn đổi mode thành "dev"
 * thì điều kiện `mode === "development"` bên dưới hỏng, componentTagger tắt.
 *
 * loadEnv(..., "VITE_") CHỈ đọc biến có tiền tố VITE_, nên các secret không
 * tiền tố trong cùng file (POSTGRES_*, JWT_SIGNER_KEY, ...) không bao giờ chạm
 * tới bundle.
 *
 * Ghi vào process.env thay vì `define` để đi đúng đường Vite vẫn dùng cho
 * build-arg của Docker — nhờ vậy hai môi trường chạy chung một cơ chế.
 * Không ghi đè biến đã có sẵn: lúc build image, build-arg phải thắng.
 */
for (const [key, value] of Object.entries(loadEnv("dev", rootDir, "VITE_"))) {
  if (process.env[key] === undefined) process.env[key] = value;
}

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => ({
  server: {
    host: "::",
    port: 3000,
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
        ws: true,
      },
    },
  },
  plugins: [react(), mode === "development" && componentTagger()].filter(
    Boolean
  ),
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  define: {
    global: "globalThis",
  },
  optimizeDeps: {
    esbuildOptions: {
      define: {
        global: "globalThis",
      },
    },
  },
}));
