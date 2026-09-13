import { useState, useEffect, useRef, useCallback } from "react";
// usePayOS của thư viện không phải React hook: mỗi lần gọi chỉ tạo một đối tượng checkout mới.
// Đổi tên khi import để gọi được trong effect, và giữ đúng một đối tượng cho mỗi link.
import { usePayOS as createPayOSCheckout } from "@payos/payos-checkout";
import { PaymentsApi } from "@/features/orders/api";
import type { PaymentResponse } from "@/features/orders/types";
import { getErrorMessage } from "@/utils/errors";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import {
    CheckCircle2,
    Loader2,
    Banknote,
    RefreshCw,
    XCircle,
} from "lucide-react";
import { useToast } from "@/hooks/use-toast";

interface PaymentDialogProps {
    isOpen: boolean;
    onClose: () => void;
    orderId: string | null;
    onPaymentSuccess?: () => void;
}

// Khung checkout payOS phía trong trang
const PAYOS_ELEMENT_ID = "payos-embedded-checkout";
const POLL_INTERVAL_MS = 5000;

// idle: đang hiện trang payOS; confirming: payOS báo đã trả, chờ backend xác nhận;
// closed: khách đã huỷ hoặc đóng trang payOS
type CheckoutState = "idle" | "confirming" | "closed";

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
    const [checkoutState, setCheckoutState] = useState<CheckoutState>("idle");
    const [cancelled, setCancelled] = useState(false);
    const pollingRef = useRef<ReturnType<typeof setInterval> | null>(null);
    const paidRef = useRef(false);

    // Parent truyền callback mới mỗi lần render. Giữ qua ref để các callback bên dưới ổn định,
    // nếu không effect nhúng checkout sẽ chạy lại và tải lại iframe payOS giữa chừng.
    const onPaymentSuccessRef = useRef(onPaymentSuccess);
    useEffect(() => {
        onPaymentSuccessRef.current = onPaymentSuccess;
    }, [onPaymentSuccess]);

    const stopPolling = useCallback(() => {
        if (pollingRef.current) {
            clearInterval(pollingRef.current);
            pollingRef.current = null;
        }
    }, []);

    const markPaid = useCallback(() => {
        if (paidRef.current) return;
        paidRef.current = true;
        stopPolling();
        setIsPaid(true);
        setPaymentData((prev) => (prev ? { ...prev, paymentStatus: "PAID" } : prev));
        toast({
            title: "🎉 Thanh toán thành công!",
            description: "Đơn hàng của bạn đã được thanh toán.",
        });
        onPaymentSuccessRef.current?.();
    }, [stopPolling, toast]);

    // Nguồn sự thật duy nhất là backend (webhook hoặc đối soát với payOS),
    // không phải sự kiện phía trình duyệt.
    const checkStatus = useCallback(async (id: string) => {
        try {
            const response = await PaymentsApi.checkPaymentStatus(id);
            if (response?.result?.paymentStatus === "PAID") {
                markPaid();
            }
        } catch (error) {
            console.error("Error checking payment status:", error);
        }
    }, [markPaid]);

    const startPolling = useCallback((id: string) => {
        stopPolling();
        pollingRef.current = setInterval(() => checkStatus(id), POLL_INTERVAL_MS);
    }, [checkStatus, stopPolling]);

    const createPayment = useCallback(async () => {
        if (!orderId) return;
        setIsCreating(true);
        setCheckoutState("idle");
        setCancelled(false);
        try {
            const response = await PaymentsApi.createPayment(orderId);
            const result = response?.result;
            if (!result) return;

            setPaymentData(result);
            if (result.paymentStatus === "PAID") {
                markPaid();
            } else {
                startPolling(orderId);
            }
        } catch (error: unknown) {
            toast({
                variant: "destructive",
                title: "Lỗi tạo thanh toán",
                description: getErrorMessage(error, "Không thể tạo link thanh toán"),
            });
        } finally {
            setIsCreating(false);
        }
    }, [orderId, markPaid, startPolling, toast]);

    // Tạo link thanh toán khi mở dialog, dọn dẹp khi đóng
    useEffect(() => {
        if (isOpen && orderId) {
            paidRef.current = false;
            createPayment();
        }
        if (!isOpen) {
            setPaymentData(null);
            setIsPaid(false);
            setIsCreating(false);
            setCheckoutState("idle");
            setCancelled(false);
        }
        return stopPolling;
        // Chỉ chạy lại khi mở/đóng dialog hoặc đổi đơn, không phải mỗi khi callback đổi
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [isOpen, orderId]);

    const checkoutUrl = isPaid || isCreating ? undefined : paymentData?.checkoutUrl;
    const showCheckout = Boolean(checkoutUrl) && checkoutState === "idle";

    // Nhúng trang checkout payOS vào khung
    useEffect(() => {
        if (!checkoutUrl || !showCheckout || !orderId) return;

        const checkout = createPayOSCheckout({
            RETURN_URL: window.location.href,
            ELEMENT_ID: PAYOS_ELEMENT_ID,
            CHECKOUT_URL: checkoutUrl,
            embedded: true,
            onSuccess: () => {
                setCheckoutState("confirming");
                checkStatus(orderId);
            },
            onCancel: () => {
                setCancelled(true);
                setCheckoutState("closed");
            },
            onExit: () => {
                setCheckoutState("closed");
            },
        });
        checkout.open();

        return () => {
            // Thư viện tự gỡ iframe khi khách trả/huỷ/thoát; chỉ gỡ khi còn iframe để tránh log lỗi
            if (document.getElementById(PAYOS_ELEMENT_ID)?.querySelector("iframe")) {
                checkout.exit();
            }
        };
    }, [checkoutUrl, showCheckout, orderId, checkStatus]);

    if (!isOpen) return null;

    return (
        <Dialog open={isOpen} onOpenChange={onClose}>
            <DialogContent className="max-w-lg max-h-[95vh] overflow-y-auto">
                <DialogHeader>
                    <DialogTitle className="flex items-center gap-2 text-2xl">
                        <Banknote className="h-6 w-6 text-primary" />
                        Thanh Toán Chuyển Khoản
                    </DialogTitle>
                    <DialogDescription>
                        Quét mã QR bằng app ngân hàng để thanh toán qua payOS
                    </DialogDescription>
                </DialogHeader>

                {isCreating ? (
                    <div className="flex flex-col items-center justify-center py-12">
                        <Loader2 className="h-12 w-12 animate-spin text-primary mb-4" />
                        <p className="text-muted-foreground">Đang tạo link thanh toán...</p>
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
                ) : paymentData?.checkoutUrl ? (
                    <div className="space-y-4">
                        {/* Amount Display */}
                        <div className="text-center p-4 bg-primary/5 rounded-xl border border-primary/20">
                            <p className="text-sm text-muted-foreground mb-1">
                                Đơn <span className="font-mono font-semibold">{paymentData.orderCode}</span>
                            </p>
                            <p className="text-3xl font-bold text-primary">
                                {formatPrice(paymentData.amount)}
                            </p>
                        </div>

                        {checkoutState === "idle" && (
                            // Iframe payOS cao 100% khung chứa nên khung phải có chiều cao cố định
                            <div
                                id={PAYOS_ELEMENT_ID}
                                className="h-[600px] max-h-[65vh] w-full overflow-hidden rounded-xl border"
                            />
                        )}

                        {checkoutState === "confirming" && (
                            <div className="flex flex-col items-center justify-center py-10 text-center">
                                <Loader2 className="h-10 w-10 animate-spin text-primary mb-4" />
                                <p className="font-medium">Đang xác nhận thanh toán...</p>
                                <p className="text-sm text-muted-foreground">
                                    Thường chỉ mất vài giây, vui lòng không đóng cửa sổ này
                                </p>
                            </div>
                        )}

                        {checkoutState === "closed" && (
                            <div className="flex flex-col items-center justify-center py-8 text-center">
                                <XCircle className="h-10 w-10 text-muted-foreground mb-4" />
                                <p className="font-medium mb-1">
                                    {cancelled ? "Bạn đã huỷ thanh toán" : "Đã đóng trang thanh toán"}
                                </p>
                                <p className="text-sm text-muted-foreground mb-6">
                                    Đơn hàng vẫn được giữ, bạn có thể thanh toán lại ngay
                                </p>
                                <div className="flex gap-3">
                                    <Button variant="outline" onClick={onClose}>
                                        Để sau
                                    </Button>
                                    <Button onClick={createPayment}>
                                        Thanh toán lại
                                    </Button>
                                </div>
                            </div>
                        )}

                        {checkoutState !== "closed" && (
                            <div className="flex items-center justify-center gap-2 p-3 bg-yellow-50 dark:bg-yellow-900/10 rounded-lg border border-yellow-200 dark:border-yellow-800">
                                <RefreshCw className="h-4 w-4 animate-spin text-yellow-600" />
                                <span className="text-sm text-yellow-700 dark:text-yellow-400">
                                    Tự động cập nhật khi nhận được tiền
                                </span>
                            </div>
                        )}
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
