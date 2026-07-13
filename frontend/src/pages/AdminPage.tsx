import { useMemo } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate, useLocation } from "react-router-dom";
import { useAuth } from "@/context/auth-context";
import { AuthService } from "@/features/auth/api/auth";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  ShoppingCart,
  FileText,
  Users,
  LogOut,
  Car,
  Eye,
  Wrench,
  MessageCircle,
} from "lucide-react";
import ProductManagement from "@/features/products/components/ProductManagement";
import { CategoryManagement } from "@/features/categories/components/CategoryManagement";
import { BrandManagement } from "@/features/brands/components/BrandManagement";
import { UserManagement } from "@/features/users/components/UserManagement";
import { PostManagement, PostCategoryManagement } from "@/features/posts/components";
import { OrderManagement } from "@/features/orders/components";
import { BannerManagement } from "@/features/banners/components";
import { ServiceManagement } from "@/features/services/components";
import { InboxPage } from "@/features/inbox/components/InboxPage";
import { CategoriesApi } from "@/features/categories/api/categories";
import { BrandsApi } from "@/features/brands/api/brands";
import { PostCategoriesApi } from "@/features/posts/api";
import { useToast } from "@/hooks/use-toast";

const AdminPage = () => {
  const { toast } = useToast();
  const navigate = useNavigate();
  const location = useLocation();
  const { user, logout } = useAuth();
  const queryClient = useQueryClient();

  // Determine active tab based on current URL path
  const activeTab = useMemo(() => {
    const path = location.pathname;
    if (path === "/admin" || path === "/admin/") return "orders";
    if (path.includes("/orders")) return "orders";
    if (path.includes("/products")) return "products";
    if (path.includes("/categories")) return "categories";
    if (path.includes("/brands")) return "brands";
    if (path.includes("/banners")) return "banners";
    if (path.includes("/posts")) return "posts";
    if (path.includes("/post-categories")) return "post-categories";
    if (path.includes("/users")) return "users";
    if (path.includes("/services")) return "services";
    if (path.includes("/inbox")) return "inbox";
    return "orders";
  }, [location.pathname]);

  const handleViewWebsite = () => {
    navigate("/");
  };

  const handleLogout = async () => {
    try {
      await AuthService.logout();
      logout();
      // Invalidate sessionCart to refetch after logout
      queryClient.invalidateQueries({ queryKey: ["sessionCart"] });
      navigate("/");
      toast({
        title: "Đăng xuất thành công",
        description: "Bạn đã đăng xuất khỏi tài khoản admin.",
      });
    } catch (error) {
      console.error("Logout error:", error);
      // Still logout locally even if API call fails
      logout();
      queryClient.invalidateQueries({ queryKey: ["sessionCart"] });
      navigate("/");
    }
  };

  const getUserDisplayName = () => {
    if (user?.fullName) {
      return user.fullName;
    }
    return user?.username || "Admin";
  };

  // Fetch categories and brands for ProductManagement
  const { data: categoriesData } = useQuery({
    queryKey: ["categories"],
    queryFn: CategoriesApi.getAll,
  });

  const { data: brandsData } = useQuery({
    queryKey: ["brands"],
    queryFn: BrandsApi.getAll,
  });

  const { data: postCategoriesData } = useQuery({
    queryKey: ["postCategories"],
    queryFn: PostCategoriesApi.getAll,
  });

  const categoryOptions =
    categoriesData?.result?.map((cat) => ({
      id: Number.parseInt(cat.id),
      name: cat.name,
    })) || [];

  const brandOptions =
    brandsData?.result?.map((brand) => ({
      id: brand.id,
      name: brand.name,
    })) || [];

  const postCategoryOptions =
    postCategoriesData?.result?.map((cat) => ({
      id: cat.id,
      name: cat.name,
    })) || [];

  return (
    <div className="flex min-h-screen bg-gray-50">
      {/* Sidebar */}
      <aside className="hidden lg:flex lg:flex-col w-64 bg-white border-r shadow-sm">
        <div className="p-6 border-b">
          <div className="flex items-center space-x-2">
            <Car className="h-8 w-8 text-primary" />
            <div>
              <h1 className="text-lg font-bold">AutoLux</h1>
              <p className="text-xs text-muted-foreground">Admin Dashboard</p>
            </div>
          </div>
        </div>
        
        <nav className="flex-1 p-4 space-y-1">
          <Button
            variant={activeTab === "orders" ? "secondary" : "ghost"}
            className="w-full justify-start"
            onClick={() => navigate("/admin/orders")}
          >
            <ShoppingCart className="mr-2 h-4 w-4" />
            Đơn hàng
          </Button>
          <Button
            variant={activeTab === "products" ? "secondary" : "ghost"}
            className="w-full justify-start"
            onClick={() => navigate("/admin/products")}
          >
            <ShoppingCart className="mr-2 h-4 w-4" />
            Sản phẩm
          </Button>
          <Button
            variant={activeTab === "categories" ? "secondary" : "ghost"}
            className="w-full justify-start"
            onClick={() => navigate("/admin/categories")}
          >
            <FileText className="mr-2 h-4 w-4" />
            Danh mục sản phẩm
          </Button>
          <Button
            variant={activeTab === "brands" ? "secondary" : "ghost"}
            className="w-full justify-start"
            onClick={() => navigate("/admin/brands")}
          >
            <FileText className="mr-2 h-4 w-4" />
            Thương hiệu
          </Button>
          <Button
            variant={activeTab === "banners" ? "secondary" : "ghost"}
            className="w-full justify-start"
            onClick={() => navigate("/admin/banners")}
          >
            <FileText className="mr-2 h-4 w-4" />
            Banner
          </Button>
          <Button
            variant={activeTab === "posts" ? "secondary" : "ghost"}
            className="w-full justify-start"
            onClick={() => navigate("/admin/posts")}
          >
            <FileText className="mr-2 h-4 w-4" />
            Bài viết
          </Button>
          <Button
            variant={activeTab === "post-categories" ? "secondary" : "ghost"}
            className="w-full justify-start"
            onClick={() => navigate("/admin/post-categories")}
          >
            <FileText className="mr-2 h-4 w-4" />
            Danh mục bài viết
          </Button>
          <Button
            variant={activeTab === "services" ? "secondary" : "ghost"}
            className="w-full justify-start"
            onClick={() => navigate("/admin/services")}
          >
            <Wrench className="mr-2 h-4 w-4" />
            Dịch vụ
          </Button>
          <Button
            variant={activeTab === "inbox" ? "secondary" : "ghost"}
            className="w-full justify-start"
            onClick={() => navigate("/admin/inbox")}
          >
            <MessageCircle className="mr-2 h-4 w-4" />
            Tin nhắn
          </Button>
          <Button
            variant={activeTab === "users" ? "secondary" : "ghost"}
            className="w-full justify-start"
            onClick={() => navigate("/admin/users")}
          >
            <Users className="mr-2 h-4 w-4" />
            Người dùng
          </Button>
        </nav>

        <div className="p-4 border-t">
          <Button
            variant="outline"
            className="w-full justify-start mb-2"
            onClick={handleViewWebsite}
          >
            <Eye className="mr-2 h-4 w-4" />
            Xem trang web
          </Button>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" className="w-full justify-start">
                <Users className="mr-2 h-4 w-4" />
                <span className="truncate">{getUserDisplayName()}</span>
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-56">
              <DropdownMenuLabel>
                <div className="flex flex-col space-y-1">
                  <p className="text-sm font-medium leading-none">
                    {getUserDisplayName()}
                  </p>
                  {user?.email && (
                    <p className="text-xs leading-none text-muted-foreground">
                      {user.email}
                    </p>
                  )}
                  <p className="text-xs leading-none text-muted-foreground">
                    Role: {user?.roles?.[0]?.name || "Admin"}
                  </p>
                </div>
              </DropdownMenuLabel>
              <DropdownMenuSeparator />
              <DropdownMenuItem onClick={handleLogout}>
                <LogOut className="mr-2 h-4 w-4" />
                Đăng xuất
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </aside>

      {/* Main Content */}
      <div className="flex-1 flex flex-col">
        {/* Mobile Header */}
        <header className="lg:hidden border-b bg-white">
          <div className="px-4 h-16 flex items-center justify-between">
            <div className="flex items-center space-x-2">
              <Car className="h-6 w-6 text-primary" />
              <h1 className="text-lg font-bold">AutoLux Admin</h1>
            </div>
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" size="sm">
                  <Users className="h-4 w-4" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-56">
                <DropdownMenuLabel>
                  <div className="flex flex-col space-y-1">
                    <p className="text-sm font-medium">{getUserDisplayName()}</p>
                    {user?.email && (
                      <p className="text-xs text-muted-foreground">{user.email}</p>
                    )}
                  </div>
                </DropdownMenuLabel>
                <DropdownMenuSeparator />
                <DropdownMenuItem onClick={handleViewWebsite}>
                  <Eye className="mr-2 h-4 w-4" />
                  Xem trang web
                </DropdownMenuItem>
                <DropdownMenuItem onClick={handleLogout}>
                  <LogOut className="mr-2 h-4 w-4" />
                  Đăng xuất
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </header>

        <main className="flex-1 overflow-y-auto p-6">

          {/* Orders Tab */}
          {activeTab === "orders" && (
            <OrderManagement />
          )}

          {/* Products Tab */}
          {activeTab === "products" && (
            <ProductManagement
              categoryOptions={categoryOptions}
              brandOptions={brandOptions}
            />
          )}

          {/* Categories Tab */}
          {activeTab === "categories" && (
            <div>
              <h2 className="text-2xl font-bold mb-6">Quản lý Danh mục sản phẩm</h2>
              <CategoryManagement />
            </div>
          )}

          {/* Brands Tab */}
          {activeTab === "brands" && (
            <div>
              <h2 className="text-2xl font-bold mb-6">Quản lý Thương hiệu</h2>
              <BrandManagement />
            </div>
          )}

          {/* Banners Tab */}
          {activeTab === "banners" && (
            <BannerManagement />
          )}

          {/* Posts Tab */}
          {activeTab === "posts" && (
            <PostManagement />
          )}

          {/* Post Categories Tab */}
          {activeTab === "post-categories" && (
            <PostCategoryManagement />
          )}

          {/* Services Tab */}
          {activeTab === "services" && (
            <ServiceManagement />
          )}

          {/* Users Tab */}
          {activeTab === "users" && (
            <UserManagement />
          )}

          {/* Inbox Tab */}
          {activeTab === "inbox" && (
            <InboxPage />
          )}
        </main>
      </div>
    </div>
  );
};

export default AdminPage;
