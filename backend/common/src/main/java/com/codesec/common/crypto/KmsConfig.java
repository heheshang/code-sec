package com.codesec.common.crypto;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * KMS configuration properties bound from {@code codesec.crypto.kms.*}.
 * <p>
 * Example application.yml:
 * <pre>
 * codesec:
 *   crypto:
 *     provider: kms
 *     kms:
 *       region: cn-hangzhou
 *       key-id: your-kms-key-id
 * </pre>
 */
@ConfigurationProperties(prefix = "codesec.crypto.kms")
public class KmsConfig {

    /** Alibaba Cloud KMS region, e.g. cn-hangzhou, ap-southeast-1. */
    private String region = "cn-hangzhou";

    /** The KMS key ID (Customer Master Key) used for encryption/decryption. */
    private String keyId = "";

    /** Connection timeout in milliseconds. */
    private int connectTimeout = 5000;

    /** Read timeout in milliseconds. */
    private int readTimeout = 10000;

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }
}
