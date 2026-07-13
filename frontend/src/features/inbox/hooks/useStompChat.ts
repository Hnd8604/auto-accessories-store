import { useEffect, useRef, useCallback } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import type { ChatMessage, SendMessagePayload } from "../types";

const WS_URL = "http://localhost:8080/api/v1/ws";

interface UseStompChatOptions {
  conversationId: string | null;
  onMessage: (msg: ChatMessage) => void;
  onAdminNewMessage?: (msg: ChatMessage) => void;
  enabled?: boolean;
}

export function useStompChat({
  conversationId,
  onMessage,
  onAdminNewMessage,
  enabled = true,
}: UseStompChatOptions) {
  const clientRef = useRef<Client | null>(null);

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
      onConnect: () => {
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
  }, [conversationId, enabled]);

  return { send };
}
