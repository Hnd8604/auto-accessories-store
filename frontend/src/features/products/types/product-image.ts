// Product Image Response Types
export interface ProductImageResponse {
  id: number;
  productId: number;
  imageUrl: string;
  altText?: string;
  isPrimary: boolean;
  sortOrder: number;
}

// Product Image Request Types
export interface ProductImageRequest {
  productId: number;
  altText?: string;
  isPrimary: boolean;
  sortOrder: number;
}

export interface ProductImageUpdateRequest {
  altText?: string;
  isPrimary: boolean;
  sortOrder: number;
}
