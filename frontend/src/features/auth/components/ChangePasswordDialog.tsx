import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { AuthService } from "@/features/auth/api/auth";
import { FormDialog } from "@/components/shared/FormDialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { toast } from "sonner";
import { Eye, EyeOff, Lock, ShieldCheck } from "lucide-react";
import type { ChangePasswordRequest } from "@/features/users/types";

interface ChangePasswordDialogProps {
    open: boolean;
    onOpenChange: (open: boolean) => void;
}

export function ChangePasswordDialog({
    open,
    onOpenChange,
}: ChangePasswordDialogProps) {
    const [currentPassword, setCurrentPassword] = useState("");
    const [newPassword, setNewPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");

    const [showCurrentPassword, setShowCurrentPassword] = useState(false);
    const [showNewPassword, setShowNewPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);

    const resetForm = () => {
        setCurrentPassword("");
        setNewPassword("");
        setConfirmPassword("");
        setShowCurrentPassword(false);
        setShowNewPassword(false);
        setShowConfirmPassword(false);
    };

    const changePasswordMutation = useMutation({
        mutationFn: (payload: ChangePasswordRequest) =>
            AuthService.changePassword(payload),
        onSuccess: () => {
            toast.success("Đổi mật khẩu thành công!");
            resetForm();
            onOpenChange(false);
        },
        onError: (error: Error) => {
            toast.error(error.message || "Đổi mật khẩu thất bại");
        },
    });

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();

        // Kiểm tra frontend
        if (newPassword.length < 8) {
            toast.error("Mật khẩu mới phải có ít nhất 8 ký tự");
            return;
        }

        if (newPassword !== confirmPassword) {
            toast.error("Xác nhận mật khẩu không khớp");
            return;
        }

        if (currentPassword === newPassword) {
            toast.error("Mật khẩu mới không được trùng mật khẩu hiện tại");
            return;
        }

        changePasswordMutation.mutate({
            currentPassword,
            newPassword,
            confirmPassword,
        });
    };

    return (
        <FormDialog
            open={open}
            onOpenChange={(val) => {
                if (!val) resetForm();
                onOpenChange(val);
            }}
            title="Đổi mật khẩu"
            description="Nhập mật khẩu hiện tại và mật khẩu mới để thay đổi"
        >
            <form onSubmit={handleSubmit} className="space-y-5 py-2">
                {/* Mật khẩu hiện tại */}
                <div className="space-y-2">
                    <Label htmlFor="currentPassword" className="flex items-center gap-2">
                        <Lock className="h-4 w-4" />
                        Mật khẩu hiện tại
                    </Label>
                    <div className="relative">
                        <Input
                            id="currentPassword"
                            type={showCurrentPassword ? "text" : "password"}
                            value={currentPassword}
                            onChange={(e) => setCurrentPassword(e.target.value)}
                            placeholder="Nhập mật khẩu hiện tại"
                            required
                        />
                        <button
                            type="button"
                            className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
                            onClick={() => setShowCurrentPassword((p) => !p)}
                        >
                            {showCurrentPassword ? (
                                <EyeOff className="h-4 w-4" />
                            ) : (
                                <Eye className="h-4 w-4" />
                            )}
                        </button>
                    </div>
                </div>

                {/* Mật khẩu mới */}
                <div className="space-y-2">
                    <Label htmlFor="newPassword" className="flex items-center gap-2">
                        <ShieldCheck className="h-4 w-4" />
                        Mật khẩu mới
                    </Label>
                    <div className="relative">
                        <Input
                            id="newPassword"
                            type={showNewPassword ? "text" : "password"}
                            value={newPassword}
                            onChange={(e) => setNewPassword(e.target.value)}
                            placeholder="Nhập mật khẩu mới (tối thiểu 8 ký tự)"
                            required
                            minLength={8}
                        />
                        <button
                            type="button"
                            className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
                            onClick={() => setShowNewPassword((p) => !p)}
                        >
                            {showNewPassword ? (
                                <EyeOff className="h-4 w-4" />
                            ) : (
                                <Eye className="h-4 w-4" />
                            )}
                        </button>
                    </div>
                    {newPassword.length > 0 && newPassword.length < 8 && (
                        <p className="text-sm text-destructive">
                            Mật khẩu phải có ít nhất 8 ký tự
                        </p>
                    )}
                </div>

                {/* Xác nhận mật khẩu mới */}
                <div className="space-y-2">
                    <Label htmlFor="confirmPassword" className="flex items-center gap-2">
                        <ShieldCheck className="h-4 w-4" />
                        Xác nhận mật khẩu mới
                    </Label>
                    <div className="relative">
                        <Input
                            id="confirmPassword"
                            type={showConfirmPassword ? "text" : "password"}
                            value={confirmPassword}
                            onChange={(e) => setConfirmPassword(e.target.value)}
                            placeholder="Nhập lại mật khẩu mới"
                            required
                        />
                        <button
                            type="button"
                            className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
                            onClick={() => setShowConfirmPassword((p) => !p)}
                        >
                            {showConfirmPassword ? (
                                <EyeOff className="h-4 w-4" />
                            ) : (
                                <Eye className="h-4 w-4" />
                            )}
                        </button>
                    </div>
                    {confirmPassword.length > 0 && newPassword !== confirmPassword && (
                        <p className="text-sm text-destructive">
                            Mật khẩu xác nhận không khớp
                        </p>
                    )}
                </div>

                {/* Buttons */}
                <div className="flex justify-end gap-3 pt-2">
                    <Button
                        type="button"
                        variant="outline"
                        onClick={() => {
                            resetForm();
                            onOpenChange(false);
                        }}
                        disabled={changePasswordMutation.isPending}
                    >
                        Hủy
                    </Button>
                    <Button
                        type="submit"
                        disabled={
                            changePasswordMutation.isPending ||
                            !currentPassword ||
                            !newPassword ||
                            !confirmPassword
                        }
                    >
                        {changePasswordMutation.isPending
                            ? "Đang xử lý..."
                            : "Xác nhận đổi mật khẩu"}
                    </Button>
                </div>
            </form>
        </FormDialog>
    );
}
