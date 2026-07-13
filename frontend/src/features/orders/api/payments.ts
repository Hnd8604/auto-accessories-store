import { http } from "@/features/auth/api/auth";
import type { ApiResponse } from "@/types";
import type { PaymentResponse } from "../types";

export const PaymentsApi = {
    // Tạo QR thanh toán cho đơn hàng
    createPayment: (orderId: string) =>
        http.request<ApiResponse<PaymentResponse>>(`/payments/${orderId}/create`, {
            method: "POST",
        }),

    // Kiểm tra trạng thái thanh toán
    checkPaymentStatus: (orderId: string) =>
        http.request<ApiResponse<PaymentResponse>>(`/payments/${orderId}/status`),
};
