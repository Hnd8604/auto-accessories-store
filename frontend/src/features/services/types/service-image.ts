export interface ServiceImageResponse {
  id: number;
  serviceId: number;
  imageUrl: string;
  altText?: string;
  isPrimary: boolean;
  sortOrder: number;
}

export interface ServiceImageUpdateRequest {
  altText?: string;
  isPrimary: boolean;
  sortOrder: number;
}
