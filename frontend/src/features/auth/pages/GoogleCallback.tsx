import { useEffect, useRef } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useQueryClient } from "@tanstack/react-query";
import { AuthService } from "@/features/auth/api/auth";
import { useAuth } from "@/context/auth-context";
import { isAdmin } from "@/features/auth/hooks/useAuth";
import { useToast } from "@/hooks/use-toast";
import { Loader2 } from "lucide-react";

/**
 * Handles the Google OAuth2 callback.
 * Google redirects here with ?code=xxx after user consents.
 * This component sends the code to backend and completes login.
 */
const GoogleCallback = () => {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const { login: authLogin } = useAuth();
    const queryClient = useQueryClient();
    const { toast } = useToast();
    const hasRun = useRef(false);

    useEffect(() => {
        // Prevent double execution in StrictMode
        if (hasRun.current) return;
        hasRun.current = true;

        const code = searchParams.get("code");
        const error = searchParams.get("error");

        if (error) {
            toast({
                variant: "destructive",
                title: "Đăng nhập Google thất bại",
                description: "Bạn đã từ chối quyền truy cập hoặc có lỗi xảy ra.",
            });
            navigate("/auth");
            return;
        }

        if (!code) {
            toast({
                variant: "destructive",
                title: "Lỗi",
                description: "Không nhận được mã xác thực từ Google.",
            });
            navigate("/auth");
            return;
        }

        // Exchange code for tokens via backend
        AuthService.googleLogin(code)
            .then((data) => {
                authLogin(data.user, data.accessToken, data.refreshToken);
                queryClient.invalidateQueries({ queryKey: ["cart", data.user.id] });

                toast({
                    title: "Đăng nhập thành công!",
                    description: `Chào mừng ${data.user.fullName || data.user.username}!`,
                });

                // Redirect based on role
                if (isAdmin(data.user)) {
                    navigate("/admin");
                } else {
                    navigate("/");
                }
            })
            .catch((err: any) => {
                console.error("Google login failed:", err);
                toast({
                    variant: "destructive",
                    title: "Đăng nhập Google thất bại",
                    description: err.message || "Đã có lỗi xảy ra. Vui lòng thử lại.",
                });
                navigate("/auth");
            });
    }, [searchParams, navigate, authLogin, queryClient, toast]);

    return (
        <div className="min-h-screen flex items-center justify-center bg-background">
            <div className="text-center space-y-4">
                <Loader2 className="h-12 w-12 animate-spin text-primary mx-auto" />
                <p className="text-lg text-muted-foreground">
                    Đang đăng nhập bằng Google...
                </p>
            </div>
        </div>
    );
};

export default GoogleCallback;
