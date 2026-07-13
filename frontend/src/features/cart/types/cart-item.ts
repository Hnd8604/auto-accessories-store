// Cart Item Response Types
export interface CartItemResponse {
  id: number;
  cartId: number;
  productId: number;
  productName?: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
}

// Cart Item Request Types
export interface CartItemRequest {
  cartId: number;
  productId: number;
  quantity: number;
}

export interface CartItemUpdateRequest {
  quantity: number;
}
