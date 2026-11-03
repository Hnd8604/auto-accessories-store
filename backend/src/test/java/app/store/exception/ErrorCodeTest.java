package app.store.exception;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCodeTest {

    @Test
    void codesAreUnique() {
        Map<Integer, Long> occurrences = Arrays.stream(ErrorCode.values())
                .collect(Collectors.groupingBy(ErrorCode::getCode, Collectors.counting()));

        assertThat(occurrences)
                .allSatisfy((code, count) -> assertThat(count)
                        .as("error code %s must be unique", code)
                        .isOne());
    }

    @Test
    void codesUseFourDigits() {
        assertThat(ErrorCode.values())
                .allSatisfy(error -> assertThat(error.getCode())
                        .as("%s must use a four-digit code", error)
                        .isBetween(1000, 9999));
    }
}
