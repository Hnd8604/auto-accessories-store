import { http } from "@/features/auth/api/auth";
import type { ApiResponse } from "@/types";
import type { ServiceRequest, ServiceResponse } from "../types";

export const ServicesApi = {
  getAll: () =>
    http.request<ApiResponse<ServiceResponse[]>>("/services"),

  getById: (id: number) =>
    http.request<ApiResponse<ServiceResponse>>(`/services/${id}`),

  getBySlug: (slug: string) =>
    http.request<ApiResponse<ServiceResponse>>(`/services/slug/${slug}`),

  create: (payload: ServiceRequest) =>
    http.request<ApiResponse<ServiceResponse>>("/services", {
      method: "POST",
      body: payload,
    }),

  update: (id: number, payload: ServiceRequest) =>
    http.request<ApiResponse<ServiceResponse>>(`/services/${id}`, {
      method: "PUT",
      body: payload,
    }),

  delete: (id: number) =>
    http.request<ApiResponse<void>>(`/services/${id}`, {
      method: "DELETE",
    }),
};
