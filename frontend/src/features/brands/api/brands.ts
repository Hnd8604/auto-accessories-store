import { http } from "@/features/auth/api/auth";
import type { ApiResponse } from "@/types";
import type { BrandRequest, BrandResponse } from "@/features/products/types";

export const BrandsApi = {
  getAll: () => http.request<ApiResponse<BrandResponse[]>>("/brands"),
  getById: (id: number) =>
    http.request<ApiResponse<BrandResponse>>(`/brands/id/${id}`),
  getBySlug: (slug: string) =>
    http.request<ApiResponse<BrandResponse>>(`/brands/slug/${slug}`),
  create: (payload: BrandRequest) =>
    http.request<ApiResponse<BrandResponse>>("/brands", {
      method: "POST",
      body: payload,
    }),
  update: (id: number, payload: BrandRequest) =>
    http.request<ApiResponse<BrandResponse>>(`/brands/${id}`, {
      method: "PUT",
      body: payload,
    }),
  delete: (id: number) =>
    http.request<ApiResponse<void>>(`/brands/${id}`, {
      method: "DELETE",
    }),
};
