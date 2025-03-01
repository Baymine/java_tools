package org.jd.stream;

import config.ConfigLoader;
import org.jd.stream.dto.Record;
import org.jd.stream.util.JdbcStreamFetcher;
import com.rsa.conf.DatabaseConfig;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.sql.*;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.jd.stream.util.HeapMemoryMonitor;

public class DorisStreamFetchDemo {

    private static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    public static String sourceName = "easyolap";
    private static final ConfigLoader configLoader = ConfigLoader.getInstance();
    public static final DatabaseConfig dbConfig = configLoader.getLakehouseDBConfig(sourceName);


    public static final String USER = dbConfig.getUsername() + "$" + sourceName;
    public static final String PASSWORD = dbConfig.getPassword();
    public static final String HOST_PORT = "drpub931.olap.jd.com:2000";
    public static final String DB = "qC/OvDU6AhrwhPCTR/6IdzLvgWYDgHcMC0EhHVwLVbGVDs/zMx/kJL3j3qh+MbiDfYDaY0+/fs5gcl17RjfZ+Q1fuXNhiEsN0XtPHZ4KgQcC+rIiA+yipwT5i+ZTolDDt7AeQ3zbNDUpCEB9PE2fXEbi9AdL3SQ37hfSD2DgzBQ6Gz9bjqrPhbeLkNZNFCJFL94hN0zu4KQLu6cv5PsvJSlPpWuIXNjP99MllJ6Lc7o=";
    private static String OUTPUT_FILE_PATH = "/home/caokaihua1/output.txt";
    private static final String MONITOR_OUTPUT_FILE_PATH = "/home/caokaihua1/monitor_output.csv";
    private static final String SQL_QUERY = "SELECT * FROM hive.dev.app_cs_cse_laga_all_process_sum_d LIMIT 2000000";

    // Memory Monitoring Interval
    private static final int MEMORY_MONITOR_INTERVAL = 10000; // Check memory every 10,000 rows

    public static Connection connection = null;

    public static void main(String[] args) {

        if (args.length > 0) {
            OUTPUT_FILE_PATH = args[0];
        }

        // Load the JDBC driver
        try {
            Class.forName(JDBC_DRIVER);
            System.out.println("JDBC Driver loaded successfully.");
        } catch (ClassNotFoundException e) {
            System.err.println("Failed to load JDBC Driver.");
            e.printStackTrace();
            return;
        }

        // Execute the streaming query
        try {
//            executeStreamFetch(SQL_QUERY);
            streamFetchDemo();
        } catch (Exception e) {
            System.err.println("IO Exception during streaming fetch:");
            e.printStackTrace();
        }
    }

    /**
     * Escapes special characters in CSV fields such as commas, quotes, and newlines.
     * Encloses the field in double quotes if it contains any special characters.
     *
     * @param fields Iterable of field values to escape.
     * @return Iterable of escaped field values as Strings.
     */
    private static Iterable<String> escapeSpecialCharacters(Iterable<?> fields) {
        java.util.List<String> escaped = new java.util.ArrayList<>();
        for (Object field : fields) {
            if (field == null) {
                escaped.add("");
                continue;
            }
            String data = field.toString();
            String escapedData = data.replace("\"", "\"\""); // Escape double quotes
            if (data.contains(",") || data.contains("\"") || data.contains("\n") || data.contains("\r")) {
                escapedData = "\"" + escapedData + "\""; // Enclose in double quotes
            }
            escaped.add(escapedData);
        }
        return escaped;
    }

    private static void streamFetchDemo() throws SQLException {
        String CONFIG = "query_timeout=300&&useSSL=false&serverTimezone=UTC&useServerPrepStmts=true&useLocalSessionState=true&rewriteBatchedStatements=true&cachePrepStmts=true&prepStmtCacheSqlLimit=99999&prepStmtCacheSize=500";
        String jdbcUrl = String.format("jdbc:mysql://%s/%s?%s",
                HOST_PORT, DB, CONFIG);
        AtomicInteger rowCount = new AtomicInteger();
        connection = DriverManager.getConnection(jdbcUrl, USER, PASSWORD);
        Boolean enableGC = true;
        int limit = 2000000;
        String tableName = "hive.dev.app_cs_cse_laga_all_process_sum_d";

        try (Stream<Record> stream = JdbcStreamFetcher.tableAsStream(connection, tableName, limit);
             BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_FILE_PATH));
             BufferedWriter monitorWriter = new BufferedWriter(new FileWriter(MONITOR_OUTPUT_FILE_PATH))
        ) {
            final boolean[] isHeaderWritten = {false};
            final Set<String> headerColumns = new java.util.LinkedHashSet<>();
            monitorWriter.write("Timestamp,RowCount,TotalMemory(MB),UsedMemory(MB),FreeMemory(MB)\n");
            long start = System.currentTimeMillis();

            stream.forEach(record -> {
                try {
                    Map<String, Object> data = record.getData();

                    if (!isHeaderWritten[0]) {
                        headerColumns.addAll(data.keySet());
                        String header = String.join(",", escapeSpecialCharacters(headerColumns));
                        writer.write(header);
                        writer.newLine();
                        isHeaderWritten[0] = true;
                    }
                    String row = String.join(",", escapeSpecialCharacters(data.values()));
                    writer.write(row);
                    writer.newLine();
                    rowCount.getAndIncrement();
                    if (false && rowCount.get() % 1000 == 0) {
                        HeapMemoryMonitor.monitorHeapUsage(rowCount.get(), null, enableGC);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Error writting to csv file. ", e);
                }
            });

            System.out.println("Total: " + (System.currentTimeMillis() - start)/1000.0);
        } catch (SQLException e) {
            System.err.println("SQL Exception occurred:");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("IO Exception occurred:");
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }


    /**
     * Executes the given SQL query in a streaming manner and writes the results to a file.
     *
     * @param sql The SQL query to execute.
     * @throws IOException If an I/O error occurs.
     */
    private static void executeStreamFetch(String sql) throws IOException {
        String jdbcUrl = String.format("jdbc:mysql://%s/%s?useSSL=false&serverTimezone=UTC&useCursorFetch=true&defaultFetchSize=1000",
                HOST_PORT, DB);

        // Initialize MemoryMXBean for monitoring heap usage
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

        // Initialize row count
        int rowCount = 0;

        // Try-with-resources to ensure all resources are closed automatically
        try (
                Connection connection = DriverManager.getConnection(jdbcUrl, USER, PASSWORD);
                Statement statement = createStreamingStatement(connection);
                ResultSet resultSet = statement.executeQuery(sql);
                BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_FILE_PATH), 16 * 1024 * 1024) // 16MB buffer
        ) {
            System.out.println("Connected to the database successfully.");

            // Get metadata for column information
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            StringBuilder lineBuilder = new StringBuilder(1024); // Initial capacity
            System.out.println("Start =========");
            while (resultSet.next()) {
                rowCount++;
                // Monitor memory usage at defined intervals
                if (rowCount % MEMORY_MONITOR_INTERVAL == 0) {
                    MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
                    long usedMB = heapMemoryUsage.getUsed() / (1024 * 1024);
                    long maxMB = heapMemoryUsage.getMax() / (1024 * 1024);
                    System.out.printf("Processed rows: %d, Used heap memory: %d MB / %d MB%n", rowCount, usedMB, maxMB);
                    if(rowCount % (MEMORY_MONITOR_INTERVAL ) == 0) {
                        System.out.println("=====GC invoke....");
                        Runtime.getRuntime().gc();
                    }
                }

                // Build the line with column values separated by commas
                lineBuilder.setLength(0); // Reset the StringBuilder
                for (int i = 1; i <= columnCount; i++) {
                    String value = resultSet.getString(i);
                    if (value != null) {
                        // Escape double quotes by replacing " with ""
                        value = value.replace("\"", "\"\"");
                        // Enclose in double quotes if value contains comma or double quotes
                        if (value.contains(",") || value.contains("\"")) {
                            value = "\"" + value + "\"";
                        }
                    } else {
                        value = "";
                    }
                    lineBuilder.append(value);
                    if (i < columnCount) {
                        lineBuilder.append(",");
                    } else {
                        lineBuilder.append("\n");
                    }
                }

                // Write the line to the output file
//                writer.write(lineBuilder.toString());
            }

            System.out.println("Streaming fetch completed. Total rows fetched: " + rowCount);
        } catch (SQLException e) {
            System.err.println("SQL Exception during streaming fetch:");
            e.printStackTrace();
        }
    }

    /**
     * Creates a Statement object configured for streaming result sets.
     *
     * @param connection The database connection.
     * @return A streaming-configured Statement.
     * @throws SQLException If a database access error occurs.
     */
    static Statement createStreamingStatement(Connection connection) throws SQLException {
        // Turn off auto-commit for MySQL streaming
        connection.setAutoCommit(false);

        // Create a Statement with TYPE_FORWARD_ONLY and CONCUR_READ_ONLY for optimal streaming
        Statement stmt = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

        // For MySQL streaming, set fetch size to Integer.MIN_VALUE or a positive value if useCursorFetch=true
        // Depending on the driver, you might need to adjust these settings
        stmt.setFetchSize(Integer.MIN_VALUE);

        return stmt;
    }

    private Stream<Record> tableAsStream(Connection connection, String table) {
        return null;
    }

    // Get id of current connection
    private static int getConnectionId(Connection conn) {
        try (Statement statement = conn.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT CONNECTION_ID();");) {
            int connectionId = -1;
            if (resultSet.next()) {
                connectionId = resultSet.getInt(1);
                System.out.println("Current connection id: " + connectionId);
            }
            return connectionId;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // User another connection to kill the query
    private static void killQuery(long id) {
        String jdbcUrl = "jdbc:mysql://" + HOST_PORT + "/" + DB + "?useCursorFetch=true";
        try (Connection conn = DriverManager.getConnection(jdbcUrl, USER, PASSWORD);
                Statement killStatement = conn.createStatement()) {
            String killQuery = "KILL QUERY " + id;
            killStatement.execute(killQuery);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
