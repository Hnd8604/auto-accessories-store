// Post Category Response Types
export interface PostCategoryResponse {
  id: number;
  name: string;
  slug: string;
  description?: string;
  createdAt?: string;
  updatedAt?: string;
  postCount?: number;
}

// Post Category Request Types
export interface PostCategoryRequest {
  name: string;
  description?: string;
}
