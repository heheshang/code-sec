// Supports: IndirectController — EXPLOITABLE 调用链末端 (sink)
package com.example.indirect;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * DAO layer containing the actual SQL injection sink.
 * Called through IndirectController → IndirectService → IndirectDao.findById().
 */
public class IndirectDao {

    private final Connection connection;

    public UserDao() {
        this.connection = null;
    }

    public UserDao(Connection connection) {
        this.connection = connection;
    }

    public String findById(String id) {
        try {
            // VULNERABLE: SQL injection through string concatenation
            String sql = "SELECT * FROM users WHERE id = " + id;
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                return rs.getString("name");
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
        return "Not found";
    }
}
