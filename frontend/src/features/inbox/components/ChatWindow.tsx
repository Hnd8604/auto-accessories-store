import { useState, useEffect, useRef } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Send, X } from "lucide-react";
import { MessageBubble } from "./MessageBubble";
import { InboxApi } from "../api/InboxApi";
import { useStompChat } from "../hooks/useStompChat";
import type { Conversation, ChatMessage } from "../types";

interface Props {
  conversation: Conversation;
  onClose: () => void;
  onMarkRead: () => void;
  onConversationClosed: () => void;
}

export function ChatWindow({ conversation, onClose, onMarkRead, onConversationClosed }: Props) {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(true);
  const scrollRef = useRef<HTMLDivElement>(null);

  const { send } = useStompChat({
    conversationId: conversation.id,
    onMessage: (msg) => {
      setMessages((prev) => {
        if (prev.find((m) => m.id === msg.id)) return prev;
        return [...prev, msg];
      });
      scrollToBottom();
    },
  });

  useEffect(() => {
    setLoading(true);
    setMessages([]);
    InboxApi.getMessages(conversation.id)
      .then((res) => setMessages(res.result.content))
      .finally(() => setLoading(false));
    InboxApi.markAsRead(conversation.id).then(onMarkRead).catch(() => {});
  }, [conversation.id]);

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  function scrollToBottom() {
    setTimeout(() => {
      if (scrollRef.current) {
        scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
      }
    }, 50);
  }

  function handleSend() {
    const text = input.trim();
    if (!text) return;
    send({ conversationId: conversation.id, content: text, senderType: "ADMIN" });
    setInput("");
  }

  async function handleClose() {
    await InboxApi.closeConversation(conversation.id);
    onConversationClosed();
  }

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-3 border-b bg-white">
        <div>
          <h3 className="font-semibold">{conversation.guestName}</h3>
          <span
            className={`text-xs px-1.5 py-0.5 rounded font-medium ${
              conversation.status === "OPEN"
                ? "bg-green-100 text-green-700"
                : "bg-gray-100 text-gray-500"
            }`}
          >
            {conversation.status === "OPEN" ? "Đang mở" : "Đã đóng"}
          </span>
        </div>
        <div className="flex gap-2">
          {conversation.status === "OPEN" && (
            <Button variant="outline" size="sm" onClick={handleClose}>
              <X className="h-3.5 w-3.5 mr-1" />
              Đóng hội thoại
            </Button>
          )}
          <Button variant="ghost" size="sm" onClick={onClose}>
            <X className="h-4 w-4" />
          </Button>
        </div>
      </div>

      {/* Messages */}
      <div
        ref={scrollRef}
        className="flex-1 overflow-y-auto p-4 space-y-1"
      >
        {loading ? (
          <div className="flex items-center justify-center h-full text-muted-foreground text-sm">
            Đang tải tin nhắn...
          </div>
        ) : messages.length === 0 ? (
          <div className="flex items-center justify-center h-full text-muted-foreground text-sm">
            Chưa có tin nhắn nào
          </div>
        ) : (
          messages.map((m) => <MessageBubble key={m.id} message={m} />)
        )}
      </div>

      {/* Input */}
      {conversation.status === "OPEN" && (
        <div className="flex gap-2 px-4 py-3 border-t bg-white">
          <Input
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder="Nhập tin nhắn..."
            onKeyDown={(e) => e.key === "Enter" && !e.shiftKey && handleSend()}
            className="flex-1"
          />
          <Button onClick={handleSend} size="sm" disabled={!input.trim()}>
            <Send className="h-4 w-4" />
          </Button>
        </div>
      )}
    </div>
  );
}
