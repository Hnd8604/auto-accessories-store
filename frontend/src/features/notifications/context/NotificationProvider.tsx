import React, {
  useState,
  useEffect,
  useRef,
  useCallback,
  ReactNode,
} from "react";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { ACCESS_TOKEN_KEY } from "@/constants/config";
import { NotificationsApi } from "@/features/notifications/api/notifications";
import { connectNotificationStream } from "@/features/notifications/api/stream";
import type { NotificationResponse } from "@/features/notifications/types";
import type { PageResponse } from "@/types";
import { NotificationContext, type NotificationContextType } from "./notification-context";

interface NotificationProviderProps {
  children: ReactNode;
}

export const NotificationProvider: React.FC<NotificationProviderProps> = ({
  children,
}) => {
  const { isAuthenticated } = useAuth();
  const [notifications, setNotifications] = useState<NotificationResponse[]>(
    []
  );
  const [unreadCount, setUnreadCount] = useState(0);
  const [isLoading, setIsLoading] = useState(false);
  const [hasMore, setHasMore] = useState(true);
  const [page, setPage] = useState(0);

  // SSE ở đây chạy bằng fetch + AbortController chứ không phải EventSource,
  // nên ref chỉ cần đúng một hàm close().
  const eventSourceRef = useRef<{ close: () => void } | null>(null);
  const reconnectTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(
    null
  );

  // ─── Fetch notifications ────────────────────────────────────
  const fetchNotifications = useCallback(
    async (pageNum: number, append = false) => {
      try {
        setIsLoading(true);
        const res = await NotificationsApi.getMyNotifications(pageNum, 10);
        const pageData: PageResponse<NotificationResponse> | undefined =
          res.result;
        if (pageData) {
          setNotifications((prev) =>
            append ? [...prev, ...pageData.content] : pageData.content
          );
          setHasMore(!pageData.last);
          setPage(pageNum);
        }
      } catch (error) {
        console.error("Failed to fetch notifications:", error);
      } finally {
        setIsLoading(false);
      }
    },
    []
  );

  const fetchUnreadCount = useCallback(async () => {
    try {
      const res = await NotificationsApi.getUnreadCount();
      const count = res.result ?? 0;
      setUnreadCount(count);
    } catch (error) {
      console.error("Failed to fetch unread count:", error);
    }
  }, []);

  // ─── SSE Connection ─────────────────────────────────────────
  const connectSSE = useCallback(() => {
    const token = localStorage.getItem(ACCESS_TOKEN_KEY);
    if (!token) return;

    eventSourceRef.current?.close();
    eventSourceRef.current = {
      close: connectNotificationStream(
        token,
        (notification) => {
          setNotifications((previous) => [notification, ...previous]);
          setUnreadCount((previous) => previous + 1);
        },
        (error) => {
          if (error) console.error("SSE connection error:", error);
          reconnectTimeoutRef.current = setTimeout(connectSSE, 5000);
        }
      ),
    };
  }, []);
  // ─── Effects ────────────────────────────────────────────────
  useEffect(() => {
    if (isAuthenticated) {
      // Fetch initial data
      fetchNotifications(0);
      fetchUnreadCount();
      // Connect SSE for real-time updates
      connectSSE();
    } else {
      // Clear state when logged out
      setNotifications([]);
      setUnreadCount(0);
      setPage(0);
      setHasMore(true);
    }

    return () => {
      // Cleanup SSE connection
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
        eventSourceRef.current = null;
      }
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current);
      }
    };
  }, [isAuthenticated, fetchNotifications, fetchUnreadCount, connectSSE]);

  // ─── Actions ────────────────────────────────────────────────
  const loadMore = useCallback(async () => {
    if (isLoading || !hasMore) return;
    await fetchNotifications(page + 1, true);
  }, [isLoading, hasMore, page, fetchNotifications]);

  const markAsRead = useCallback(
    async (notificationId: string) => {
      try {
        await NotificationsApi.markAsRead(notificationId);
        setNotifications((prev) =>
          prev.map((n) =>
            n.id === notificationId ? { ...n, isRead: true } : n
          )
        );
        setUnreadCount((prev) => Math.max(0, prev - 1));
      } catch (error) {
        console.error("Failed to mark notification as read:", error);
      }
    },
    []
  );

  const markAllAsRead = useCallback(async () => {
    try {
      await NotificationsApi.markAllAsRead();
      setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
      setUnreadCount(0);
    } catch (error) {
      console.error("Failed to mark all as read:", error);
    }
  }, []);

  const refresh = useCallback(async () => {
    await fetchNotifications(0);
    await fetchUnreadCount();
  }, [fetchNotifications, fetchUnreadCount]);

  // ─── Context Value ──────────────────────────────────────────
  const value: NotificationContextType = React.useMemo(
    () => ({
      notifications,
      unreadCount,
      isLoading,
      hasMore,
      loadMore,
      markAsRead,
      markAllAsRead,
      refresh,
    }),
    [
      notifications,
      unreadCount,
      isLoading,
      hasMore,
      loadMore,
      markAsRead,
      markAllAsRead,
      refresh,
    ]
  );

  return (
    <NotificationContext.Provider value={value}>
      {children}
    </NotificationContext.Provider>
  );
};

