// Brand Response Types
export interface BrandResponse {
  id: number;
  name: string;
  description?: string;
  slug?: string;
  productCount?: number;
}

// Brand Request Types
export interface BrandRequest {
  name: string;
  description?: string;
}
