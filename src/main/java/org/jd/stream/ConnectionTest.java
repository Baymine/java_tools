package org.jd.stream;

import java.sql.*;
import java.util.List;
import java.util.Properties;


// Commenting out static import, will manage connection locally
// import static org.jd.stream.DorisStreamFetchDemo.connection;

public class ConnectionTest {

    static private final String id = String.valueOf(10);

    // --- Connection Details (Update with your actual Doris FE info) ---
    private static final String DORIS_DB_URL_TEMPLATE = "jdbc:mysql://%s:%s/%s"; // Standard MySQL/Doris JDBC URL
    private static final List<String> FE_IPS = List.of("127.0.0.1"); // Replace with actual FE IPs/Hostnames
    private static final String FE_PORT = "9030"; // Default Doris FE MySQL port
    private static final String DATABASE_NAME = "your_database"; // Replace with your database name
    private static final String USERNAME = "your_username"; // Replace with your username
    private static final String PASSWORD = "your_password"; // Replace with your password
    // --- End Connection Details ---

    public static void main(String[] args) { // Removed throws SQLException, handled internally now
        Connection connection = null; // Define connection locally
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            // --- Establish Connection ---
            // Class.forName("com.mysql.cj.jdbc.Driver"); // Ensure driver is loaded if needed (usually automatic with JDBC 4+)
            String dbUrl = String.format(DORIS_DB_URL_TEMPLATE, FE_IPS.get(0), FE_PORT, DATABASE_NAME);
            Properties properties = new Properties();
            properties.put("user", USERNAME);
            properties.put("password", PASSWORD);
            // Add other necessary properties if needed from JdbcDemo
            // properties.put("useSSL", "true");
            // properties.put("socketKeepAlive", "true");

            System.out.println("Connecting to database: " + dbUrl);
            connection = DriverManager.getConnection(dbUrl, properties);
            System.out.println("Database connection established");
            // --- Connection Established ---


            // --- Execute Query (Existing Logic) ---
            // Note: Sending multiple statements separated by ';' might not work by default with all JDBC drivers/configs.
            // You might need allowMultiQueries=true property or execute them separately.
            // For now, let's execute SET DIALECT first.
            try (Statement stmt = connection.createStatement()) {
                 String dialectValue = "doris";
                 stmt.execute("SET sql_dialect = '" + dialectValue + "'");
                 System.out.println("SQL dialect set to: " + dialectValue);
            }

            String sql = "SELECT * FROM your_table WHERE id = ?"; // Use your actual table name
            pstmt = connection.prepareStatement(sql);
            // Parameters are now 1-based for the SELECT statement
            pstmt.setInt(1, Integer.parseInt(id));

            System.out.println("Executing query: " + sql + " with id = " + id);
            rs = pstmt.executeQuery();
            System.out.println("Query executed successfully");

            // --- Process ResultSet (Example) ---
             while (rs.next()) {
                 // Replace with your actual column names and types
                 System.out.println("Found row: " + rs.getString(1));
             }
            // --- End Process ResultSet ---

        } catch (SQLException e) {
            System.err.println("SQL Error occurred: " + e.getMessage());
            e.printStackTrace();
        // } catch (ClassNotFoundException e) { // Uncomment if you use Class.forName
        //    System.err.println("JDBC Driver not found: " + e.getMessage());
        //    e.printStackTrace();
        } finally {
            // --- Close Resources ---
            try {
                if (rs != null) rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                if (connection != null) connection.close();
                 System.out.println("Database connection closed");
            } catch (SQLException e) {
                e.printStackTrace();
            }
            // --- End Close Resources ---
        }
    }
}
