import type { ChatMessage } from "../types";

interface Props {
  message: ChatMessage;
}

function formatTime(dateStr: string) {
  const d = new Date(dateStr);
  return d.toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" });
}

export function MessageBubble({ message }: Props) {
  const isAdmin = message.senderType === "ADMIN";
  return (
    <div className={`flex ${isAdmin ? "justify-end" : "justify-start"} mb-2`}>
      <div
        className={`max-w-[70%] rounded-2xl px-4 py-2 text-sm ${
          isAdmin
            ? "bg-primary text-primary-foreground rounded-br-sm"
            : "bg-muted text-foreground rounded-bl-sm"
        }`}
      >
        <p className="break-words">{message.content}</p>
        <p
          className={`text-[10px] mt-1 ${
            isAdmin ? "text-primary-foreground/70 text-right" : "text-muted-foreground"
          }`}
        >
          {message.createdAt ? formatTime(message.createdAt) : ""}
        </p>
      </div>
    </div>
  );
}
