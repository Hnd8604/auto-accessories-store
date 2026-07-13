import type { OrderStatus, PaymentStatus, PaymentMethod } from './order-status';
import type { OrderDetailResponse, OrderDetailRequest } from './order-detail';

// Order Response Types
export interface OrderResponse {
  id: string;
  userId: string;
  orderCode?: string;
  totalPrice: number;
  nameRecipient?: string;
  phoneRecipient?: string;
  addressRecipient?: string;
  note?: string;
  status?: OrderStatus;
  paymentMethod?: PaymentMethod;
  paymentStatus?: PaymentStatus;
  createdAt?: string;
  orderDetails?: OrderDetailResponse[];
}

// Payment Response Types
export interface PaymentResponse {
  orderId: string;
  orderCode: string;
  amount: number;
  qrCodeUrl: string;
  bankName: string;
  bankAccountNumber: string;
  bankAccountName: string;
  paymentContent: string;
  paymentStatus: PaymentStatus;
}

// Order Request Types
export interface OrderCreationRequest {
  userId: string;
  nameRecipient?: string;
  phoneRecipient?: string;
  addressRecipient?: string;
  note?: string;
  paymentMethod?: PaymentMethod;
  orderDetails?: OrderDetailRequest[];
}

export interface OrderUpdateByUserRequest {
  nameRecipient?: string;
  phoneRecipient?: string;
  addressRecipient?: string;
  note?: string;
}

export interface OrderUpdateByAdminRequest {
  paymentStatus?: PaymentStatus;
  orderStatus?: OrderStatus;
}

export interface OrderUpdateStatusRequest {
  paymentStatus?: PaymentStatus;
  orderStatus?: OrderStatus;
}

