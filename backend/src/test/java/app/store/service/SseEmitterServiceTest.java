package app.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SseEmitterService không có dependency nào -> không cần Mockito, chỉ new lên và test trực tiếp.
 * Trạng thái nội bộ (emitterMap) được đọc bằng ReflectionTestUtils vì nó là private.
 */
public class SseEmitterServiceTest {

    private SseEmitterService sseEmitterService;

    @BeforeEach
    void setUp() {
        sseEmitterService = new SseEmitterService();
    }

    @SuppressWarnings("unchecked")
    private Map<String, CopyOnWriteArrayList<SseEmitter>> emitterMap() {
        return (Map<String, CopyOnWriteArrayList<SseEmitter>>)
                ReflectionTestUtils.getField(sseEmitterService, "emitterMap");
    }

    @Test
    void createEmitter_shouldRegisterEmitterForUser() {
        SseEmitter emitter = sseEmitterService.createEmitter("u1");

        assertThat(emitter).isNotNull();
        assertThat(emitterMap()).containsKey("u1");
        assertThat(emitterMap().get("u1")).containsExactly(emitter);
    }

    @Test
    void createEmitter_shouldSupportMultipleTabsPerUser() {
        SseEmitter tab1 = sseEmitterService.createEmitter("u1");
        SseEmitter tab2 = sseEmitterService.createEmitter("u1");

        assertThat(emitterMap().get("u1")).containsExactly(tab1, tab2);
    }

    @Test
    void createEmitter_shouldKeepUsersSeparated() {
        sseEmitterService.createEmitter("u1");
        sseEmitterService.createEmitter("u2");

        assertThat(emitterMap()).containsOnlyKeys("u1", "u2");
    }

    @Test
    void sendToUser_shouldDoNothing_whenUserHasNoConnection() {
        assertThatCode(() -> sseEmitterService.sendToUser("khong-online", "payload"))
                .doesNotThrowAnyException();
    }

    @Test
    void sendToUser_shouldNotThrow_whenUserHasConnection() {
        sseEmitterService.createEmitter("u1");

        assertThatCode(() -> sseEmitterService.sendToUser("u1", "payload"))
                .doesNotThrowAnyException();

        assertThat(emitterMap().get("u1")).hasSize(1);
    }
}
