package app.store.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Banner extends BaseEntityLong {
    @Column(nullable = false)
    String title;

    String subtitle;

    @Column(nullable = false, columnDefinition = "TEXT")
    String imageUrl;

    String redirectUrl;

    String altText;

    String buttonText;

    @Column(nullable = false)
    @Builder.Default
    Integer displayOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    Boolean isActive = true;

    // BannerMapper map request qua builder của Lombok, mà builder luôn gọi
    // .displayOrder(...)/.isActive(...) kể cả khi request để trống -> giá trị
    // khởi tạo của @Builder.Default bị ghi đè bằng null. Chốt lại ở đây để
    // không phụ thuộc vào việc mapper có truyền null hay không.
    @PrePersist
    @PreUpdate
    void applyDefaults() {
        if (displayOrder == null)
            displayOrder = 0;
        if (isActive == null)
            isActive = true;
    }
}
