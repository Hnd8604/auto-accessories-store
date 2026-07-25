package app.store.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.data.domain.Sort;
import static org.assertj.core.api.Assertions.assertThat;

public class SortUtilsTest {
    @ParameterizedTest
    @CsvSource({
            "'name,ASC',   name,  ASC",
            "'price,desc', price, DESC",
            "id,           id,    ASC"
    })
    void buildSort_shouldParseFieldAndDirection(String input, String expectedField, String expectedDirection) {
        // Act
        Sort result = SortUtils.buildSort(input);

        // Assert
        Sort.Order order = result.getOrderFor(expectedField);
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.valueOf(expectedDirection));
    }

    @Test
    void buildSort_shouldFallbackToIdAsc_whenNull() {
        // Act
        Sort result = SortUtils.buildSort(null);

        // Assert
        assertThat(result).isEqualTo(Sort.by(Sort.Direction.ASC, "id"));
    }

    @Test
    void buildSort_shouldFallbackToIdAsc_whenGarbageInput() {
        // Act
        Sort result = SortUtils.buildSort("abc,xyz123");

        // Assert
        assertThat(result).isEqualTo(Sort.by(Sort.Direction.ASC, "id"));
    }
}
