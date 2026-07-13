import { http } from "@/features/auth/api/auth";
import type { ApiResponse } from "@/types";
import type { RoleRequest, RoleResponse } from "../types";

export const RolesApi = {
  getAll: () => http.request<ApiResponse<RoleResponse[]>>("/roles"),
  getById: (id: string) => http.request<ApiResponse<RoleResponse>>(`/roles/${id}`),
  create: (payload: RoleRequest) => http.request<ApiResponse<RoleResponse>>("/roles", { method: "POST", body: payload }),
  update: (id: string, payload: RoleRequest) => http.request<ApiResponse<RoleResponse>>(`/roles/${id}`, { method: "PUT", body: payload }),
  delete: (id: string) => http.request<ApiResponse<void>>(`/roles/${id}`, { method: "DELETE" }),
  addPermissions: (roleId: string, permissionNames: string[]) => 
    http.request<ApiResponse<RoleResponse>>(`/roles/${roleId}/permissions`, { 
      method: "POST", 
      body: permissionNames 
    }),
  removePermissions: (roleId: string, permissionNames: string[]) => 
    http.request<ApiResponse<RoleResponse>>(`/roles/${roleId}/permissions`, { 
      method: "DELETE", 
      body: permissionNames 
    }),
};


