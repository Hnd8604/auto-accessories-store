import { http } from "@/features/auth/api/auth";
import type {
  ApiResponse,
  PageResponse,
  PaginationParams,
} from "@/types";
import type { ProductImageRequest, ProductImageResponse, ProductImageUpdateRequest } from "../types";

export const ProductImagesApi = {
  getAll: (params?: PaginationParams) =>
    http.request<ApiResponse<PageResponse<ProductImageResponse>>>(
      `/product-images${buildQuery(params)}`
    ),
  getById: (id: number) =>
    http.request<ApiResponse<ProductImageResponse>>(
      `/product-images/${id}`
    ),
  getByProductId: (productId: number) =>
    http.request<ApiResponse<ProductImageResponse[]>>(
      `/product-images/products/${productId}`
    ),
  // Create product image - only supports file, productId, isPrimary
  create: (file: File, productId: number, isPrimary: boolean = false) => {
    const formData = new FormData();
    formData.append("file", file);

    const params = new URLSearchParams();
    params.append("productId", productId.toString());
    params.append("isPrimary", isPrimary.toString());

    return http.request<ApiResponse<ProductImageResponse>>(
      `/product-images?${params.toString()}`,
      {
        method: "POST",
        body: formData,
      }
    );
  },

  // Create with full metadata using upload + update approach
  createWithMetadata: async (
    file: File,
    productId: number,
    isPrimary: boolean = false,
    altText?: string,
    sortOrder?: number
  ) => {
    // First upload file with basic info
    const createResponse = await ProductImagesApi.create(
      file,
      productId,
      isPrimary
    );

    // Then update with metadata if provided
    if (createResponse.result && (altText || sortOrder !== undefined)) {
      const updateData: ProductImageUpdateRequest = {
        imageUrl: createResponse.result.imageUrl,
        altText: altText || "",
        isPrimary: isPrimary,
        sortOrder: sortOrder !== undefined ? sortOrder : 0,
      };

      return await ProductImagesApi.update(
        createResponse.result.id,
        updateData
      );
    }

    return createResponse;
  },
  update: (id: number, payload: ProductImageUpdateRequest) =>
    http.request<ApiResponse<ProductImageResponse>>(
      `/product-images/${id}`,
      {
        method: "PUT",
        body: payload,
      }
    ),
  delete: (id: number) =>
    http.request<ApiResponse<void>>(`/product-images/${id}`, {
      method: "DELETE",
    }),
  setPrimary: (productId: number, imageId: number) =>
    http.request<ApiResponse<void>>(
      `/product-images/products/${productId}/images/${imageId}/set-primary`,
      { method: "POST" }
    ),
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
