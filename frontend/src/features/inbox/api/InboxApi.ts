import axios from "axios";
import { API_BASE_URL, ACCESS_TOKEN_KEY } from "@/constants/config";
import type { Conversation, ChatMessage } from "../types";

interface ApiResponse<T> {
  code: number;
  message?: string;
  result: T;
}

interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

function authHeaders() {
  const token = localStorage.getItem(ACCESS_TOKEN_KEY);
  return token ? { Authorization: `Bearer ${token}` } : {};
}

export const InboxApi = {
  createConversation: async (guestName: string): Promise<ApiResponse<Conversation>> => {
    const res = await axios.post<ApiResponse<Conversation>>(
      `${API_BASE_URL}/conversations`,
      { guestName }
    );
    return res.data;
  },

  getConversations: async (page = 0, size = 20): Promise<ApiResponse<PageResponse<Conversation>>> => {
    const res = await axios.get<ApiResponse<PageResponse<Conversation>>>(
      `${API_BASE_URL}/conversations`,
      { params: { page, size }, headers: authHeaders() }
    );
    return res.data;
  },

  getMessages: async (
    conversationId: string,
    page = 0,
    size = 50
  ): Promise<ApiResponse<PageResponse<ChatMessage>>> => {
    const res = await axios.get<ApiResponse<PageResponse<ChatMessage>>>(
      `${API_BASE_URL}/conversations/${conversationId}/messages`,
      { params: { page, size } }
    );
    return res.data;
  },

  markAsRead: async (conversationId: string): Promise<void> => {
    await axios.put(
      `${API_BASE_URL}/conversations/${conversationId}/read`,
      {},
      { headers: authHeaders() }
    );
  },

  closeConversation: async (conversationId: string): Promise<void> => {
    await axios.put(
      `${API_BASE_URL}/conversations/${conversationId}/close`,
      {},
      { headers: authHeaders() }
    );
  },

  getTotalUnread: async (): Promise<ApiResponse<number>> => {
    const res = await axios.get<ApiResponse<number>>(
      `${API_BASE_URL}/conversations/unread-count`,
      { headers: authHeaders() }
    );
    return res.data;
  },
};
