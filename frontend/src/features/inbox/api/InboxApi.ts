import { http } from "@/services/http";
import { simpleHttp } from "@/services/axios";
import type { ApiResponse, PageResponse } from "@/types";
import type { Conversation, ChatMessage } from "../types";

export const InboxApi = {
  createConversation: (guestName: string) =>
    simpleHttp.request<ApiResponse<Conversation>>("/conversations", {
      method: "POST",
      body: { guestName },
    }),

  getConversations: (page = 0, size = 20) =>
    http.request<ApiResponse<PageResponse<Conversation>>>("/conversations", {
      params: { page, size },
    }),

  getMessages: (conversationId: string, page = 0, size = 50) =>
    simpleHttp.request<ApiResponse<PageResponse<ChatMessage>>>(
      `/conversations/${conversationId}/messages`,
      { params: { page, size } }
    ),

  markAsRead: (conversationId: string) =>
    http.request<void>(`/conversations/${conversationId}/read`, {
      method: "PUT",
      body: {},
    }),

  closeConversation: (conversationId: string) =>
    http.request<void>(`/conversations/${conversationId}/close`, {
      method: "PUT",
      body: {},
    }),

  getTotalUnread: () =>
    http.request<ApiResponse<number>>("/conversations/unread-count"),
};
