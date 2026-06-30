package com.codesec.sample;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Sample code demonstrating SQL injection vulnerability.
 * Finding expected at line 17 (executeQuery with string concatenation).
 */
public class UserDao {
    private final Connection connection;

    public UserDao(Connection connection) {
        this.connection = connection;
    }

    // VULNERABLE: String concatenation in executeQuery - line 17
    public ResultSet findUser(String userId) throws Exception {
        String sql = "SELECT * FROM users WHERE id = " + userId;
        Statement stmt = connection.createStatement();
        return stmt.executeQuery(sql);
    }

    // SAFE: Parameterized query using PreparedStatement
    public ResultSet findUserSafe(int userId) throws Exception {
        String sql = "SELECT * FROM users WHERE id = ?";
        var ps = connection.prepareStatement(sql);
        ps.setInt(1, userId);
        return ps.executeQuery();
    }
}
