export interface Conversation {
  id: string;
  guestName: string;
  channel: "WEB";
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

// senderType do server suy ra từ phiên STOMP (có token admin hay không)
export interface SendMessagePayload {
  conversationId: string;
  content: string;
}

export interface ChatError {
  code: number;
  message: string;
}
