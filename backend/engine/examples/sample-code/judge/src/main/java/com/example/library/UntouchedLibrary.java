// Expected: POTENTIALLY_EXPLOITABLE — 无 HTTP 入口，调用图无法判定
package com.example.library;

/**
 * A standalone library class with no HTTP entry point and no
 * controller callers. Contains a hardcoded secret inside the
 * getApiKey method. When scanned in isolation (no entry points),
 * the judger returns POTENTIALLY_EXPLOITABLE.
 */
public class UntouchedLibrary {

    private UntouchedLibrary() {
    }

    public String getApiKey() {
        // VULNERABLE: hardcoded secret inside method body
        String apiKey = "lib-secret-key-98765";
        return apiKey;
    }

    public String processData(String data) {
        return data;
    }
}
