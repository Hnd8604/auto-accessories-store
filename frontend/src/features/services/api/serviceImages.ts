import { http } from "@/features/auth/api/auth";
import type { ApiResponse } from "@/types";
import type { ServiceImageResponse, ServiceImageUpdateRequest } from "../types";

export const ServiceImagesApi = {
  getByServiceId: (serviceId: number) =>
    http.request<ApiResponse<ServiceImageResponse[]>>(
      `/service-images/services/${serviceId}`
    ),

  create: (file: File, serviceId: number, isPrimary: boolean = false) => {
    const formData = new FormData();
    formData.append("file", file);

    const params = new URLSearchParams();
    params.append("serviceId", serviceId.toString());
    params.append("isPrimary", isPrimary.toString());

    return http.request<ApiResponse<ServiceImageResponse>>(
      `/service-images?${params.toString()}`,
      { method: "POST", body: formData }
    );
  },

  update: (id: number, payload: ServiceImageUpdateRequest) =>
    http.request<ApiResponse<ServiceImageResponse>>(`/service-images/${id}`, {
      method: "PUT",
      body: payload,
    }),

  delete: (id: number) =>
    http.request<ApiResponse<void>>(`/service-images/${id}`, {
      method: "DELETE",
    }),

  setPrimary: (serviceId: number, imageId: number) =>
    http.request<ApiResponse<void>>(
      `/service-images/services/${serviceId}/images/${imageId}/set-primary`,
      { method: "POST" }
    ),
};
