package app.store.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
public class TransactionUtils {

    private TransactionUtils() {
    }

    /**
     * Chạy action sau khi transaction hiện tại commit thành công; không có transaction thì chạy ngay.
     * <p>
     * Dùng cho việc gọi ra ngoài (payOS, Cloudinary, Redis, SSE, WebSocket): không giữ DB connection
     * trong lúc gọi, và không gọi nếu transaction rollback. Lỗi chỉ ghi log vì dữ liệu đã commit,
     * ném lỗi lúc này chỉ làm client nhận 500 cho một thao tác thực ra đã thành công.
     * <p>
     * Action chạy sau commit không còn được bọc transaction: chỉ nên đọc DB, không ghi.
     */
    public static void runAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            runQuietly(action);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                runQuietly(action);
            }
        });
    }

    private static void runQuietly(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            log.error("After-commit action failed", e);
        }
    }
}
