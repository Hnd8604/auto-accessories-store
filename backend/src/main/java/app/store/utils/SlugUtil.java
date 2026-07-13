package app.store.utils;

import org.springframework.stereotype.Component;

import java.text.Normalizer;

@Component
public class SlugUtil {

    /**
     * Tạo slug chuẩn như WordPress / Laravel / Django / Shopify
     */
    public String toSlug(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }

        // 1. Trim và đưa về lowercase
        input = input.trim().toLowerCase();

        // 2. Chuẩn hóa riêng chữ đ & Đ
        input = input.replace("đ", "d").replace("Đ", "D");

        // 3. Normalize NFD để tách dấu ra khỏi chữ
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);

        // 4. Loại bỏ toàn bộ dấu kết hợp (accent)
        String withoutDiacritics = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        // 5. Chuyển mọi ký tự không phải chữ cái/số thành "-"
        String slug = withoutDiacritics.replaceAll("[^a-z0-9]+", "-");

        // 6. Loại bỏ "-" ở đầu và cuối
        slug = slug.replaceAll("(^-+|-+$)", "");

        // 7. Gom tất cả dấu gạch ngang liên tiếp thành 1
        slug = slug.replaceAll("-{2,}", "-");

        return slug;
    }


    /**
     * Tạo slug duy nhất (slug, slug-1, slug-2...)
     * existsChecker nhận vào slug và trả về true nếu slug đó đã tồn tại
     */
    public String createUniqueSlug(String baseSlug, java.util.function.Function<String, Boolean> existsChecker) {
        String slug = baseSlug;
        int counter = 1;

        while (existsChecker.apply(slug)) {
            slug = baseSlug + "-" + counter;
            counter++;
        }

        return slug;
    }
}
