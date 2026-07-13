import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Car, Phone, Mail, Menu, User, LogOut, Settings, ShoppingCart, KeyRound, ClipboardList } from "lucide-react";
import { useState as useImageState } from "react";
import { useAuth } from "@/context/auth-context";
import { useCart } from "@/context/cart-context";
import { isAdmin } from "@/features/auth/hooks/useAuth";
import { AuthService } from "@/features/auth/api/auth";
import { useNavigate, useLocation } from "react-router-dom";
import { useQueryClient } from "@tanstack/react-query";
const COMPANY_HOTLINE = String(import.meta.env.VITE_COMPANY_HOTLINE);
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { SearchBar } from "@/components/SearchBar";
import { ChangePasswordDialog } from "@/features/auth/components/ChangePasswordDialog";
import { NotificationBell } from "@/features/notifications/components/NotificationBell";

import { memo, useCallback, useState, useEffect } from "react";

const LOGO_SRC = "/logo.jpeg";
const LogoArea = memo(() => {
  const [imgError, setImgError] = useImageState(false);

  return (
    <a
      href="/"
      className="flex items-center gap-2 flex-shrink-0 group"
      aria-label="AutoLux Interior – Trang chủ"
    >
      {!imgError ? (
        <img
          src={LOGO_SRC}
          alt="AutoLux Interior logo"
          onError={() => setImgError(true)}
          className="h-10 w-auto max-w-[160px] object-contain transition-opacity duration-200 group-hover:opacity-80"
        />
      ) : (
        /* ── Fallback: icon + text ── */
        <>
          <Car className="h-8 w-8 text-primary transition-transform duration-200 group-hover:scale-110" />
          <span className="text-xl md:text-2xl font-bold bg-gradient-accent bg-clip-text text-transparent whitespace-nowrap">
            AutoLux Interior
          </span>
        </>
      )}
    </a>
  );
});

LogoArea.displayName = "LogoArea";

const CartButton = memo(() => {
  const { itemCount } = useCart();
  const navigate = useNavigate();

  const handleClick = useCallback(() => {
    navigate("/cart");
  }, [navigate]);

  return (
    <Button
      variant="ghost"
      size="sm"
      className="relative text-gray-200 hover:text-white hover:bg-gray-700"
      onClick={handleClick}
    >
      <ShoppingCart className="h-5 w-5" />
      {itemCount > 0 && (
        <Badge
          variant="destructive"
          className="absolute -top-2 -right-2 h-5 w-5 flex items-center justify-center p-0 text-xs"
        >
          {itemCount}
        </Badge>
      )}
      <span className="ml-2 hidden sm:inline">Giỏ Hàng</span>
    </Button>
  );
});

CartButton.displayName = "CartButton";

export const Header = memo(() => {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const queryClient = useQueryClient();
  const [isVisible, setIsVisible] = useState(true);
  const [lastScrollY, setLastScrollY] = useState(0);
  const [openChangePassword, setOpenChangePassword] = useState(false);

  const handleContactClick = useCallback((e: React.MouseEvent<HTMLAnchorElement>) => {
    e.preventDefault();
    if (location.pathname === '/') {
      // Already on homepage — smooth scroll
      const el = document.getElementById('contact');
      if (el) {
        el.scrollIntoView({ behavior: 'smooth' });
      }
    } else {
      // Navigate to homepage then scroll
      navigate('/#contact');
      setTimeout(() => {
        const el = document.getElementById('contact');
        if (el) el.scrollIntoView({ behavior: 'smooth' });
      }, 300);
    }
  }, [location.pathname, navigate]);

  // Handle scroll to show/hide header
  useEffect(() => {
    const handleScroll = () => {
      const currentScrollY = window.scrollY;

      // Show header when at top
      if (currentScrollY < 10) {
        setIsVisible(true);
      }
      // Hide header when scrolling down
      else if (currentScrollY > lastScrollY && currentScrollY > 100) {
        setIsVisible(false);
      }
      // Show header when scrolling up
      else if (currentScrollY < lastScrollY) {
        setIsVisible(true);
      }

      setLastScrollY(currentScrollY);
    };

    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => window.removeEventListener('scroll', handleScroll);
  }, [lastScrollY]);

  const handleLogout = useCallback(async () => {
    try {
      await AuthService.logout();
      logout();
      // Invalidate sessionCart to refetch after logout
      queryClient.invalidateQueries({ queryKey: ["sessionCart"] });
      navigate("/");
    } catch (error) {
      console.error("Logout error:", error);
      // Still logout locally even if API call fails
      logout();
      queryClient.invalidateQueries({ queryKey: ["sessionCart"] });
      navigate("/");
    }
  }, [logout, queryClient, navigate]);

  const getUserInitials = useCallback((user: any) => {
    if (user?.fullName) {
      const names = user.fullName.split(' ');
      if (names.length >= 2) {
        return `${names[0][0]}${names[names.length - 1][0]}`.toUpperCase();
      }
      return user.fullName[0].toUpperCase();
    }
    if (user?.username) {
      return user.username[0].toUpperCase();
    }
    return "U";
  }, []);

  const getHighestRole = useCallback((user: any) => {
    if (!user?.roles || user.roles.length === 0) {
      return "USER";
    }

    // Check if user has ADMIN role
    const hasAdmin = user.roles.some((role: any) =>
      role.name?.toUpperCase() === "ADMIN"
    );

    if (hasAdmin) {
      return "ADMIN";
    }

    // Otherwise return first role name in uppercase
    return user.roles[0]?.name?.toUpperCase() || "USER";
  }, []);

  return (
    <>
      <header className={`fixed top-0 left-0 right-0 z-50 transition-transform duration-300 ${isVisible ? 'translate-y-0' : '-translate-y-full'}`}>
        {/* Row 1: Top bar */}
        <div className="bg-gray-900 border-b border-gray-700">
          <div className="w-full px-6">
            <div className="grid grid-cols-3 items-center gap-4 py-4">
              {/* Logo — left column */}
              <div className="flex items-center justify-start">
                <LogoArea />
              </div>

              {/* Search Bar — centre column */}
              <div className="flex justify-center">
                <div className="w-full max-w-md">
                  <SearchBar />
                </div>
              </div>

              {/* Right Actions — right column */}
              <div className="flex items-center justify-end gap-3 md:gap-4">
                {/* Phone */}
                <a
                  href={`tel:${COMPANY_HOTLINE.replace(/\s/g, '')}`}
                  className="hidden lg:flex items-center gap-2 text-sm text-gray-300 hover:text-white transition-colors"
                >
                  <Phone className="h-4 w-4" />
                  <span>{COMPANY_HOTLINE}</span>
                </a>

                {/* User Authentication */}
                {isAuthenticated ? (
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button
                        variant="ghost"
                        className="relative h-10 w-10 rounded-full hover:bg-gray-700"
                      >
                        <Avatar className="h-10 w-10">
                          <AvatarImage src="" alt={user?.username || ""} />
                          <AvatarFallback className="bg-primary/10 text-primary">
                            {getUserInitials(user)}
                          </AvatarFallback>
                        </Avatar>
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent className="w-56" align="end" forceMount>
                      <DropdownMenuLabel className="font-normal">
                        <div className="flex flex-col space-y-1">
                          <p className="text-sm font-medium leading-none">
                            {user?.fullName || user?.username}
                          </p>
                          <p className="text-xs leading-none text-muted-foreground">
                            {user?.email || user?.username}
                          </p>
                          <p className="text-xs leading-none text-muted-foreground">
                            Role: {getHighestRole(user)}
                          </p>
                        </div>
                      </DropdownMenuLabel>
                      <DropdownMenuSeparator />
                      {isAdmin(user) && (
                        <DropdownMenuItem onClick={() => navigate("/admin")}>
                          <Settings className="mr-2 h-4 w-4" />
                          <span>Quản trị</span>
                        </DropdownMenuItem>
                      )}
                      <DropdownMenuItem onClick={() => setOpenChangePassword(true)}>
                        <KeyRound className="mr-2 h-4 w-4" />
                        <span>Đổi mật khẩu</span>
                      </DropdownMenuItem>
                      <DropdownMenuItem onClick={() => navigate("/my-orders")}>
                        <ClipboardList className="mr-2 h-4 w-4" />
                        <span>Đơn hàng của tôi</span>
                      </DropdownMenuItem>
                      <DropdownMenuSeparator />
                      <DropdownMenuItem onClick={handleLogout}>
                        <LogOut className="mr-2 h-4 w-4" />
                        <span>Đăng xuất</span>
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                ) : (
                  <a
                    href="/auth"
                    className="flex items-center justify-center w-10 h-10 rounded-full bg-gray-700 hover:bg-gray-600 transition-colors"
                    title="Đăng nhập / Đăng ký"
                  >
                    <User className="h-5 w-5 text-gray-200" />
                  </a>
                )}

                {/* Notification Bell - chỉ hiện khi đăng nhập */}
                {isAuthenticated && <NotificationBell />}

                <CartButton />
                <Button
                  variant="luxury"
                  size="sm"
                  className="hidden lg:inline-flex"
                  onClick={() => {
                    if (location.pathname === '/') {
                      document.getElementById('contact')?.scrollIntoView({ behavior: 'smooth' });
                    } else {
                      navigate('/');
                      setTimeout(() => {
                        document.getElementById('contact')?.scrollIntoView({ behavior: 'smooth' });
                      }, 300);
                    }
                  }}
                >
                  Tư Vấn Miễn Phí
                </Button>
              </div>
            </div>
          </div>
        </div>

        {/* Row 2: Red navigation bar */}
        <div className="bg-red-600 shadow-md">
          <div className="container mx-auto px-4">
            <div className="py-0">
              <nav className="hidden md:flex items-center justify-center gap-1">
                {[
                  { href: "/", label: "Trang Chủ" },
                  { href: "/order", label: "Đặt Hàng" },
                  { href: "#services", label: "Dịch Vụ" },
                  { href: "/blog", label: "Blog" },
                ].map(({ href, label }) => (
                  <a
                    key={href}
                    href={href}
                    className="relative px-4 py-3 text-sm font-semibold text-white tracking-wide hover:bg-red-700 transition-colors duration-200 group"
                  >
                    {label}
                    <span className="absolute bottom-0 left-0 w-full h-0.5 bg-white scale-x-0 group-hover:scale-x-100 transition-transform duration-200 origin-center" />
                  </a>
                ))}
                <a
                  href="/#contact"
                  onClick={handleContactClick}
                  className="relative px-4 py-3 text-sm font-semibold text-white tracking-wide hover:bg-red-700 transition-colors duration-200 group"
                >
                  Liên Hệ
                  <span className="absolute bottom-0 left-0 w-full h-0.5 bg-white scale-x-0 group-hover:scale-x-100 transition-transform duration-200 origin-center" />
                </a>
              </nav>
            </div>
          </div>
        </div>
      </header>

      {/* Change Password Dialog */}
      <ChangePasswordDialog
        open={openChangePassword}
        onOpenChange={setOpenChangePassword}
      />
    </>
  );
});

Header.displayName = "Header";
