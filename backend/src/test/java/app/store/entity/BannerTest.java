package app.store.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BannerTest {

    @Test
    void applyDefaults_shouldFillNullsLeftByMapper() {
        // BannerMapper dựng Banner qua builder và truyền cả null -> @Builder.Default bị ghi đè
        Banner banner = Banner.builder().title("t").displayOrder(null).isActive(null).build();

        banner.applyDefaults();

        assertThat(banner.getDisplayOrder()).isZero();
        assertThat(banner.getIsActive()).isTrue();
    }

    @Test
    void applyDefaults_shouldKeepExplicitValues() {
        Banner banner = Banner.builder().title("t").displayOrder(5).isActive(false).build();

        banner.applyDefaults();

        assertThat(banner.getDisplayOrder()).isEqualTo(5);
        assertThat(banner.getIsActive()).isFalse();
    }
}
