import { memo, useState, useCallback, useRef, useEffect } from "react";
import { Bell, Check, CheckCheck, Package, CreditCard, AlertCircle, Clock } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Separator } from "@/components/ui/separator";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { useNotifications } from "@/context/notification-context";
import { NotificationType } from "@/features/notifications/types";
import type { NotificationResponse } from "@/features/notifications/types";

/** Returns icon and color based on notification type */
function getNotificationStyle(type: NotificationType) {
  switch (type) {
    case NotificationType.ORDER_CREATED:
      return {
        icon: Package,
        color: "text-blue-500",
        bg: "bg-blue-500/10",
        label: "Đơn hàng mới",
      };
    case NotificationType.ORDER_STATUS_CHANGED:
      return {
        icon: Clock,
        color: "text-amber-500",
        bg: "bg-amber-500/10",
        label: "Cập nhật đơn hàng",
      };
    case NotificationType.ORDER_CANCELED:
      return {
        icon: AlertCircle,
        color: "text-red-500",
        bg: "bg-red-500/10",
        label: "Đơn hàng bị hủy",
      };
    case NotificationType.PAYMENT_RECEIVED:
      return {
        icon: CreditCard,
        color: "text-emerald-500",
        bg: "bg-emerald-500/10",
        label: "Thanh toán",
      };
    case NotificationType.SYSTEM:
    default:
      return {
        icon: Bell,
        color: "text-purple-500",
        bg: "bg-purple-500/10",
        label: "Hệ thống",
      };
  }
}

/** Format relative time in Vietnamese */
function formatTimeAgo(dateString: string): string {
  const now = new Date();
  const date = new Date(dateString);
  const diffMs = now.getTime() - date.getTime();
  const diffSec = Math.floor(diffMs / 1000);
  const diffMin = Math.floor(diffSec / 60);
  const diffHour = Math.floor(diffMin / 60);
  const diffDay = Math.floor(diffHour / 24);

  if (diffSec < 60) return "Vừa xong";
  if (diffMin < 60) return `${diffMin} phút trước`;
  if (diffHour < 24) return `${diffHour} giờ trước`;
  if (diffDay < 7) return `${diffDay} ngày trước`;

  return date.toLocaleDateString("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
}

/** Single notification item */
const NotificationItem = memo(
  ({
    notification,
    onRead,
  }: {
    notification: NotificationResponse;
    onRead: (id: string) => void;
  }) => {
    const style = getNotificationStyle(notification.type);
    const Icon = style.icon;

    return (
      <button
        className={`w-full flex items-start gap-3 p-3 text-left transition-colors hover:bg-accent/50 rounded-lg group ${
          !notification.isRead ? "bg-primary/5" : ""
        }`}
        onClick={() => {
          if (!notification.isRead) {
            onRead(notification.id);
          }
        }}
      >
        {/* Icon */}
        <div
          className={`flex-shrink-0 w-9 h-9 rounded-full flex items-center justify-center ${style.bg}`}
        >
          <Icon className={`w-4 h-4 ${style.color}`} />
        </div>

        {/* Content */}
        <div className="flex-1 min-w-0">
          <div className="flex items-center justify-between gap-2">
            <span
              className={`text-xs font-medium ${style.color}`}
            >
              {style.label}
            </span>
            <span className="text-[10px] text-muted-foreground flex-shrink-0">
              {formatTimeAgo(notification.createdAt)}
            </span>
          </div>
          <p
            className={`text-sm leading-snug mt-0.5 ${
              !notification.isRead
                ? "font-semibold text-foreground"
                : "text-muted-foreground"
            }`}
          >
            {notification.title}
          </p>
          <p className="text-xs text-muted-foreground mt-0.5 line-clamp-2">
            {notification.message}
          </p>
        </div>

        {/* Unread indicator */}
        {!notification.isRead && (
          <div className="flex-shrink-0 mt-2">
            <div className="w-2.5 h-2.5 rounded-full bg-primary animate-pulse" />
          </div>
        )}
      </button>
    );
  }
);
NotificationItem.displayName = "NotificationItem";

/** Notification bell button with dropdown panel */
export const NotificationBell = memo(() => {
  const {
    notifications,
    unreadCount,
    isLoading,
    hasMore,
    loadMore,
    markAsRead,
    markAllAsRead,
  } = useNotifications();
  const [isOpen, setIsOpen] = useState(false);
  const scrollRef = useRef<HTMLDivElement>(null);

  // Handle infinite scroll
  const handleScroll = useCallback(
    (e: React.UIEvent<HTMLDivElement>) => {
      const target = e.target as HTMLDivElement;
      const nearBottom =
        target.scrollHeight - target.scrollTop - target.clientHeight < 100;
      if (nearBottom && hasMore && !isLoading) {
        loadMore();
      }
    },
    [hasMore, isLoading, loadMore]
  );

  return (
    <Popover open={isOpen} onOpenChange={setIsOpen}>
      <PopoverTrigger asChild>
        <Button
          variant="ghost"
          size="sm"
          className="relative h-10 w-10 rounded-full"
          id="notification-bell"
        >
          <Bell className="h-5 w-5" />
          {unreadCount > 0 && (
            <Badge
              variant="destructive"
              className="absolute -top-1 -right-1 h-5 min-w-5 flex items-center justify-center p-0 text-[10px] font-bold animate-in zoom-in-50"
            >
              {unreadCount > 99 ? "99+" : unreadCount}
            </Badge>
          )}
        </Button>
      </PopoverTrigger>

      <PopoverContent
        className="w-[380px] p-0 shadow-xl border border-border/60"
        align="end"
        sideOffset={8}
      >
        {/* Header */}
        <div className="flex items-center justify-between px-4 py-3 border-b">
          <div className="flex items-center gap-2">
            <h3 className="font-semibold text-sm">Thông báo</h3>
            {unreadCount > 0 && (
              <Badge
                variant="secondary"
                className="text-[10px] h-5 px-1.5"
              >
                {unreadCount} mới
              </Badge>
            )}
          </div>
          {unreadCount > 0 && (
            <Button
              variant="ghost"
              size="sm"
              className="text-xs h-7 px-2 text-primary hover:text-primary/80"
              onClick={markAllAsRead}
            >
              <CheckCheck className="w-3.5 h-3.5 mr-1" />
              Đọc tất cả
            </Button>
          )}
        </div>

        {/* Notification list */}
        <ScrollArea
          className="max-h-[400px]"
          onScrollCapture={handleScroll}
          ref={scrollRef}
        >
          <div className="p-1">
            {notifications.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-12 text-muted-foreground">
                <Bell className="w-10 h-10 mb-3 opacity-30" />
                <p className="text-sm font-medium">Chưa có thông báo</p>
                <p className="text-xs mt-1">
                  Thông báo mới sẽ hiển thị tại đây
                </p>
              </div>
            ) : (
              <>
                {notifications.map((notification, index) => (
                  <div key={notification.id}>
                    <NotificationItem
                      notification={notification}
                      onRead={markAsRead}
                    />
                    {index < notifications.length - 1 && (
                      <Separator className="mx-3" />
                    )}
                  </div>
                ))}

                {/* Loading indicator */}
                {isLoading && (
                  <div className="flex items-center justify-center py-3">
                    <div className="w-5 h-5 border-2 border-primary/30 border-t-primary rounded-full animate-spin" />
                  </div>
                )}

                {/* Load more button */}
                {hasMore && !isLoading && (
                  <Button
                    variant="ghost"
                    size="sm"
                    className="w-full text-xs text-muted-foreground hover:text-foreground"
                    onClick={loadMore}
                  >
                    Tải thêm thông báo
                  </Button>
                )}

                {/* End of list */}
                {!hasMore && notifications.length > 0 && (
                  <p className="text-center text-xs text-muted-foreground py-3">
                    Đã hiển thị tất cả thông báo
                  </p>
                )}
              </>
            )}
          </div>
        </ScrollArea>
      </PopoverContent>
    </Popover>
  );
});
NotificationBell.displayName = "NotificationBell";
