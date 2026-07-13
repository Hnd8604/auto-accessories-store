// Notification types matching backend DTOs

export enum NotificationType {
  ORDER_CREATED = "ORDER_CREATED",
  ORDER_STATUS_CHANGED = "ORDER_STATUS_CHANGED",
  ORDER_CANCELED = "ORDER_CANCELED",
  PAYMENT_RECEIVED = "PAYMENT_RECEIVED",
  SYSTEM = "SYSTEM",
}

export interface NotificationResponse {
  id: string;
  title: string;
  message: string;
  type: NotificationType;
  referenceId: string | null;
  isRead: boolean;
  createdAt: string; // ISO date string from backend
}
