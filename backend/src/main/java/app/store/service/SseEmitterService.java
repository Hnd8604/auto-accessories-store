package app.store.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Quản lý các kết nối SSE (Server-Sent Events) theo userId.
 * Mỗi user có thể mở nhiều tab/device → nhiều emitter.
 */
@Service
@Slf4j
public class SseEmitterService {

    // Mỗi user có thể có nhiều kết nối SSE (nhiều tab trình duyệt)
    private final Map<String, CopyOnWriteArrayList<SseEmitter>> emitterMap = new ConcurrentHashMap<>(); // key: userId, value: list of SseEmitter

    private static final long SSE_TIMEOUT = 30 * 60 * 1000L; // 30 phút

    /**
     * Tạo SseEmitter mới cho user
     */
    public SseEmitter createEmitter(String userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT); // emitter: kênh gửi dữ liệu từ client->server

        emitterMap.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter); // nếu chưa có key thì tạo mới, sau đó add emitter vào list

        emitter.onCompletion(() -> removeEmitter(userId, emitter)); // khi emitter bị đóng
        emitter.onTimeout(() -> removeEmitter(userId, emitter)); // khi emitter timeout
        emitter.onError(e -> removeEmitter(userId, emitter)); // khi emitter có lỗi

        log.info("SSE connection opened for userId={}", userId);

        // Gửi event kết nối thành công
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("SSE connection established"));
        } catch (IOException e) {
            log.error("Failed to send initial SSE event for userId={}", userId, e);
        }

        return emitter;
    }

    /**
     * Push notification đến tất cả kết nối SSE của user
     */
    public void sendToUser(String userId, Object data) { // data là NotificationResponse
        CopyOnWriteArrayList<SseEmitter> emitters = emitterMap.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            log.debug("No active SSE connections for userId={}", userId);
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(data));
            } catch (IOException e) {
                log.warn("Failed to send SSE event to userId={}, removing emitter", userId);
                removeEmitter(userId, emitter);
            }
        }
    }

    private void removeEmitter(String userId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = emitterMap.get(userId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                emitterMap.remove(userId);
            }
        }
        log.debug("SSE connection removed for userId={}", userId);
    }
}
