import { useEffect, useRef, useCallback } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { ACCESS_TOKEN_KEY, WS_URL } from "@/constants/config";
import { refreshAccessToken } from "@/features/auth/api/auth";
import { isTokenExpiringSoon } from "@/utils/jwt";
import type { ChatError, ChatMessage, SendMessagePayload } from "../types";

// refresh sớm hơn hạn thật một chút để bù lệch giờ giữa máy khách và server
const TOKEN_EXPIRY_SKEW_SECONDS = 30;

interface UseStompChatOptions {
  conversationId: string | null;
  onMessage: (msg: ChatMessage) => void;
  onAdminNewMessage?: (msg: ChatMessage) => void;
  onError?: (err: ChatError) => void;
  // true: gửi access token khi CONNECT (Inbox admin). Widget của khách để false để luôn là guest
  authenticated?: boolean;
  enabled?: boolean;
}

export function useStompChat({
  conversationId,
  onMessage,
  onAdminNewMessage,
  onError,
  authenticated = false,
  enabled = true,
}: UseStompChatOptions) {
  const clientRef = useRef<Client | null>(null);
  const onErrorRef = useRef(onError);
  onErrorRef.current = onError;
  // bật khi server từ chối CONNECT dù token còn hạn theo exp (đã logout ở tab khác, lệch giờ...)
  const forceRefreshRef = useRef(false);

  const send = useCallback((payload: SendMessagePayload) => {
    const client = clientRef.current;
    if (!client || !client.connected) return;
    client.publish({
      destination: "/app/chat.send",
      body: JSON.stringify(payload),
    });
  }, []);

  useEffect(() => {
    if (!enabled) return;

    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      reconnectDelay: 5000,
      // stompjs await callback này trước mỗi lần kết nối, kể cả reconnect
      beforeConnect: async () => {
        if (!authenticated) {
          client.connectHeaders = {};
          return;
        }

        let token = localStorage.getItem(ACCESS_TOKEN_KEY);
        if (
          forceRefreshRef.current ||
          !token ||
          isTokenExpiringSoon(token, TOKEN_EXPIRY_SKEW_SECONDS)
        ) {
          forceRefreshRef.current = false;
          token = await refreshAccessToken();
        }

        if (!token) {
          // hết phiên đăng nhập: dừng hẳn thay vì reconnect mãi với token hỏng
          await client.deactivate();
          return;
        }
        client.connectHeaders = { Authorization: `Bearer ${token}` };
      },
      onStompError: () => {
        // ERROR frame khi chưa CONNECTED nghĩa là server từ chối token → lần reconnect sau phải refresh
        if (authenticated && !client.connected) {
          forceRefreshRef.current = true;
        }
      },
      onConnect: () => {
        client.subscribe("/user/queue/errors", (frame) => {
          try {
            const err: ChatError = JSON.parse(frame.body);
            onErrorRef.current?.(err);
          } catch {
            // ignore parse errors
          }
        });

        if (conversationId) {
          client.subscribe(`/topic/conversation/${conversationId}`, (frame) => {
            try {
              const msg: ChatMessage = JSON.parse(frame.body);
              onMessage(msg);
            } catch {
              // ignore parse errors
            }
          });
        }

        if (onAdminNewMessage) {
          client.subscribe("/topic/admin/new-message", (frame) => {
            try {
              const msg: ChatMessage = JSON.parse(frame.body);
              onAdminNewMessage(msg);
            } catch {
              // ignore parse errors
            }
          });
        }
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [conversationId, enabled, authenticated]);

  return { send };
}
