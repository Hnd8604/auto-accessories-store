import React, {
  createContext,
  useContext,
  useState,
  useEffect,
  useRef,
  useCallback,
  ReactNode,
} from "react";
import { useAuth } from "@/context/auth-context";
import { ACCESS_TOKEN_KEY, API_BASE_URL } from "@/constants/config";
import { NotificationsApi } from "@/features/notifications/api/notifications";
import type { NotificationResponse } from "@/features/notifications/types";
import type { PageResponse } from "@/types";

interface NotificationContextType {
  notifications: NotificationResponse[];
  unreadCount: number;
  isLoading: boolean;
  hasMore: boolean;
  /** Tải thêm thông báo (phân trang) */
  loadMore: () => Promise<void>;
  /** Đánh dấu một thông báo đã đọc */
  markAsRead: (notificationId: string) => Promise<void>;
  /** Đánh dấu tất cả thông báo đã đọc */
  markAllAsRead: () => Promise<void>;
  /** Refresh lại danh sách thông báo */
  refresh: () => Promise<void>;
}

const NotificationContext = createContext<NotificationContextType | undefined>(
  undefined
);

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

  const eventSourceRef = useRef<EventSource | null>(null);
  const reconnectTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(
    null
  );

  // ─── Fetch notifications ────────────────────────────────────
  const fetchNotifications = useCallback(
    async (pageNum: number, append = false) => {
      try {
        setIsLoading(true);
        const res = await NotificationsApi.getMyNotifications(pageNum, 10);
        const pageData: PageResponse<NotificationResponse> | undefined = (
          res as any
        )?.result;
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
      const count = (res as any)?.result ?? 0;
      setUnreadCount(count);
    } catch (error) {
      console.error("Failed to fetch unread count:", error);
    }
  }, []);

  // ─── SSE Connection ─────────────────────────────────────────
  const connectSSE = useCallback(() => {
    const token = localStorage.getItem(ACCESS_TOKEN_KEY);
    if (!token) return;

    // Close existing connection
    if (eventSourceRef.current) {
      eventSourceRef.current.close();
    }

    // EventSource does not support custom headers natively.
    // The standard workaround is to pass the token as a query parameter
    // or use a polyfill. Since our backend is Spring Boot SSE with
    // security, we'll use the token as a query param approach.
    // Alternative: Use fetch-based SSE for header support.
    const sseUrl = `${API_BASE_URL}/notifications/stream?token=${encodeURIComponent(token)}`;

    // Since EventSource doesn't support Authorization header,
    // we'll use a fetch-based approach for SSE
    const abortController = new AbortController();

    const connectWithFetch = async () => {
      try {
        const response = await fetch(
          `${API_BASE_URL}/notifications/stream`,
          {
            headers: {
              Authorization: `Bearer ${token}`,
              Accept: "text/event-stream",
            },
            signal: abortController.signal,
          }
        );

        if (!response.ok) {
          console.error("SSE connection failed:", response.status);
          // Retry after 5 seconds
          reconnectTimeoutRef.current = setTimeout(connectSSE, 5000);
          return;
        }

        const reader = response.body?.getReader();
        const decoder = new TextDecoder();

        if (!reader) return;

        let buffer = "";

        const processStream = async () => {
          try {
            while (true) {
              const { done, value } = await reader.read();
              if (done) break;

              buffer += decoder.decode(value, { stream: true });

              // Process complete SSE events from the buffer
              const events = buffer.split("\n\n");
              buffer = events.pop() || ""; // Keep the incomplete event in buffer

              for (const eventStr of events) {
                if (!eventStr.trim()) continue;

                const lines = eventStr.split("\n");
                let eventName = "";
                let eventData = "";

                for (const line of lines) {
                  if (line.startsWith("event:")) {
                    eventName = line.slice(6).trim();
                  } else if (line.startsWith("data:")) {
                    eventData = line.slice(5).trim();
                  }
                }

                if (eventName === "notification" && eventData) {
                  try {
                    const notification: NotificationResponse =
                      JSON.parse(eventData);
                    // Add to the top of the list
                    setNotifications((prev) => [notification, ...prev]);
                    setUnreadCount((prev) => prev + 1);
                  } catch (e) {
                    console.error("Failed to parse SSE notification:", e);
                  }
                }
              }
            }
          } catch (error: any) {
            if (error.name !== "AbortError") {
              console.error("SSE stream error:", error);
              // Reconnect after 5 seconds
              reconnectTimeoutRef.current = setTimeout(connectSSE, 5000);
            }
          }
        };

        processStream();
      } catch (error: any) {
        if (error.name !== "AbortError") {
          console.error("SSE connection error:", error);
          reconnectTimeoutRef.current = setTimeout(connectSSE, 5000);
        }
      }
    };

    connectWithFetch();

    // Store abort controller for cleanup
    eventSourceRef.current = {
      close: () => abortController.abort(),
    } as any;
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

export const useNotifications = (): NotificationContextType => {
  const context = useContext(NotificationContext);
  if (context === undefined) {
    throw new Error(
      "useNotifications must be used within a NotificationProvider"
    );
  }
  return context;
};
