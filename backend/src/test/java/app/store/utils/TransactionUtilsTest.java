package app.store.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class TransactionUtilsTest {

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void runAfterCommit_shouldRunImmediately_whenNoTransaction() {
        AtomicInteger calls = new AtomicInteger();

        TransactionUtils.runAfterCommit(calls::incrementAndGet);

        assertThat(calls).hasValue(1);
    }

    @Test
    void runAfterCommit_shouldWaitForCommit_whenInTransaction() {
        TransactionSynchronizationManager.initSynchronization();
        AtomicInteger calls = new AtomicInteger();

        TransactionUtils.runAfterCommit(calls::incrementAndGet);
        assertThat(calls).hasValue(0); // chưa commit thì chưa chạy

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        assertThat(calls).hasValue(1);
    }

    @Test
    void runAfterCommit_shouldNotRun_whenTransactionRollsBack() {
        TransactionSynchronizationManager.initSynchronization();
        AtomicInteger calls = new AtomicInteger();

        TransactionUtils.runAfterCommit(calls::incrementAndGet);
        // Rollback chỉ gọi afterCompletion, không gọi afterCommit
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        assertThat(calls).hasValue(0);
    }

    @Test
    void runAfterCommit_shouldOnlyLog_whenActionFails() {
        assertThatCode(() -> TransactionUtils.runAfterCommit(() -> {
            throw new IllegalStateException("payOS down");
        })).doesNotThrowAnyException();
    }
}
