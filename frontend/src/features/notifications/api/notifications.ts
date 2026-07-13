import axios from "axios";
import { API_BASE_URL, ACCESS_TOKEN_KEY } from "@/constants/config";
import type { ApiResponse, PageResponse } from "@/types";
import type { NotificationResponse } from "../types";

export const NotificationsApi = {
  /** Lấy danh sách thông báo có phân trang */
  getMyNotifications: async (page = 0, size = 10) => {
    const token = localStorage.getItem(ACCESS_TOKEN_KEY);
    const res = await axios.get<ApiResponse<PageResponse<NotificationResponse>>>(
      `${API_BASE_URL}/notifications`,
      {
        params: { page, size },
        headers: {
          Authorization: `Bearer ${token}`,
        },
      }
    );
    return res.data;
  },

  /** Lấy số lượng thông báo chưa đọc */
  getUnreadCount: async () => {
    const token = localStorage.getItem(ACCESS_TOKEN_KEY);
    const res = await axios.get<ApiResponse<number>>(
      `${API_BASE_URL}/notifications/unread-count`,
      {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      }
    );
    return res.data;
  },

  /** Đánh dấu một thông báo đã đọc */
  markAsRead: async (notificationId: string) => {
    const token = localStorage.getItem(ACCESS_TOKEN_KEY);
    const res = await axios.put<ApiResponse<void>>(
      `${API_BASE_URL}/notifications/${notificationId}/read`,
      {},
      {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      }
    );
    return res.data;
  },

  /** Đánh dấu tất cả thông báo đã đọc */
  markAllAsRead: async () => {
    const token = localStorage.getItem(ACCESS_TOKEN_KEY);
    const res = await axios.put<ApiResponse<void>>(
      `${API_BASE_URL}/notifications/read-all`,
      {},
      {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      }
    );
    return res.data;
  },
};
