# Vite React Shadcn TS

A modern React + Vite + TypeScript template bootstrapped with shadcn/ui components and utilities.

## API Client

This project includes a lightweight typed API client under `src/api` generated from your OpenAPI surface.

- Configure base URL via `VITE_API_BASE_URL` (defaults to `http://localhost:8080/api`).
- Tokens are stored in `localStorage` under `access_token` and `refresh_token`.
- Automatic token refresh on 401 using `/auth/refresh`.

### Structure

- `src/api/config.ts`: base URL and token keys
- `src/api/http.ts`: auth-aware HTTP client with refresh queueing
- `src/api/auth.ts`: login/logout and exported `http` instance
- `src/api/types.ts`: core request/response types
- `src/api/endpoints/*`: resource-specific modules

### Usage

```ts
import { AuthService } from "./src/api/auth";
import { ProductsApi } from "./src/api/endpoints/products";

await AuthService.login({ username: "user", password: "pass" });
const products = await ProductsApi.getAll({ page: 0, size: 10 });
```

### Env

Create `.env.local`:

```
VITE_API_BASE_URL=http://localhost:8080/api
```

## 📂 Project Structure

```
src/
├── components/
│   ├── ui/              # Primitive components from shadcn/ui
│   ├── shared/          # Reusable business components
│   └── *.tsx            # Landing page components
├── features/            # Feature modules
├── pages/              # Page components
├── types/              # TypeScript types
├── services/           # HTTP clients
└── ...
```

## 📚 Documentation

### Architecture
- **[COMPONENT_ARCHITECTURE.md](./COMPONENT_ARCHITECTURE.md)** - 📐 Visual diagram & architecture overview
- **[UI_VS_SHARED.md](./UI_VS_SHARED.md)** - 🔍 Chi tiết sự khác biệt `ui/` vs `shared/`
- **[PROJECT_STRUCTURE.md](./PROJECT_STRUCTURE.md)** - 📁 Cấu trúc dự án chi tiết
- **[INDEX_PATTERN.md](./INDEX_PATTERN.md)** - 📦 Giải thích pattern `index.ts` (Barrel Exports)

### Development Guides
- **[SHARED_COMPONENTS.md](./SHARED_COMPONENTS.md)** - 🎯 Hướng dẫn sử dụng shared components
- **[REFACTORING_PLAN.md](./REFACTORING_PLAN.md)** - 🚀 Kế hoạch tối ưu components

## 🤔 Common Questions

**Q: Tại sao có 2 folder `ui/` và `shared/`?**
→ Đọc [UI_VS_SHARED.md](./UI_VS_SHARED.md)

**Q: Component mới nên đặt ở đâu?**
→ Đọc [COMPONENT_ARCHITECTURE.md](./COMPONENT_ARCHITECTURE.md)

**Q: Làm sao tái sử dụng code CRUD?**
→ Đọc [SHARED_COMPONENTS.md](./SHARED_COMPONENTS.md)

**Q: Tại sao mọi folder đều có `index.ts`?**
→ Đọc [INDEX_PATTERN.md](./INDEX_PATTERN.md)
