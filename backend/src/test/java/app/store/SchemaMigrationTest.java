package app.store;

import jakarta.persistence.Entity;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Chạy Flyway trên một Postgres RỖNG rồi bắt Hibernate `validate` đối chiếu
 * toàn bộ entity với schema vừa tạo. Đây là chốt chặn cho lỗi hay gặp nhất khi
 * dùng Flyway: sửa entity mà quên viết migration (hoặc ngược lại) — app sẽ
 * chết lúc khởi động ở production, còn test này bắt được ngay từ CI.
 *
 * <p>Bỏ qua nếu không có biến TEST_DB_URL, nên `mvn test` ở máy vẫn nhanh và
 * không cần Docker. CI cấp DB qua service container (xem .github/workflows/ci.yml).
 * Chạy tay:
 * <pre>
 * docker run -d --name pg-test -e POSTGRES_PASSWORD=test -e POSTGRES_DB=storetest -p 55432:5432 postgres:16-alpine
 * TEST_DB_URL=jdbc:postgresql://localhost:55432/storetest TEST_DB_USER=postgres TEST_DB_PASSWORD=test \
 *   ./mvnw test -Dtest=SchemaMigrationTest
 * </pre>
 *
 * <p>CẢNH BÁO: test gọi {@code flyway.clean()} — xoá sạch schema của DB được trỏ tới.
 * Chỉ set TEST_DB_URL vào database dùng-một-lần, tuyệt đối không phải DB thật.
 *
 * <p>Tên lớp phải kết thúc bằng {@code Test}. Surefire chỉ quét các mẫu
 * {@code Test...}, {@code ...Test}, {@code ...Tests}, {@code ...TestCase};
 * lớp đặt tên {@code ...IT} sẽ bị bỏ qua âm thầm trong {@code mvn test}
 * vì hậu tố đó dành cho plugin Failsafe.
 */
@EnabledIfEnvironmentVariable(named = "TEST_DB_URL", matches = ".+")
class SchemaMigrationTest {

    private static final String URL = System.getenv("TEST_DB_URL");
    private static final String USER = System.getenv().getOrDefault("TEST_DB_USER", "postgres");
    private static final String PASSWORD = System.getenv().getOrDefault("TEST_DB_PASSWORD", "test");

    @Test
    void migrationsBuildSchemaThatMatchesEntities() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(URL, USER, PASSWORD)
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .load();
        flyway.clean(); // luôn bắt đầu từ schema rỗng
        MigrateResult result = flyway.migrate();

        assertThat(result.migrationsExecuted)
                .as("phải có ít nhất migration V1")
                .isGreaterThanOrEqualTo(1);

        // hbm2ddl=validate ném exception nếu bảng/cột/kiểu dữ liệu lệch so với entity
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.connection.url", URL)
                .applySetting("hibernate.connection.username", USER)
                .applySetting("hibernate.connection.password", PASSWORD)
                .applySetting("hibernate.connection.driver_class", "org.postgresql.Driver")
                .applySetting("hibernate.physical_naming_strategy",
                        "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy")
                .applySetting("hibernate.implicit_naming_strategy",
                        "org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy")
                .applySetting("hibernate.hbm2ddl.auto", "validate")
                .build();

        MetadataSources sources = new MetadataSources(registry);
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
        for (var candidate : scanner.findCandidateComponents("app.store.entity")) {
            sources.addAnnotatedClass(Class.forName(candidate.getBeanClassName()));
        }
        sources.buildMetadata().buildSessionFactory().close();
    }
}
