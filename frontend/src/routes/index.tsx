import { Routes, Route } from "react-router-dom";
import ProtectedRoute from "@/features/auth/components/ProtectedRoute";

// Pages
import Index from "@/pages/Index";
import BlogPage from "@/pages/BlogPage";
import AdminPage from "@/pages/AdminPage";
import CartPage from "@/pages/CartPage";
import NotFound from "@/pages/NotFound";

// Feature Pages
import AuthPage from "@/features/auth/pages/AuthPage";
import GoogleCallback from "@/features/auth/pages/GoogleCallback";
import ProductDetailPage from "@/features/products/pages/ProductDetailPage";
import OrderPage from "@/features/orders/pages/OrderPage";
import MyOrdersPage from "@/features/orders/pages/MyOrdersPage";
import PostDetailPage from "@/features/posts/pages/PostDetailPage";
import PostEditorPage from "@/features/posts/pages/PostEditorPage";
import ServiceDetailPage from "@/features/services/pages/ServiceDetailPage";

export const AppRoutes = () => {
  return (
    <Routes>
      <Route path="/" element={<Index />} />
      <Route path="/products/:slug" element={<ProductDetailPage />} />
      <Route path="/cart" element={<CartPage />} />
      <Route path="/order" element={<OrderPage />} />
      <Route
        path="/my-orders"
        element={
          <ProtectedRoute requireAuth>
            <MyOrdersPage />
          </ProtectedRoute>
        }
      />
      <Route path="/blog" element={<BlogPage />} />
      <Route path="/blog/:slug" element={<PostDetailPage />} />
      <Route path="/services/:slug" element={<ServiceDetailPage />} />
      <Route path="/auth" element={<AuthPage />} />
      <Route path="/auth/google/callback" element={<GoogleCallback />} />
      <Route
        path="/admin"
        element={
          <ProtectedRoute requireAuth requireAdmin>
            <AdminPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/orders"
        element={
          <ProtectedRoute requireAuth requireAdmin>
            <AdminPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/products"
        element={
          <ProtectedRoute requireAuth requireAdmin>
            <AdminPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/categories"
        element={
          <ProtectedRoute requireAuth requireAdmin>
            <AdminPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/brands"
        element={
          <ProtectedRoute requireAuth requireAdmin>
            <AdminPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/banners"
        element={
          <ProtectedRoute requireAuth requireAdmin>
            <AdminPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/posts"
        element={
          <ProtectedRoute requireAuth requireAdmin>
            <AdminPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/post-categories"
        element={
          <ProtectedRoute requireAuth requireAdmin>
            <AdminPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/users"
        element={
          <ProtectedRoute requireAuth requireAdmin>
            <AdminPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/services"
        element={
          <ProtectedRoute requireAuth requireAdmin>
            <AdminPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/inbox"
        element={
          <ProtectedRoute requireAuth requireAdmin>
            <AdminPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/posts/new"
        element={
          <ProtectedRoute requireAuth requireAdmin>
            <PostEditorPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/posts/edit/:id"
        element={
          <ProtectedRoute requireAuth requireAdmin>
            <PostEditorPage />
          </ProtectedRoute>
        }
      />
      {/* ADD ALL CUSTOM ROUTES ABOVE THE CATCH-ALL "*" ROUTE */}
      <Route path="*" element={<NotFound />} />
    </Routes>
  );
};
