// Expected: NOT_EXPLOITABLE — 工具类，无 HTTP 入口调用
package com.example.deadcode;

/**
 * A standalone utility class that is never called by any controller
 * or HTTP entry point. Contains a hardcoded secret inside the
 * getPassword method for testing unreachable findings.
 */
public class DeadCodeUtil {

    private DeadCodeUtil() {
    }

    public String getPassword() {
        // VULNERABLE: hardcoded password inside unreachable method body
        String password = "deadcode-admin-123!";
        return password;
    }

    public String generateReport(String data) {
        return "Report: " + data;
    }
}
