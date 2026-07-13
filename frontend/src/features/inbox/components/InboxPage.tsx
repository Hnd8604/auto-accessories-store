import { useState, useEffect, useCallback } from "react";
import { MessageSquare } from "lucide-react";
import { ConversationList } from "./ConversationList";
import { ChatWindow } from "./ChatWindow";
import { InboxApi } from "../api/InboxApi";
import { useStompChat } from "../hooks/useStompChat";
import type { Conversation, ChatMessage } from "../types";

export function InboxPage() {
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [selected, setSelected] = useState<Conversation | null>(null);
  const [loading, setLoading] = useState(true);

  const loadConversations = useCallback(() => {
    InboxApi.getConversations()
      .then((res) => setConversations(res.result.content))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    loadConversations();
  }, [loadConversations]);

  // Listen for new messages from any conversation to update the list
  useStompChat({
    conversationId: null,
    onMessage: () => {},
    onAdminNewMessage: (_msg: ChatMessage) => {
      loadConversations();
    },
  });

  function handleSelect(conv: Conversation) {
    setSelected(conv);
  }

  function handleMarkRead() {
    setConversations((prev) =>
      prev.map((c) => (c.id === selected?.id ? { ...c, unreadCount: 0 } : c))
    );
  }

  function handleConversationClosed() {
    setConversations((prev) =>
      prev.map((c) => (c.id === selected?.id ? { ...c, status: "CLOSED" } : c))
    );
    setSelected((prev) => (prev ? { ...prev, status: "CLOSED" } : null));
  }

  return (
    <div className="h-[calc(100vh-120px)] flex flex-col">
      <h2 className="text-2xl font-bold mb-4">Tin nhắn</h2>
      <div className="flex-1 flex border rounded-lg overflow-hidden bg-white shadow-sm">
        {/* Left: conversation list */}
        <div className="w-72 flex-shrink-0">
          {loading ? (
            <div className="p-4 text-sm text-muted-foreground">Đang tải...</div>
          ) : (
            <ConversationList
              conversations={conversations}
              selectedId={selected?.id ?? null}
              onSelect={handleSelect}
            />
          )}
        </div>

        {/* Right: chat area */}
        <div className="flex-1">
          {selected ? (
            <ChatWindow
              key={selected.id}
              conversation={selected}
              onClose={() => setSelected(null)}
              onMarkRead={handleMarkRead}
              onConversationClosed={handleConversationClosed}
            />
          ) : (
            <div className="flex flex-col items-center justify-center h-full text-muted-foreground gap-3">
              <MessageSquare className="h-12 w-12 opacity-30" />
              <p className="text-sm">Chọn một hội thoại để bắt đầu</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
