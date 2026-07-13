import { useState, useEffect, useRef } from "react";
import { MessageCircle, X, Send } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { InboxApi } from "@/features/inbox/api/InboxApi";
import { useStompChat } from "@/features/inbox/hooks/useStompChat";
import type { ChatMessage } from "@/features/inbox/types";

const CONVERSATION_KEY = "chat_conversation_id";
const GUEST_NAME_KEY = "chat_guest_name";

function formatTime(dateStr: string) {
  return new Date(dateStr).toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" });
}

export function ChatWidget() {
  const [open, setOpen] = useState(false);
  const [conversationId, setConversationId] = useState<string | null>(
    () => localStorage.getItem(CONVERSATION_KEY)
  );
  const [guestName, setGuestName] = useState(
    () => localStorage.getItem(GUEST_NAME_KEY) || ""
  );
  const [nameInput, setNameInput] = useState("");
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState("");
  const [starting, setStarting] = useState(false);
  const scrollRef = useRef<HTMLDivElement>(null);

  const { send } = useStompChat({
    conversationId,
    onMessage: (msg) => {
      setMessages((prev) => {
        if (prev.find((m) => m.id === msg.id)) return prev;
        return [...prev, msg];
      });
      scrollToBottom();
    },
    enabled: !!conversationId,
  });

  useEffect(() => {
    if (conversationId) {
      InboxApi.getMessages(conversationId).then((res) =>
        setMessages(res.result.content)
      );
    }
  }, [conversationId]);

  useEffect(() => {
    if (open) scrollToBottom();
  }, [open, messages]);

  function scrollToBottom() {
    setTimeout(() => {
      if (scrollRef.current) {
        scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
      }
    }, 50);
  }

  async function handleStart() {
    const name = nameInput.trim();
    if (!name) return;
    setStarting(true);
    try {
      const res = await InboxApi.createConversation(name);
      const id = res.result.id;
      localStorage.setItem(CONVERSATION_KEY, id);
      localStorage.setItem(GUEST_NAME_KEY, name);
      setConversationId(id);
      setGuestName(name);
    } finally {
      setStarting(false);
    }
  }

  function handleSend() {
    const text = input.trim();
    if (!text || !conversationId) return;
    send({ conversationId, content: text, senderType: "CUSTOMER" });
    setInput("");
  }

  return (
    <div className="fixed bottom-6 right-6 z-50 flex flex-col items-end gap-3">
      {/* Chat window */}
      {open && (
        <div className="w-80 h-[420px] bg-white rounded-2xl shadow-2xl border flex flex-col overflow-hidden animate-in slide-in-from-bottom-4">
          {/* Header */}
          <div className="flex items-center justify-between px-4 py-3 bg-primary text-primary-foreground">
            <div className="flex items-center gap-2">
              <MessageCircle className="h-5 w-5" />
              <div>
                <p className="font-semibold text-sm">AutoLux Support</p>
                <p className="text-xs opacity-80">Thường trả lời trong vài phút</p>
              </div>
            </div>
            <Button
              variant="ghost"
              size="sm"
              className="h-7 w-7 p-0 text-primary-foreground hover:bg-primary-foreground/20"
              onClick={() => setOpen(false)}
            >
              <X className="h-4 w-4" />
            </Button>
          </div>

          {/* Body */}
          {!conversationId ? (
            /* Name form */
            <div className="flex-1 flex flex-col items-center justify-center p-6 gap-4">
              <MessageCircle className="h-10 w-10 text-primary opacity-60" />
              <p className="text-sm text-center text-muted-foreground">
                Xin chào! Cho chúng tôi biết tên của bạn để bắt đầu chat.
              </p>
              <Input
                placeholder="Nhập tên của bạn..."
                value={nameInput}
                onChange={(e) => setNameInput(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && handleStart()}
                className="text-sm"
              />
              <Button
                onClick={handleStart}
                disabled={!nameInput.trim() || starting}
                className="w-full"
                size="sm"
              >
                {starting ? "Đang kết nối..." : "Bắt đầu chat"}
              </Button>
            </div>
          ) : (
            /* Chat area */
            <>
              <div
                ref={scrollRef}
                className="flex-1 overflow-y-auto p-3 space-y-2"
              >
                {messages.length === 0 && (
                  <div className="text-center text-xs text-muted-foreground py-4">
                    Xin chào {guestName}! Chúng tôi sẵn sàng hỗ trợ bạn.
                  </div>
                )}
                {messages.map((m) => (
                  <div
                    key={m.id}
                    className={`flex ${m.senderType === "CUSTOMER" ? "justify-end" : "justify-start"}`}
                  >
                    <div
                      className={`max-w-[80%] rounded-2xl px-3 py-2 text-sm ${m.senderType === "CUSTOMER"
                          ? "bg-primary text-primary-foreground rounded-br-sm"
                          : "bg-muted text-foreground rounded-bl-sm"
                        }`}
                    >
                      <p className="break-words">{m.content}</p>
                      <p className={`text-[10px] mt-0.5 ${m.senderType === "CUSTOMER" ? "text-primary-foreground/70 text-right" : "text-muted-foreground"}`}>
                        {m.createdAt ? formatTime(m.createdAt) : ""}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
              <div className="flex gap-2 px-3 py-2 border-t">
                <Input
                  value={input}
                  onChange={(e) => setInput(e.target.value)}
                  placeholder="Nhập tin nhắn..."
                  onKeyDown={(e) => e.key === "Enter" && handleSend()}
                  className="flex-1 h-9 text-sm"
                />
                <Button size="sm" className="h-9 w-9 p-0" onClick={handleSend} disabled={!input.trim()}>
                  <Send className="h-4 w-4" />
                </Button>
              </div>
            </>
          )}
        </div>
      )}

      {/* Toggle button */}
      <Button
        onClick={() => setOpen((v) => !v)}
        size="lg"
        className="h-14 w-14 rounded-full shadow-lg hover:scale-105 transition-transform"
      >
        {open ? <X className="h-6 w-6" /> : <MessageCircle className="h-6 w-6" />}
      </Button>
    </div>
  );
}
