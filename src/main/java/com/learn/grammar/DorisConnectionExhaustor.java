package com.learn.grammar;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DorisConnectionExhaustor {
    private static final String JDBC_URL="jdbc:mysql://127.0.0.1:9030?useSSL=false";
    public static final String USERNAME = "root";
    private static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    public static final String LONG_RUNNING_QUERY = "SELECT 1;";
    public static final int MAX_ATTEMPTS = 2000;

    private static final List<Connection> connectionPool = new ArrayList<>();

    public static void simpleExhaustor() {
        List<Connection> connectionPool = new ArrayList<>();
        int successfulConnections = 0;
        int failedConnections = 0;

        try {
            Class.forName(JDBC_DRIVER);
        } catch (ClassNotFoundException e) {
            System.err.println("Driver not found.");
            e.printStackTrace();
            return;
        }
        System.out.println("starting to open connections...");

        for (int i = 1; i <= MAX_ATTEMPTS; i++) {
            try {
                DriverManager.getConnection(JDBC_URL, USERNAME, "");
            } catch (SQLException e) {
                failedConnections++;
                System.err.println("Failed to establish connection #" + i + ": " + e.getMessage());
            }
        }
        System.out.println("\nConnection Attempt Summary:");
        System.out.println("Successful Connections: " + successfulConnections);
        System.out.println("Failed Connections: " + failedConnections);

        // Keep the connections open to maintain the exhaustion
        System.out.println("\nConnections are kept open to maintain the exhaustion state.");
        System.out.println("Press Enter to close all connections and exit.");

        try {
            System.in.read();
        } catch (Exception e) {

        }

        for(Connection conn : connectionPool) {
            try {
                conn.close();
            } catch (SQLException e) {

            }
        }
        System.out.println("All connections closed. Exiting.");
    }

    private static void multiThreadExhaustor() {

    }

    public static void main(String[] args) {

    }
}
