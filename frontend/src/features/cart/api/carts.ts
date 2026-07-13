import { http } from "@/features/auth/api/auth";
import type { ApiResponse } from "@/types";
import type {
  CartRequest,
  CartResponse,
  CartCreationResponse,
  CartItemRequest,
  CartItemUpdateRequest,
  CartItemResponse,
} from "../types";

export const CartsApi = {
  getMyCart: () => http.request<ApiResponse<CartResponse>>("/carts/my-cart"),
  getById: (cartId: number) => http.request<ApiResponse<CartResponse>>(`/carts/${cartId}`),
  getItems: (cartId: number) => http.request<ApiResponse<CartItemResponse[]>>(`/carts/${cartId}/items`),
  addItem: (payload: CartItemRequest) => http.request<ApiResponse<CartItemResponse>>("/carts/items", { method: "POST", body: payload }),
  updateItem: (itemId: number, payload: CartItemUpdateRequest) => http.request<ApiResponse<CartItemResponse>>(`/carts/items/${itemId}`, { method: "PUT", body: payload }),
  removeItem: (cartId: number, itemId: number) => http.request<ApiResponse<void>>(`/carts/${cartId}/items/${itemId}`, { method: "DELETE" }),
};


