// Expected: NOT_EXPLOITABLE — @PreAuthorize 保护
package com.example.protection;

import java.sql.Connection;
import java.sql.Statement;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Demonstrates a protected controller with @PreAuthorize annotation.
 * Although the method accepts user input via @RequestParam and
 * passes it into a SQL query, the method-level Spring Security
 * @PreAuthorize annotation should mark the finding as NOT_EXPLOITABLE.
 */
@RestController
public class ProtectedController {

    private final Connection connection;

    public ProtectedController(Connection connection) {
        this.connection = connection;
    }

    @GetMapping("/api/admin/query")
    @PreAuthorize("hasRole('ADMIN')")
    public String searchUser(@RequestParam("name") String name) throws Exception {
        // VULNERABLE: SQL injection with string concatenation
        String sql = "SELECT * FROM users WHERE name = '" + name + "'";
        Statement stmt = connection.createStatement();
        stmt.executeQuery(sql);
        return "Search completed";
    }
}
