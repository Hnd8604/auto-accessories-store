import {
  FileText,
  MessageCircle,
  Package,
  ShoppingCart,
  Users,
  Wrench,
  type LucideIcon,
} from "lucide-react";

export const adminSections = [
  { id: "orders", label: "Đơn hàng", icon: ShoppingCart },
  { id: "products", label: "Sản phẩm", icon: Package },
  { id: "categories", label: "Danh mục sản phẩm", icon: FileText },
  { id: "brands", label: "Thương hiệu", icon: FileText },
  { id: "banners", label: "Banner", icon: FileText },
  { id: "posts", label: "Bài viết", icon: FileText },
  { id: "post-categories", label: "Danh mục bài viết", icon: FileText },
  { id: "services", label: "Dịch vụ", icon: Wrench },
  { id: "inbox", label: "Tin nhắn", icon: MessageCircle },
  { id: "users", label: "Người dùng", icon: Users },
] as const satisfies ReadonlyArray<{ id: string; label: string; icon: LucideIcon }>;

export type AdminSection = (typeof adminSections)[number]["id"];

export function isAdminSection(value: string): value is AdminSection {
  return adminSections.some((section) => section.id === value);
}
