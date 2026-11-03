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
// Backend luôn thêm vào giỏ của user đang đăng nhập, không nhận cartId
export interface CartItemRequest {
  productId: number;
  quantity: number;
}

export interface CartItemUpdateRequest {
  quantity: number;
}
