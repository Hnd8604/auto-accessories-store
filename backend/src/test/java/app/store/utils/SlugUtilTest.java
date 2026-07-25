package app.store.utils;

import org.junit.jupiter.api.Test; // annotation đánh dấu đây là 1 test
import static org.assertj.core.api.Assertions.assertThat; // thư viện assert đẹp
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SlugUtilTest {
    // Tạo object cần test. SlugUtil không phụ thuộc gì nên "new" thẳng.
    private final SlugUtil slugUtil = new SlugUtil();

    @Test
    void toSlug_shouldRemoveVietnamAcccents() {
        // Arrange
        String input = "Lốp xe Ô tô";

        // Act
        String result = slugUtil.toSlug(input);

        // Assert
        assertThat(result).isEqualTo("lop-xe-o-to");
    }

    @ParameterizedTest // test chạy 1 lần cho MỖI dòng dữ liệu dưới
    @CsvSource({
            "'Lốp xe Ô tô',        'lop-xe-o-to'",
            "'Đèn LED',            'den-led'", // kiểm luật đ → d
            "'A---B',              'a-b'", // gom nhiều dấu - thành 1
            "'  Trim  Me  ',       'trim-me'"
    })
    void toSlug_shouldConvertVariousInputs(String input, String expected) {
        assertThat(slugUtil.toSlug(input)).isEqualTo(expected);
    }

    @Test
    void createUniqueSlug_shouldAppendNumber_whenBaseAndFirstSuffixExist() {
        // Arrange: giả lập "den-led" và "den-led-1" đã tồn tại, các slug khác thì chưa
        java.util.function.Function<String, Boolean> existsChecker =
                slug -> slug.equals("den-led") || slug.equals("den-led-1");

        // Act
        String result = slugUtil.createUniqueSlug("den-led", existsChecker);

        // Assert
        assertThat(result).isEqualTo("den-led-2");
    }

    @Test
    void createUniqueSlug_shouldReturnBaseSlug_whenNotExists() {
        // Arrange: không slug nào tồn tại
        java.util.function.Function<String, Boolean> existsChecker = slug -> false;

        // Act
        String result = slugUtil.createUniqueSlug("den-led", existsChecker);

        // Assert: giữ nguyên slug gốc, không thêm hậu tố
        assertThat(result).isEqualTo("den-led");
    }
}