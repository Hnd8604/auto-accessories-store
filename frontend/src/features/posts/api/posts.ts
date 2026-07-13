import { http } from "@/features/auth/api/auth";
import type { ApiResponse, PageResponse, PaginationParams } from "@/types";
import type { PostRequest, PostResponse } from "../types";

export const PostsApi = {
  getAll: (params?: PaginationParams) =>
    http.request<ApiResponse<PageResponse<PostResponse>>>(
      "/posts",
      params ? { params } : undefined
    ),
  getById: (id: number) =>
    http.request<ApiResponse<PostResponse>>(`/posts/id/${id}`),
  getBySlug: (slug: string) =>
    http.request<ApiResponse<PostResponse>>(`/posts/slug/${slug}`),
  getPublished: (params?: PaginationParams) =>
    http.request<ApiResponse<PageResponse<PostResponse>>>(
      "/posts/published",
      params ? { params } : undefined
    ),
  getByCategorySlug: (slug: string, params?: PaginationParams) =>
    http.request<ApiResponse<PageResponse<PostResponse>>>(
      `/posts/category/${slug}`,
      params ? { params } : undefined
    ),
  create: (file: File, post: PostRequest) => {
    const formData = new FormData();
    formData.append("file", file);
    formData.append("post", new Blob([JSON.stringify(post)], { type: "application/json" }));
    
    return http.request<ApiResponse<PostResponse>>("/posts", {
      method: "POST",
      body: formData,
      headers: {
        // Let browser set Content-Type with boundary for multipart
      },
    });
  },
  update: (id: number, file: File | null, post: PostRequest) => {
    const formData = new FormData();
    if (file) {
      formData.append("file", file);
    }
    formData.append("post", new Blob([JSON.stringify(post)], { type: "application/json" }));
    
    return http.request<ApiResponse<PostResponse>>(`/posts/${id}`, {
      method: "PUT",
      body: formData,
      headers: {
        // Let browser set Content-Type with boundary for multipart
      },
    });
  },
  delete: (id: number) =>
    http.request<ApiResponse<void>>(`/posts/${id}`, {
      method: "DELETE",
    }),
  incrementViewCount: (id: number) =>
    http.request<ApiResponse<void>>(`/posts/${id}/view`, {
      method: "POST",
    }),
};
