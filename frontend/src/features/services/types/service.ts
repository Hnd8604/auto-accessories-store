export interface ServiceResponse {
  id: number;
  name: string;
  shortDescription?: string;
  fullDescription?: string;
  features: string[];
  slug: string;
  displayOrder?: number;
  primaryImageUrl?: string;
}

export interface ServiceRequest {
  name: string;
  shortDescription?: string;
  fullDescription?: string;
  features: string[];
  displayOrder?: number;
}
