package app.store.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import vn.payos.PayOS;
import vn.payos.core.ClientOptions;

@Configuration
public class PayosConfig {
    private static final int TIMEOUT_MS = 15_000;

    @Bean
    PayOS payOS(
            @Value("${payos.client-id}") String clientId,
            @Value("${payos.api-key}") String apiKey,
            @Value("${payos.checksum-key}") String checksumKey) {
        return new PayOS(ClientOptions.builder()
                .clientId(clientId.trim())
                .apiKey(apiKey.trim())
                .checksumKey(checksumKey.trim())
                .timeoutMs(TIMEOUT_MS)
                .build());
    }
}
