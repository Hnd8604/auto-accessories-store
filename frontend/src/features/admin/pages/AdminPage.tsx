import { Link, useNavigate, useParams } from "react-router-dom";
import { Car, Eye, LogOut, Users } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { useLogout } from "@/features/auth/hooks/useLogout";
import { useToast } from "@/hooks/use-toast";
import NotFoundPage from "@/app/pages/NotFoundPage";
import { AdminContent } from "../components/AdminContent";
import { adminSections, isAdminSection } from "../sections";

export default function AdminPage() {
  const { section: sectionParam } = useParams<{ section: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const signOut = useLogout();
  const { toast } = useToast();
  const activeSection = sectionParam && isAdminSection(sectionParam)
    ? sectionParam
    : "orders";

  if (sectionParam && !isAdminSection(sectionParam)) {
    return <NotFoundPage />;
  }

  const userName = user?.fullName || user?.username || "Admin";

  const handleLogout = async () => {
    if (await signOut()) {
      toast({ title: "Đăng xuất thành công" });
    }
  };

  return (
    <div className="flex min-h-screen bg-gray-50">
      <aside className="hidden lg:flex lg:flex-col w-64 shrink-0 bg-white border-r shadow-sm">
        <div className="p-6 border-b flex items-center gap-2">
          <Car className="h-8 w-8 text-primary" />
          <div>
            <h1 className="text-lg font-bold">AutoLux</h1>
            <p className="text-xs text-muted-foreground">Admin Dashboard</p>
          </div>
        </div>

        <nav aria-label="Quản trị" className="flex-1 p-4 space-y-1">
          {adminSections.map(({ id, label, icon: Icon }) => (
            <Button key={id} asChild
              variant={activeSection === id ? "secondary" : "ghost"}
              className="w-full justify-start">
              <Link to={`/admin/${id}`} aria-current={activeSection === id ? "page" : undefined}>
                <Icon className="mr-2 h-4 w-4" />
                {label}
              </Link>
            </Button>
          ))}
        </nav>

        <div className="p-4 border-t space-y-2">
          <Button asChild variant="outline" className="w-full justify-start">
            <Link to="/"><Eye className="mr-2 h-4 w-4" />Xem trang web</Link>
          </Button>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" className="w-full justify-start">
                <Users className="mr-2 h-4 w-4" />
                <span className="truncate">{userName}</span>
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-56">
              <DropdownMenuLabel>
                <p className="font-medium truncate">{userName}</p>
                {user?.email && <p className="text-xs text-muted-foreground truncate">{user.email}</p>}
                <p className="text-xs text-muted-foreground">{user?.roles?.[0]?.name || "Admin"}</p>
              </DropdownMenuLabel>
              <DropdownMenuSeparator />
              <DropdownMenuItem onSelect={() => { void handleLogout(); }}>
                <LogOut className="mr-2 h-4 w-4" />Đăng xuất
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </aside>

      <div className="flex-1 min-w-0 flex flex-col">
        <header className="lg:hidden border-b bg-white p-4 space-y-3">
          <div className="flex items-center justify-between gap-2">
            <div className="flex items-center gap-2">
              <Car className="h-6 w-6 text-primary" />
              <span className="font-bold">AutoLux Admin</span>
            </div>
            <Button asChild variant="ghost" size="sm"><Link to="/">Xem web</Link></Button>
          </div>
          <div className="flex gap-2">
            <Select value={activeSection} onValueChange={(value) => navigate(`/admin/${value}`)}>
              <SelectTrigger aria-label="Chọn mục quản trị" className="flex-1">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {adminSections.map(({ id, label }) => (
                  <SelectItem key={id} value={id}>{label}</SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Button variant="outline" size="icon" aria-label="Đăng xuất" onClick={() => { void handleLogout(); }}>
              <LogOut className="h-4 w-4" />
            </Button>
          </div>
        </header>

        <main className="flex-1 p-4 md:p-6">
          <AdminContent section={activeSection} />
        </main>
      </div>
    </div>
  );
}
