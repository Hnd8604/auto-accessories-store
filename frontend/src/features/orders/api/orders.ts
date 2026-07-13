import { http } from "@/features/auth/api/auth";
import type { ApiResponse, PageResponse, PaginationParams } from "@/types";
import type {
  OrderCreationRequest,
  OrderResponse,
  OrderUpdateByUserRequest,
  OrderUpdateByAdminRequest,
} from "../types";

export const OrdersApi = {
  getAll: (params?: PaginationParams) =>
    http.request<ApiResponse<PageResponse<OrderResponse>>>(
      "/orders",
      params ? { params } : undefined
    ),
  getMyOrders: () =>
    http.request<ApiResponse<OrderResponse[]>>("/orders/my-order"),
  getById: (orderId: string) =>
    http.request<ApiResponse<OrderResponse>>(`/orders/${orderId}`),
  createFromCart: (payload: OrderCreationRequest) =>
    http.request<ApiResponse<OrderResponse>>("/orders", {
      method: "POST",
      body: payload,
    }),
  updateByUser: (orderId: string, payload: OrderUpdateByUserRequest) =>
    http.request<ApiResponse<OrderResponse>>(
      `/orders/${orderId}/update-by-user`,
      { method: "PUT", body: payload }
    ),
  updateByAdmin: (orderId: string, payload: OrderUpdateByAdminRequest) =>
    http.request<ApiResponse<OrderResponse>>(
      `/orders/${orderId}/update-by-admin`,
      { method: "PUT", body: payload }
    ),
  cancel: (orderId: string) =>
    http.request<ApiResponse<OrderResponse>>(`/orders/${orderId}/cancel`, {
      method: "PUT",
    }),
  delete: (orderId: string) =>
    http.request<ApiResponse<void>>(`/orders/${orderId}`, {
      method: "DELETE",
    }),
};


