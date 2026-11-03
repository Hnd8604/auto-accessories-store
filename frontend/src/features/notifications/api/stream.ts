import { API_BASE_URL } from "@/constants/config";
import type { NotificationResponse } from "../types";

export function connectNotificationStream(
  token: string,
  onNotification: (notification: NotificationResponse) => void,
  onDisconnect: (error?: Error) => void
) {
  const controller = new AbortController();

  const readStream = async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/notifications/stream`, {
        headers: {
          Authorization: `Bearer ${token}`,
          Accept: "text/event-stream",
        },
        signal: controller.signal,
      });
      if (!response.ok || !response.body) {
        throw new Error(`Không thể kết nối thông báo (${response.status})`);
      }

      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let buffer = "";

      while (!controller.signal.aborted) {
        const { done, value } = await reader.read();
        if (done) break;
        buffer += decoder.decode(value, { stream: true });
        buffer = buffer.replace(/\r\n/g, "\n");
        const events = buffer.split("\n\n");
        buffer = events.pop() ?? "";

        for (const event of events) {
          const lines = event.split("\n");
          const eventName = lines.find((line) => line.startsWith("event:"))?.slice(6).trim();
          const data = lines.filter((line) => line.startsWith("data:")).map((line) => line.slice(5).trim()).join("\n");
          if (eventName !== "notification" || !data) continue;
          try {
            onNotification(JSON.parse(data) as NotificationResponse);
          } catch (error) {
            console.error("Failed to parse notification:", error);
          }
        }
      }
      if (!controller.signal.aborted) onDisconnect();
    } catch (error) {
      if (!controller.signal.aborted) {
        onDisconnect(error instanceof Error ? error : new Error(String(error)));
      }
    }
  };

  void readStream();
  return () => controller.abort();
}
