// Order Detail Response Types
export interface OrderDetailResponse {
  id: string;
  orderId: string;
  productId: string;
  productName?: string;
  unitPrice?: number;
  productImage?: string;
  quantity: number;
}

// Order Detail Request Types
export interface OrderDetailRequest {
  productId: number;
  quantity: number;
}
