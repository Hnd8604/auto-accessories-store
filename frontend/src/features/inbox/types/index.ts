export interface Conversation {
  id: string;
  guestName: string;
  channel: "WEB" | "ZALO" | "MESSENGER";
  status: "OPEN" | "CLOSED";
  unreadCount: number;
  lastMessageAt: string | null;
  createdAt: string;
  lastMessage: string | null;
}

export interface ChatMessage {
  id: string;
  conversationId: string;
  senderType: "CUSTOMER" | "ADMIN";
  content: string;
  createdAt: string;
}

export interface SendMessagePayload {
  conversationId: string;
  content: string;
  senderType: "CUSTOMER" | "ADMIN";
}
