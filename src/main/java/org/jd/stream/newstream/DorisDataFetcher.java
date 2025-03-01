package org.jd.stream.newstream;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;

public class DorisDataFetcher {
    public static final String USER = "root$easyolap";
    public static final String PASSWORD = "#3q6831dada2gua?d";
    public static final String HOST_PORT = "drpub931.olap.jd.com:2000";
    public static final String DB = "qC/OvDU6AhrwhPCTR/6IdzLvgWYDgHcMC0EhHVwLVbGVDs/zMx/kJL3j3qh+MbiDfYDaY0+/fs5gcl17RjfZ+Q1fuXNhiEsN0XtPHZ4KgQcC+rIiA+yipwT5i+ZTolDDt7AeQ3zbNDUpCEB9PE2fXEbi9AdL3SQ37hfSD2DgzBQ6Gz9bjqrPhbeLkNZNFCJFL94hN0zu4KQLu6cv5PsvJSlPpWuIXNjP99MllJ6Lc7o=";
    private static final String OUTPUT_FILE_PATH = "/home/caokaihua1/output.txt";

    public static void main(String[] args) {

        // JDBC connection details
        String jdbcUrl = String.format("jdbc:mysql://%s/%s?useSSL=false&serverTimezone=UTC",
                HOST_PORT, DB);
        String user = "root$easyolap"; // Replace with your username
        String password = "#3q6831dada2gua?d"; // Replace with your password

        String outputFile = "OUTPUT_FILE_PATH"; // Output file path

        String sql = "SELECT * FROM hive.dev.app_cs_cse_laga_all_process_sum_d LIMIT 2000000";

        // Use try-with-resources to ensure resources are closed properly
        try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password);
             Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY,
                     ResultSet.CONCUR_READ_ONLY);
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {

            // Enable streaming of result sets
            stmt.setFetchSize(Integer.MIN_VALUE);

            int rowCount = 0;

            // Execute the query
            try (ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    // Fetch data from ResultSet
                    // Replace with actual column names or indices
                    String col1 = rs.getString("shipping_bill_id");
                    String col2 = rs.getString("source");
                    // ... fetch other columns as needed

                    // Write data to file
                    writer.write(col1 + "\t" + col2 + "\n"); // Adjust delimiter and columns as needed

                    rowCount++;

                    // Monitor heap memory usage every 10,000 rows
                    if (rowCount % 10000 == 0) {
                        monitorHeapUsage(rowCount);
                    }
                }
            }

            System.out.println("Data fetching completed. Total rows processed: " + rowCount);

        } catch (SQLException e) {
            System.err.println("Database error occurred:");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("IO error occurred:");
            e.printStackTrace();
        }
    }

    /**
     * Monitors and prints the current heap memory usage.
     *
     * @param rowCount The number of rows processed so far.
     */
    private static void monitorHeapUsage(int rowCount) {
        Runtime runtime = Runtime.getRuntime();

        // Optionally, invoke garbage collector
        // runtime.gc();

        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        System.out.println("Processed " + rowCount + " rows.");
        System.out.println("Heap Memory Usage:");
        System.out.println("  Total Memory: " + (totalMemory / (1024 * 1024)) + " MB");
        System.out.println("  Used Memory: " + (usedMemory / (1024 * 1024)) + " MB");
        System.out.println("  Free Memory: " + (freeMemory / (1024 * 1024)) + " MB");
        System.out.println("---------------------------------------------");
    }
}

