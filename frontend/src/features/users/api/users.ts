import { http } from "@/features/auth/api/auth";
import { simpleHttp } from "@/services/axios";
import type {
  ApiResponse,
  PageResponse,
  PaginationParams,
} from "@/types";
import type {
  UserResponse,
  UserCreationRequest,
  UserUpdateRequest,
} from "../types";

export const UsersApi = {
  getAll: (params?: PaginationParams) => http.request<ApiResponse<PageResponse<UserResponse>>>(`/users${buildQuery(params)}`),
  getById: (userId: string) =>
    http.request<ApiResponse<UserResponse>>(`/users/${userId}`),
  getMyInfo: () => http.request<ApiResponse<UserResponse>>("/users/my-info"),
  create: (payload: UserCreationRequest) =>
    http.request<ApiResponse<UserResponse>>("/users", {
      method: "POST",
      body: payload,
    }),
  update: (userId: string, payload: UserUpdateRequest) =>
    http.request<ApiResponse<UserResponse>>(`/users/${userId}`, {
      method: "PUT",
      body: payload,
    }),
  delete: (userId: string) =>
    http.request<ApiResponse<void>>(`/users/${userId}`, {
      method: "DELETE",
    }),
};

function buildQuery(params?: Record<string, any>): string {
  if (!params) return "";
  const usp = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v === undefined || v === null || v === "") return;
    usp.append(k, String(v));
  });
  const qs = usp.toString();
  return qs ? `?${qs}` : "";
}
