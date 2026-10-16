import { http } from "@/services/http";
import type { ApiResponse, PageResponse } from "@/types";
import type { NotificationResponse } from "../types";

export const NotificationsApi = {
  getMyNotifications: (page = 0, size = 10) =>
    http.request<ApiResponse<PageResponse<NotificationResponse>>>("/notifications", {
      params: { page, size },
    }),

  getUnreadCount: () =>
    http.request<ApiResponse<number>>("/notifications/unread-count"),

  markAsRead: (notificationId: string) =>
    http.request<ApiResponse<void>>(`/notifications/${notificationId}/read`, {
      method: "PUT",
    }),

  markAllAsRead: () =>
    http.request<ApiResponse<void>>("/notifications/read-all", {
      method: "PUT",
    }),
};
