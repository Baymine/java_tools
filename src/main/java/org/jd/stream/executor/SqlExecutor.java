package org.jd.stream.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * The {@code SqlExecutor} class provides functionality to execute multiple SQL commands
 * against a given database connection. It supports two modes of operation:
 * <ul>
 *     <li><strong>Normal Mode:</strong> Executes SQL commands and displays results or update counts.</li>
 *     <li><strong>Error-Only Mode:</strong> Suppresses normal outputs, prints only errors, and displays the row count of result sets.</li>
 * </ul>
 */
public class SqlExecutor {

    private static final Logger logger = LoggerFactory.getLogger(SqlExecutor.class);

    /**
     * Executes multiple SQL commands and processes their outputs based on the specified mode.
     *
     * @param connection    The {@link Connection} object representing the database connection.
     * @param sql           The SQL command(s) to be executed. Can contain multiple statements separated by semicolons.
     * @param errorOnlyMode A boolean flag indicating the mode of operation:
     *                      {@code false}: Normal mode, {@code true}: Error-only mode.
     */
    public static void executeMultiSql(Connection connection, String sql, boolean errorOnlyMode) {
        logger.debug("Executing SQL in {} mode", errorOnlyMode ? "error-only" : "normal");
        long startTime = System.currentTimeMillis();

        if (!errorOnlyMode) {
            System.out.println("Executing SQL command: " + sql);
        }

        try (Statement statement = connection.createStatement()) {
            boolean hasResultSet = statement.execute(sql);

            do {
                if (hasResultSet) {
                    try (ResultSet resultSet = statement.getResultSet()) {
                        if (errorOnlyMode) {
                            int rowCount = 0;
                            while (resultSet.next()) {
                                rowCount++;
                            }
                            System.out.println("Number of rows: " + rowCount);
                        } else {
                            displayResultSet(resultSet);
                        }
                    }
                } else {
                    int updateCount = statement.getUpdateCount();
                    if (updateCount >= 0) {
                        System.out.println("Update Count: " + updateCount);
                    } else {
                        break;
                    }
                }
                hasResultSet = statement.getMoreResults();
            } while (true);

            long endTime = System.currentTimeMillis();
            System.out.println("All queries executed in " + (endTime - startTime) + " ms.");
        } catch (SQLException e) {
            System.err.println("SQL Error: " + e.getMessage());
            logger.error("SQL Error: ", e);
        }
    }

    /**
     * Interactive SQL client.
     *
     * @param connection the database connection
     */
    public static void interactive(Connection connection) {
        logger.debug("Starting interactive SQL client");
        try {
            SqlTerminal terminal = new SqlTerminal(connection);
            logger.debug("SqlTerminal created successfully");
            
            CountDownLatch latch = new CountDownLatch(1);
            
            Thread terminalThread = new Thread(() -> {
                try {
                    logger.debug("Terminal thread starting");
                    terminal.start();
                    logger.debug("Terminal thread running");
                    
                    // Keep the terminal thread alive until interrupted
                    while (!Thread.currentThread().isInterrupted()) {
                        Thread.sleep(100);
                    }
                    logger.debug("Terminal thread interrupted");
                } catch (InterruptedException e) {
                    logger.debug("Terminal thread interrupted: {}", e.getMessage());
                } catch (Exception e) {
                    logger.error("Error in terminal thread: ", e);
                } finally {
                    logger.debug("Terminal thread finishing");
                    terminal.stop();
                    latch.countDown();
                }
            });
            
            terminalThread.setName("SQL-Terminal-Thread");
            terminalThread.start();
            logger.debug("Terminal thread started");
            
            // Wait for terminal thread to finish
            logger.debug("Main thread waiting for terminal to finish");
            latch.await();
            logger.debug("Terminal finished, exiting interactive mode");
            
        } catch (IOException e) {
            logger.error("Failed to initialize terminal: ", e);
        } catch (InterruptedException e) {
            logger.error("Terminal thread interrupted: ", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("Unexpected error in interactive mode: ", e);
        }
    }

    /**
     * Displays the contents of a {@link ResultSet} in a tabular format.
     *
     * @param resultSet The {@link ResultSet} to be displayed.
     * @throws SQLException If an SQL error occurs while accessing the {@code ResultSet}.
     */
    public static void displayResultSet(ResultSet resultSet) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();

        // Get column names and maximum widths
        List<String> columnNames = new ArrayList<>();
        List<Integer> columnWidths = new ArrayList<>();

        for (int i = 1; i <= columnCount; i++) {
            String columnName = metaData.getColumnLabel(i);
            columnNames.add(columnName);
            columnWidths.add(columnName.length());
        }

        // Get data and update column widths
        List<List<String>> data = new ArrayList<>();
        while (resultSet.next()) {
            List<String> row = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                String value = resultSet.getString(i);
                if (value == null) value = "NULL";
                row.add(value);
                columnWidths.set(i - 1, Math.max(columnWidths.get(i - 1), value.length()));
            }
            data.add(row);
        }

        // Build the output string
        StringBuilder output = new StringBuilder();

        // Print header
        printRowSeparator(output, columnWidths);
        for (int i = 0; i < columnCount; i++) {
            output.append(String.format("| %-" + columnWidths.get(i) + "s ", columnNames.get(i)));
        }
        output.append("|\n");
        printRowSeparator(output, columnWidths);

        // Print data
        for (List<String> row : data) {
            for (int i = 0; i < columnCount; i++) {
                output.append(String.format("| %-" + columnWidths.get(i) + "s ", row.get(i)));
            }
            output.append("|\n");
        }
        printRowSeparator(output, columnWidths);

        output.append(data.size()).append(" row(s) in set\n");

        // Display using pager if available
        try {
            if (PagerUtil.isPagerAvailable()) {
                PagerUtil.displayWithPager(output.toString());
            } else {
                System.out.print(output);
            }
        } catch (IOException e) {
            logger.error("Error displaying results with pager", e);
            System.out.print(output);
        }
    }

    private static void printRowSeparator(StringBuilder output, List<Integer> columnWidths) {
        for (int width : columnWidths) {
            output.append("+-");
            for (int i = 0; i < width; i++) {
                output.append("-");
            }
            output.append("-");
        }
        output.append("+\n");
    }
}
