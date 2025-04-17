package com.cluster.operations;

import com.cluster.core.DorisCluster;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class VariableConfigOperation implements ConfigOperation {
    @Override
    public Map<String, String> fetchConfig(DorisCluster cluster) throws SQLException {
        return getStringStringMap(cluster.getJdbcUrl(), cluster.getUser(), cluster.getPassword());
    }

    public static Map<String, String> getStringStringMap(String jdbcUrl, String user, String password) throws SQLException {
        Map<String, String> variables = new HashMap<>();
        String query = "SHOW VARIABLES";

        try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                variables.put(rs.getString("Variable_name"), rs.getString("Value"));
            }
        }
        return variables;
    }
} 