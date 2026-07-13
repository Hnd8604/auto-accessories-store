import { Badge } from "@/components/ui/badge";
import type { Conversation } from "../types";

interface Props {
  conversation: Conversation;
  isSelected: boolean;
  onClick: () => void;
}

function timeAgo(dateStr: string | null) {
  if (!dateStr) return "";
  const d = new Date(dateStr);
  const now = new Date();
  const diff = Math.floor((now.getTime() - d.getTime()) / 1000);
  if (diff < 60) return "Vừa xong";
  if (diff < 3600) return `${Math.floor(diff / 60)} phút trước`;
  if (diff < 86400) return `${Math.floor(diff / 3600)} giờ trước`;
  return d.toLocaleDateString("vi-VN");
}

export function ConversationItem({ conversation, isSelected, onClick }: Props) {
  return (
    <button
      onClick={onClick}
      className={`w-full text-left px-4 py-3 border-b transition-colors hover:bg-muted/50 ${
        isSelected ? "bg-muted" : ""
      }`}
    >
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2 min-w-0">
          {conversation.unreadCount > 0 && (
            <span className="w-2 h-2 rounded-full bg-primary flex-shrink-0" />
          )}
          <span className="font-medium text-sm truncate">{conversation.guestName}</span>
        </div>
        <div className="flex items-center gap-1 flex-shrink-0 ml-2">
          {conversation.unreadCount > 0 && (
            <Badge variant="default" className="h-5 min-w-5 text-[10px] px-1">
              {conversation.unreadCount}
            </Badge>
          )}
          <span className="text-[10px] text-muted-foreground">
            {timeAgo(conversation.lastMessageAt)}
          </span>
        </div>
      </div>
      {conversation.lastMessage && (
        <p className="text-xs text-muted-foreground mt-0.5 truncate pl-4">
          {conversation.lastMessage}
        </p>
      )}
      <div className="flex items-center gap-1 mt-1 pl-4">
        <span
          className={`text-[10px] px-1.5 py-0.5 rounded font-medium ${
            conversation.status === "OPEN"
              ? "bg-green-100 text-green-700"
              : "bg-gray-100 text-gray-500"
          }`}
        >
          {conversation.status === "OPEN" ? "Đang mở" : "Đã đóng"}
        </span>
      </div>
    </button>
  );
}
