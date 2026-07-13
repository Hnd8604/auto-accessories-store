import { useState, useEffect, useRef, useCallback } from "react";
import { useQuery } from "@tanstack/react-query";
import { PaymentsApi } from "@/features/orders/api";
import type { PaymentResponse } from "@/features/orders/types";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import {
    QrCode,
    Copy,
    CheckCircle2,
    Loader2,
    Clock,
    Banknote,
    CreditCard,
    RefreshCw,
} from "lucide-react";
import { useToast } from "@/hooks/use-toast";

interface PaymentDialogProps {
    isOpen: boolean;
    onClose: () => void;
    orderId: string | null;
    onPaymentSuccess?: () => void;
}

const formatPrice = (price: number) => {
    return new Intl.NumberFormat("vi-VN", {
        style: "currency",
        currency: "VND",
    }).format(price);
};

export const PaymentDialog = ({
    isOpen,
    onClose,
    orderId,
    onPaymentSuccess,
}: PaymentDialogProps) => {
    const { toast } = useToast();
    const [paymentData, setPaymentData] = useState<PaymentResponse | null>(null);
    const [isCreating, setIsCreating] = useState(false);
    const [isPaid, setIsPaid] = useState(false);
    const [copied, setCopied] = useState<string | null>(null);
    const pollingRef = useRef<ReturnType<typeof setInterval> | null>(null);

    // Create payment QR when dialog opens
    useEffect(() => {
        if (isOpen && orderId && !paymentData) {
            createPayment();
        }
        return () => {
            if (pollingRef.current) {
                clearInterval(pollingRef.current);
                pollingRef.current = null;
            }
        };
    }, [isOpen, orderId]);

    // Reset state when dialog closes
    useEffect(() => {
        if (!isOpen) {
            setPaymentData(null);
            setIsPaid(false);
            setIsCreating(false);
            if (pollingRef.current) {
                clearInterval(pollingRef.current);
                pollingRef.current = null;
            }
        }
    }, [isOpen]);

    const createPayment = async () => {
        if (!orderId) return;
        setIsCreating(true);
        try {
            const response = await PaymentsApi.createPayment(orderId);
            if (response?.result) {
                setPaymentData(response.result);
                // Start polling for payment status
                startPolling(orderId);
            }
        } catch (error: any) {
            toast({
                variant: "destructive",
                title: "Lỗi tạo thanh toán",
                description: error.message || "Không thể tạo QR thanh toán",
            });
        } finally {
            setIsCreating(false);
        }
    };

    const startPolling = (id: string) => {
        // Clear existing polling
        if (pollingRef.current) {
            clearInterval(pollingRef.current);
        }

        // Poll every 5 seconds
        pollingRef.current = setInterval(async () => {
            try {
                const response = await PaymentsApi.checkPaymentStatus(id);
                if (response?.result?.paymentStatus === "PAID") {
                    setIsPaid(true);
                    setPaymentData((prev) =>
                        prev ? { ...prev, paymentStatus: "PAID" } : null
                    );
                    if (pollingRef.current) {
                        clearInterval(pollingRef.current);
                        pollingRef.current = null;
                    }
                    toast({
                        title: "🎉 Thanh toán thành công!",
                        description: "Đơn hàng của bạn đã được thanh toán.",
                    });
                    onPaymentSuccess?.();
                }
            } catch (error) {
                console.error("Error checking payment status:", error);
            }
        }, 5000);
    };

    const copyToClipboard = (text: string, label: string) => {
        navigator.clipboard.writeText(text);
        setCopied(label);
        setTimeout(() => setCopied(null), 2000);
        toast({
            title: "Đã sao chép!",
            description: `${label} đã được sao chép vào clipboard.`,
        });
    };

    if (!isOpen) return null;

    return (
        <Dialog open={isOpen} onOpenChange={onClose}>
            <DialogContent className="max-w-lg max-h-[90vh] overflow-y-auto">
                <DialogHeader>
                    <DialogTitle className="flex items-center gap-2 text-2xl">
                        <Banknote className="h-6 w-6 text-primary" />
                        Thanh Toán Chuyển Khoản
                    </DialogTitle>
                    <DialogDescription>
                        Quét mã QR hoặc chuyển khoản theo thông tin bên dưới
                    </DialogDescription>
                </DialogHeader>

                {isCreating ? (
                    <div className="flex flex-col items-center justify-center py-12">
                        <Loader2 className="h-12 w-12 animate-spin text-primary mb-4" />
                        <p className="text-muted-foreground">Đang tạo mã QR thanh toán...</p>
                    </div>
                ) : isPaid ? (
                    /* Payment Success State */
                    <div className="flex flex-col items-center justify-center py-12 text-center">
                        <div className="w-20 h-20 rounded-full bg-green-100 dark:bg-green-900/30 flex items-center justify-center mb-6">
                            <CheckCircle2 className="h-12 w-12 text-green-600" />
                        </div>
                        <h3 className="text-2xl font-bold text-green-600 mb-2">
                            Thanh toán thành công!
                        </h3>
                        <p className="text-muted-foreground mb-2">
                            Mã đơn hàng:{" "}
                            <span className="font-mono font-bold">
                                {paymentData?.orderCode}
                            </span>
                        </p>
                        <p className="text-muted-foreground mb-6">
                            Số tiền: <span className="font-bold">{paymentData ? formatPrice(paymentData.amount) : ""}</span>
                        </p>
                        <Button onClick={onClose} className="w-full max-w-xs">
                            Đóng
                        </Button>
                    </div>
                ) : paymentData ? (
                    /* QR Code and Payment Info */
                    <div className="space-y-6">
                        {/* Amount Display */}
                        <div className="text-center p-4 bg-primary/5 rounded-xl border border-primary/20">
                            <p className="text-sm text-muted-foreground mb-1">Số tiền cần thanh toán</p>
                            <p className="text-3xl font-bold text-primary">
                                {formatPrice(paymentData.amount)}
                            </p>
                        </div>

                        {/* QR Code */}
                        <div className="flex flex-col items-center">
                            <div className="bg-white p-4 rounded-2xl shadow-lg border-2 border-primary/20">
                                <img
                                    src={paymentData.qrCodeUrl}
                                    alt="QR Code thanh toán"
                                    className="w-64 h-64 object-contain"
                                    onError={(e) => {
                                        (e.target as HTMLImageElement).style.display = "none";
                                    }}
                                />
                            </div>
                            <p className="text-sm text-muted-foreground mt-3 flex items-center gap-1">
                                <QrCode className="h-4 w-4" />
                                Mở app ngân hàng và quét mã QR
                            </p>
                        </div>

                        <Separator />

                        {/* Bank Transfer Info */}
                        <div className="space-y-3">
                            <h4 className="font-semibold text-sm flex items-center gap-2">
                                <CreditCard className="h-4 w-4 text-primary" />
                                Hoặc chuyển khoản thủ công
                            </h4>

                            {/* Bank Name */}
                            <Card className="border-dashed">
                                <CardContent className="p-3 flex items-center justify-between">
                                    <div>
                                        <p className="text-xs text-muted-foreground">Ngân hàng</p>
                                        <p className="font-medium">{paymentData.bankName}</p>
                                    </div>
                                </CardContent>
                            </Card>

                            {/* Account Number */}
                            <Card className="border-dashed">
                                <CardContent className="p-3 flex items-center justify-between">
                                    <div>
                                        <p className="text-xs text-muted-foreground">Số tài khoản</p>
                                        <p className="font-mono font-bold text-lg">
                                            {paymentData.bankAccountNumber}
                                        </p>
                                    </div>
                                    <Button
                                        variant="ghost"
                                        size="sm"
                                        onClick={() =>
                                            copyToClipboard(paymentData.bankAccountNumber, "Số tài khoản")
                                        }
                                    >
                                        {copied === "Số tài khoản" ? (
                                            <CheckCircle2 className="h-4 w-4 text-green-500" />
                                        ) : (
                                            <Copy className="h-4 w-4" />
                                        )}
                                    </Button>
                                </CardContent>
                            </Card>

                            {/* Account Name */}
                            <Card className="border-dashed">
                                <CardContent className="p-3 flex items-center justify-between">
                                    <div>
                                        <p className="text-xs text-muted-foreground">Tên tài khoản</p>
                                        <p className="font-medium">{paymentData.bankAccountName}</p>
                                    </div>
                                </CardContent>
                            </Card>

                            {/* Amount */}
                            <Card className="border-dashed">
                                <CardContent className="p-3 flex items-center justify-between">
                                    <div>
                                        <p className="text-xs text-muted-foreground">Số tiền</p>
                                        <p className="font-bold text-primary text-lg">
                                            {formatPrice(paymentData.amount)}
                                        </p>
                                    </div>
                                    <Button
                                        variant="ghost"
                                        size="sm"
                                        onClick={() =>
                                            copyToClipboard(
                                                paymentData.amount.toString(),
                                                "Số tiền"
                                            )
                                        }
                                    >
                                        {copied === "Số tiền" ? (
                                            <CheckCircle2 className="h-4 w-4 text-green-500" />
                                        ) : (
                                            <Copy className="h-4 w-4" />
                                        )}
                                    </Button>
                                </CardContent>
                            </Card>

                            {/* Payment Content */}
                            <Card className="border-primary/30 bg-primary/5">
                                <CardContent className="p-3 flex items-center justify-between">
                                    <div>
                                        <p className="text-xs text-muted-foreground">
                                            Nội dung chuyển khoản
                                        </p>
                                        <p className="font-mono font-bold text-primary text-lg">
                                            {paymentData.paymentContent}
                                        </p>
                                    </div>
                                    <Button
                                        variant="ghost"
                                        size="sm"
                                        onClick={() =>
                                            copyToClipboard(
                                                paymentData.paymentContent,
                                                "Nội dung CK"
                                            )
                                        }
                                    >
                                        {copied === "Nội dung CK" ? (
                                            <CheckCircle2 className="h-4 w-4 text-green-500" />
                                        ) : (
                                            <Copy className="h-4 w-4" />
                                        )}
                                    </Button>
                                </CardContent>
                            </Card>
                        </div>

                        {/* Status Indicator */}
                        <div className="flex items-center justify-center gap-2 p-3 bg-yellow-50 dark:bg-yellow-900/10 rounded-lg border border-yellow-200 dark:border-yellow-800">
                            <RefreshCw className="h-4 w-4 animate-spin text-yellow-600" />
                            <span className="text-sm text-yellow-700 dark:text-yellow-400">
                                Đang chờ thanh toán... Tự động cập nhật khi nhận được tiền
                            </span>
                        </div>

                        {/* Warning */}
                        <p className="text-xs text-muted-foreground text-center">
                            ⚠️ Vui lòng nhập đúng{" "}
                            <span className="font-bold">nội dung chuyển khoản</span> để hệ
                            thống tự động xác nhận đơn hàng
                        </p>
                    </div>
                ) : (
                    <div className="flex flex-col items-center justify-center py-12">
                        <p className="text-muted-foreground">Không có dữ liệu thanh toán</p>
                        <Button onClick={createPayment} className="mt-4">
                            Thử lại
                        </Button>
                    </div>
                )}
            </DialogContent>
        </Dialog>
    );
};
