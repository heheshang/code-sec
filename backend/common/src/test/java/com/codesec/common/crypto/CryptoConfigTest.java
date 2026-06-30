package com.codesec.common.crypto;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class CryptoConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CryptoConfig.class))
            .withPropertyValues("codesec.crypto.master-key=MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=");

    @Test
    void aesProvider_whenPropertyMissing() {
        runner.run(ctx -> {
            CryptoService service = ctx.getBean(CryptoService.class);
            assertThat(service).isNotNull();
            assertThat(service.isKms()).isFalse();
        });
    }

    @Test
    void aesProvider_whenPropertyAes() {
        runner.withPropertyValues("codesec.crypto.provider=aes")
                .run(ctx -> {
                    CryptoService service = ctx.getBean(CryptoService.class);
                    assertThat(service).isNotNull();
                    assertThat(service.isKms()).isFalse();
                });
    }
}
