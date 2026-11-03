import { lazy, Suspense } from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import ProtectedRoute from "@/features/auth/components/ProtectedRoute";

const HomePage = lazy(() => import("@/app/pages/HomePage"));
const NotFoundPage = lazy(() => import("@/app/pages/NotFoundPage"));
const ProductCatalogPage = lazy(() => import("@/features/products/pages/ProductCatalogPage"));
const ProductDetailPage = lazy(() => import("@/features/products/pages/ProductDetailPage"));
const CartPage = lazy(() => import("@/features/cart/pages/CartPage"));
const MyOrdersPage = lazy(() => import("@/features/orders/pages/MyOrdersPage"));
const BlogPage = lazy(() => import("@/features/posts/pages/BlogPage"));
const PostDetailPage = lazy(() => import("@/features/posts/pages/PostDetailPage"));
const ServiceDetailPage = lazy(() => import("@/features/services/pages/ServiceDetailPage"));
const AuthPage = lazy(() => import("@/features/auth/pages/AuthPage"));
const GoogleCallback = lazy(() => import("@/features/auth/pages/GoogleCallback"));
const AdminPage = lazy(() => import("@/features/admin/pages/AdminPage"));
const PostEditorPage = lazy(() => import("@/features/posts/pages/PostEditorPage"));

export function AppRoutes() {
  return (
    <Suspense fallback={<div role="status" className="min-h-screen flex items-center justify-center">Đang tải...</div>}>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/products" element={<ProductCatalogPage />} />
        <Route path="/products/:slug" element={<ProductDetailPage />} />
        <Route path="/cart" element={<CartPage />} />
        <Route path="/order" element={<Navigate to="/cart" replace />} />
        <Route path="/blog" element={<BlogPage />} />
        <Route path="/blog/:slug" element={<PostDetailPage />} />
        <Route path="/services/:slug" element={<ServiceDetailPage />} />
        <Route path="/auth" element={<AuthPage />} />
        <Route path="/auth/google/callback" element={<GoogleCallback />} />

        <Route element={<ProtectedRoute requireAuth />}>
          <Route path="/my-orders" element={<MyOrdersPage />} />
        </Route>

        <Route element={<ProtectedRoute requireAuth requireAdmin />}>
          <Route path="/admin" element={<AdminPage />} />
          <Route path="/admin/:section" element={<AdminPage />} />
          <Route path="/admin/posts/new" element={<PostEditorPage />} />
          <Route path="/admin/posts/edit/:id" element={<PostEditorPage />} />
        </Route>

        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </Suspense>
  );
}
