import { http } from "@/features/auth/api/auth";
import type { ApiResponse, PageResponse, PaginationParams } from "@/types";
import type { ProductRequest, ProductResponse, ProductSearchRequest } from "../types";

export const ProductsApi = {
  getAll: (params?: PaginationParams) =>
    http.request<ApiResponse<PageResponse<ProductResponse>>>(
      "/products",
      { params }
    ),
  getById: (id: number) =>
    http.request<ApiResponse<ProductResponse>>(`/products/id/${id}`),
  getBySlug: (slug: string) =>
    http.request<ApiResponse<ProductResponse>>(`/products/slug/${slug}`),
  create: (payload: ProductRequest) =>
    http.request<ApiResponse<ProductResponse>>("/products", {
      method: "POST",
      body: payload,
    }),
  update: (id: number, payload: ProductRequest) =>
    http.request<ApiResponse<ProductResponse>>(`/products/${id}`, {
      method: "PUT",
      body: payload,
    }),
  delete: (id: number) =>
    http.request<ApiResponse<void>>(`/products/${id}`, {
      method: "DELETE",
    }),
  byCategory: (categoryId: number, params?: PaginationParams) =>
    http.request<ApiResponse<PageResponse<ProductResponse>>>(
      `/products/categories/${categoryId}`,
      { params }
    ),
  byBrand: (brandId: number, params?: PaginationParams) =>
    http.request<ApiResponse<PageResponse<ProductResponse>>>(
      `/products/brands/${brandId}`,
      { params }
    ),
  search: (searchRequest: ProductSearchRequest, params?: PaginationParams) =>
    http.request<ApiResponse<PageResponse<ProductResponse>>>(
      "/products/search",
      {
        method: "POST",
        body: searchRequest,
        params,
      }
    ),
};
