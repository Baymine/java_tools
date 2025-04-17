package com.cluster.operations;

import com.cluster.core.DorisCluster;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class FrontendConfigOperation implements ConfigOperation {
    @Override
    public Map<String, String> fetchConfig(DorisCluster cluster) throws SQLException {
        Map<String, String> config = new HashMap<>();
        String query = "ADMIN SHOW FRONTEND CONFIG";

        try (Connection conn = DriverManager.getConnection(
                cluster.getJdbcUrl(), cluster.getUser(), cluster.getPassword());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                config.put(rs.getString("Key"), rs.getString("Value"));
            }
        }
        return config;
    }
} 