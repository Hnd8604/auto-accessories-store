import { lazy, Suspense } from "react";
import { useQuery } from "@tanstack/react-query";
import { CategoriesApi } from "@/features/categories/api/categories";
import { BrandsApi } from "@/features/brands/api/brands";
import type { AdminSection } from "../sections";

const OrderManagement = lazy(() => import("@/features/orders/components/OrderManagement").then((module) => ({ default: module.OrderManagement })));
const ProductManagement = lazy(() => import("@/features/products/components/ProductManagement"));
const CategoryManagement = lazy(() => import("@/features/categories/components/CategoryManagement").then((module) => ({ default: module.CategoryManagement })));
const BrandManagement = lazy(() => import("@/features/brands/components/BrandManagement").then((module) => ({ default: module.BrandManagement })));
const BannerManagement = lazy(() => import("@/features/banners/components/BannerManagement").then((module) => ({ default: module.BannerManagement })));
const PostManagement = lazy(() => import("@/features/posts/components/PostManagement").then((module) => ({ default: module.PostManagement })));
const PostCategoryManagement = lazy(() => import("@/features/posts/components/PostCategoryManagement").then((module) => ({ default: module.PostCategoryManagement })));
const ServiceManagement = lazy(() => import("@/features/services/components/ServiceManagement").then((module) => ({ default: module.ServiceManagement })));
const InboxPage = lazy(() => import("@/features/inbox/components/InboxPage").then((module) => ({ default: module.InboxPage })));
const UserManagement = lazy(() => import("@/features/users/components/UserManagement").then((module) => ({ default: module.UserManagement })));

function ProductsSection() {
  const { data: categoriesData } = useQuery({
    queryKey: ["categories"],
    queryFn: CategoriesApi.getAll,
  });
  const { data: brandsData } = useQuery({
    queryKey: ["brands"],
    queryFn: BrandsApi.getAll,
  });

  const categoryOptions = categoriesData?.result?.map((category) => ({
    id: Number(category.id),
    name: category.name,
  })) ?? [];
  const brandOptions = brandsData?.result?.map((brand) => ({
    id: brand.id,
    name: brand.name,
  })) ?? [];

  return <ProductManagement categoryOptions={categoryOptions} brandOptions={brandOptions} />;
}

export function AdminContent({ section }: { section: AdminSection }) {
  let content: React.ReactNode;
  switch (section) {
    case "orders": content = <OrderManagement />; break;
    case "products": content = <ProductsSection />; break;
    case "categories": content = <><h2 className="text-2xl font-bold mb-6">Quản lý Danh mục sản phẩm</h2><CategoryManagement /></>; break;
    case "brands": content = <><h2 className="text-2xl font-bold mb-6">Quản lý Thương hiệu</h2><BrandManagement /></>; break;
    case "banners": content = <BannerManagement />; break;
    case "posts": content = <PostManagement />; break;
    case "post-categories": content = <PostCategoryManagement />; break;
    case "services": content = <ServiceManagement />; break;
    case "inbox": content = <InboxPage />; break;
    case "users": content = <UserManagement />; break;
  }

  return <Suspense fallback={<div role="status" className="py-12 text-center">Đang tải...</div>}>{content}</Suspense>;
}
