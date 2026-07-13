import { http } from "@/features/auth/api/auth";
import type { ApiResponse } from "@/types";
import type { PermissionResponse, PermissionRequest } from "../types";

export const PermissionsApi = {
  getAll: () => http.request<ApiResponse<PermissionResponse[]>>("/permissions"),
  create: (payload: PermissionRequest) => http.request<ApiResponse<PermissionResponse>>("/permissions", { method: "POST", body: payload }),
  update: (id: string, payload: PermissionRequest) => http.request<ApiResponse<PermissionResponse>>(`/permissions/${id}`, { method: "PUT", body: payload }),
  delete: (id: string) => http.request<ApiResponse<void>>(`/permissions/${id}`, { method: "DELETE" }),
};


