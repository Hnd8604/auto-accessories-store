// Category Response Types
export interface CategoryResponse {
  id: string;
  name: string;
  description?: string;
  slug?: string;
  productCount?: number;
}

// Category Request Types
export interface CategoryRequest {
  name: string;
  description?: string;
}

export interface CategoryBrandsRequest {
  brandIds: number[];
}
