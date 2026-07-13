// Banner Response Types
export interface BannerResponse {
  id: number;
  title?: string;
  subtitle?: string;
  imageUrl: string;
  redirectUrl?: string;
  altText?: string;
  buttonText?: string;
  displayOrder?: number;
  isActive: boolean;
  createdAt?: string;
  updatedAt?: string;
}

// Banner Request Types
export interface BannerRequest {
  title?: string;
  subtitle?: string;
  redirectUrl?: string;
  altText?: string;
  buttonText?: string;
  displayOrder?: number;
  isActive?: boolean;
}
