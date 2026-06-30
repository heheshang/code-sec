package com.codesec.common.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CryptoConfig {

    @Value("${codesec.crypto.master-key}")
    private String masterKey;

    @Value("${codesec.crypto.kms.region:cn-hangzhou}")
    private String kmsRegion;

    @Value("${codesec.crypto.kms.key-id:}")
    private String kmsKeyId;

    @Bean
    @ConditionalOnProperty(name = "codesec.crypto.provider", havingValue = "kms")
    public CryptoService kmsCryptoService() {
        return new KmsCryptoService(kmsRegion, kmsKeyId, new AesGcmCryptoService(masterKey));
    }

    @Bean
    @ConditionalOnProperty(name = "codesec.crypto.provider", havingValue = "aes", matchIfMissing = true)
    public CryptoService aesCryptoService() {
        return new AesGcmCryptoService(masterKey);
    }
}
