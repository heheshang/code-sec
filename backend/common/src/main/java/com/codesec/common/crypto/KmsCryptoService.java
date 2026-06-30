package com.codesec.common.crypto;

import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

public class KmsCryptoService implements CryptoService {

    private static final Logger log = LoggerFactory.getLogger(KmsCryptoService.class);
    private static final String KMS_API_VERSION = "2016-01-20";

    private final IAcsClient client;
    private final String regionId;
    private final String keyId;
    private final AesGcmCryptoService fallback;
    private final boolean available;

    public KmsCryptoService(String regionId, String keyId, AesGcmCryptoService fallback) {
        this.regionId = regionId;
        this.keyId = keyId;
        this.fallback = fallback;

        IAcsClient acsClient;
        boolean ok;
        try {
            DefaultProfile profile = DefaultProfile.getProfile(regionId);
            acsClient = new DefaultAcsClient(profile);
            CommonRequest check = new CommonRequest();
            check.setDomain(kmsDomain(regionId));
            check.setVersion(KMS_API_VERSION);
            check.setAction("DescribeKey");
            check.putQueryParameter("KeyId", keyId);
            acsClient.getCommonResponse(check);
            ok = true;
            log.info("KMS client initialized: region={} keyId={}", regionId, keyId);
        } catch (Exception e) {
            log.warn("KMS unavailable, AES fallback: {}", e.getMessage());
            acsClient = null;
            ok = false;
        }
        this.client = acsClient;
        this.available = ok;
    }

    @Override
    public String encrypt(String plain) {
        if (!available) {
            log.warn("KMS unavailable, AES fallback for encrypt");
            return fallback.encrypt(plain);
        }
        try {
            CommonRequest req = new CommonRequest();
            req.setMethod(MethodType.POST);
            req.setDomain(kmsDomain(regionId));
            req.setVersion(KMS_API_VERSION);
            req.setAction("Encrypt");
            req.putQueryParameter("KeyId", keyId);
            req.putQueryParameter("Plaintext", Base64.getEncoder().encodeToString(plain.getBytes()));
            CommonResponse resp = client.getCommonResponse(req);
            if (resp.getHttpStatus() != 200) {
                throw new CryptoException("KMS encrypt returned status " + resp.getHttpStatus(), null);
            }
            return resp.getData();
        } catch (Exception e) {
            log.error("KMS encrypt failed, AES fallback: {}", e.getMessage());
            return fallback.encrypt(plain);
        }
    }

    @Override
    public String decrypt(String cipher) {
        if (!available) {
            log.warn("KMS unavailable, AES fallback for decrypt");
            return fallback.decrypt(cipher);
        }
        try {
            CommonRequest req = new CommonRequest();
            req.setMethod(MethodType.POST);
            req.setDomain(kmsDomain(regionId));
            req.setVersion(KMS_API_VERSION);
            req.setAction("Decrypt");
            req.putQueryParameter("CiphertextBlob", cipher);
            CommonResponse resp = client.getCommonResponse(req);
            if (resp.getHttpStatus() != 200) {
                throw new CryptoException("KMS decrypt returned status " + resp.getHttpStatus(), null);
            }
            return resp.getData();
        } catch (Exception e) {
            log.error("KMS decrypt failed, AES fallback: {}", e.getMessage());
            return fallback.decrypt(cipher);
        }
    }

    @Override
    public void rotate() {
        if (!available) {
            log.warn("KMS unavailable, rotation skipped");
            return;
        }
        try {
            CommonRequest req = new CommonRequest();
            req.setMethod(MethodType.POST);
            req.setDomain(kmsDomain(regionId));
            req.setVersion(KMS_API_VERSION);
            req.setAction("RotateKeyVersion");
            req.putQueryParameter("KeyId", keyId);
            CommonResponse resp = client.getCommonResponse(req);
            if (resp.getHttpStatus() != 200) {
                throw new CryptoException("KMS rotation returned status " + resp.getHttpStatus(), null);
            }
            log.info("KMS key rotation succeeded: keyId={}", keyId);
        } catch (Exception e) {
            throw new CryptoException("KMS key rotation failed for keyId=" + keyId, e);
        }
    }

    @Override
    public boolean isKms() {
        return available;
    }

    private static String kmsDomain(String region) {
        return "kms." + region + ".aliyuncs.com";
    }
}
