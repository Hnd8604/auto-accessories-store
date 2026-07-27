package app.store;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Cần Postgres/Redis/Kafka + biến môi trường thật, tách khỏi vòng unit test (xem docs/TESTING.md)")
class StoreApplicationTests {

	@Test
	void contextLoads() {
	}

}
