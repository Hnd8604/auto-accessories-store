import { http } from "@/features/auth/api/auth";
import type { ApiResponse } from "@/types";
import type { PostCategoryRequest, PostCategoryResponse } from "../types";

export const PostCategoriesApi = {
  getAll: () =>
    http.request<ApiResponse<PostCategoryResponse[]>>("/post-categories"),
  getById: (id: number) =>
    http.request<ApiResponse<PostCategoryResponse>>(`/post-categories/id/${id}`),
  getBySlug: (slug: string) =>
    http.request<ApiResponse<PostCategoryResponse>>(
      `/post-categories/slug/${slug}`
    ),
  create: (payload: PostCategoryRequest) =>
    http.request<ApiResponse<PostCategoryResponse>>("/post-categories", {
      method: "POST",
      body: payload,
    }),
  update: (id: number, payload: PostCategoryRequest) =>
    http.request<ApiResponse<PostCategoryResponse>>(`/post-categories/${id}`, {
      method: "PUT",
      body: payload,
    }),
  delete: (id: number) =>
    http.request<ApiResponse<void>>(`/post-categories/${id}`, {
      method: "DELETE",
    }),
};
