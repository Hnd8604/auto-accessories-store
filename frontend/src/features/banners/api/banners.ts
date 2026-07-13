import { http } from "@/features/auth/api/auth";
import type { ApiResponse } from "@/types";
import type { BannerResponse, BannerRequest } from "../types";

export const BannersApi = {
  getAll: () => http.request<ApiResponse<BannerResponse[]>>("/banners"),
  
  getActive: () => http.request<ApiResponse<BannerResponse[]>>("/banners/active"),

  getById: (id: number) =>
    http.request<ApiResponse<BannerResponse>>(`/banners/${id}`),

  create: (file: File, banner: BannerRequest) => {
    const formData = new FormData();
    formData.append("file", file);
    formData.append("banner", new Blob([JSON.stringify(banner)], { type: "application/json" }));
    
    return http.request<ApiResponse<BannerResponse>>("/banners", {
      method: "POST",
      body: formData,
      headers: {
        // Let browser set Content-Type with boundary for multipart
      },
    });
  },

  update: (id: number, banner: BannerRequest, file?: File) => {
    const formData = new FormData();
    if (file) {
      formData.append("file", file);
    }
    formData.append("banner", new Blob([JSON.stringify(banner)], { type: "application/json" }));
    
    return http.request<ApiResponse<BannerResponse>>(`/banners/${id}`, {
      method: "PUT",
      body: formData,
      headers: {
        // Let browser set Content-Type with boundary for multipart
      },
    });
  },

  delete: (id: number) =>
    http.request<ApiResponse<void>>(`/banners/${id}`, {
      method: "DELETE",
    }),
};
