package org.jd.stream.executor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.Assert.*;

public class SqlTerminalTest {
    private Connection connection;
    private Path historyPath;
    private SqlTerminal terminal;
    private ExecutorService executor;

    @Before
    public void setUp() throws Exception {
        // Create a new in-memory H2 database for each test
        connection = DriverManager.getConnection("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        Statement stmt = connection.createStatement();
        
        // Drop existing tables if they exist
        stmt.execute("DROP TABLE IF EXISTS test");
        stmt.execute("DROP TABLE IF EXISTS employees");
        stmt.execute("DROP TABLE IF EXISTS test_table");
        stmt.execute("DROP TABLE IF EXISTS users");
        
        // Create test tables
        stmt.execute("CREATE TABLE test (id INT PRIMARY KEY, name VARCHAR(255))");
        stmt.execute("INSERT INTO test VALUES (1, 'Test1'), (2, 'Test2')");
        
        stmt.execute("CREATE TABLE employees (id INT PRIMARY KEY, name VARCHAR(255), city VARCHAR(255))");
        stmt.execute("INSERT INTO employees VALUES (1, '张三', 'São Paulo')");
        
        stmt.execute("CREATE TABLE test_table (id INT PRIMARY KEY, column_name VARCHAR(255))");
        stmt.execute("INSERT INTO test_table VALUES (101, 'test value')");
        
        stmt.execute("CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(255), city VARCHAR(255))");
        stmt.execute("INSERT INTO users VALUES (1, 'O''Connor', 'New York')");
        
        // Create a temporary history file
        Path tempDir = Files.createTempDirectory("sql_terminal_test");
        historyPath = tempDir.resolve(".sql_history");
        
        // Initialize terminal with test configuration using a dumb terminal
        Terminal dumbTerminal = TerminalBuilder.builder()
                .dumb(true)
                .build();
        
//        terminal = new SqlTerminal(connection, historyPath, dumbTerminal);
        executor = Executors.newSingleThreadExecutor();
    }

    @After
    public void tearDown() throws Exception {
        if (terminal != null) {
            terminal.stop();
        }
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
        if (executor != null) {
            executor.shutdownNow();
        }
        if (historyPath != null) {
            Files.deleteIfExists(historyPath);
            Files.deleteIfExists(historyPath.getParent());
        }
    }

    @Test
    public void testHistoryPersistence() throws IOException {
        // Test command history
        terminal.processCommand("SELECT * FROM test");
        terminal.processCommand("SELECT id FROM test");
        terminal.processCommand("SELECT name FROM test");

        List<String> commands = terminal.getCommandBuffer();
        assertEquals(3, commands.size());
        assertEquals("SELECT * FROM test", commands.get(0));
        assertEquals("SELECT id FROM test", commands.get(1));
        assertEquals("SELECT name FROM test", commands.get(2));
    }

    @Test
    public void testBasicCommands() throws IOException {
        // Test help command
        terminal.processCommand("help");
        assertTrue(terminal.getCommandBuffer().contains("help"));

        // Test history command
        terminal.processCommand("history");
        assertTrue(terminal.getCommandBuffer().contains("history"));

        // Test clear command
        terminal.processCommand("clear");
        assertTrue(terminal.getCommandBuffer().contains("clear"));
    }

    @Test
    public void testErrorHandling() throws IOException {
        // Test invalid SQL
        terminal.processCommand("SELECT * FROM nonexistent_table");
        
        // Test valid SQL
        terminal.processCommand("SELECT * FROM test");
        
        List<String> commands = terminal.getCommandBuffer();
        assertEquals(2, commands.size());
        assertTrue(commands.get(0).contains("nonexistent_table")); // Error case
        assertTrue(commands.get(1).contains("test")); // Valid case
    }

    @Test
    public void testAsyncOperation() throws Exception {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch stopLatch = new CountDownLatch(1);
        
        Future<?> terminalFuture = executor.submit(() -> {
            try {
                startLatch.countDown();
                terminal.start();
                stopLatch.countDown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        
        // Wait for terminal to start
        assertTrue("Terminal failed to start", startLatch.await(5, TimeUnit.SECONDS));
        Thread.sleep(100); // Give terminal time to initialize
        
        // Send a test command
        terminal.processCommand("SELECT * FROM test");
        
        // Stop the terminal
        terminal.stop();
        
        // Wait for terminal to stop
        assertTrue("Terminal failed to stop", stopLatch.await(5, TimeUnit.SECONDS));
        terminalFuture.get(1, TimeUnit.SECONDS);
        
        // Verify the command was processed
        List<String> commands = terminal.getCommandBuffer();
        assertEquals(1, commands.size());
        assertTrue(commands.get(0).contains("SELECT * FROM test"));
    }

    @Test
    public void testLineEditing() throws Exception {
        // Test SQL query with special characters and spaces
        terminal.processCommand("SELECT * FROM test_table WHERE column_name = 'test value' AND id > 100;");
        List<String> commands = terminal.getCommandBuffer();
        assertEquals(1, commands.size());
        assertTrue(commands.get(0).contains("test value"));

        // Test SQL query with single quotes and spaces
        terminal.processCommand("SELECT * FROM users WHERE name = 'O''Connor' AND city = 'New York';");
        commands = terminal.getCommandBuffer();
        assertEquals(2, commands.size());
        assertTrue(commands.get(1).contains("O''Connor"));
    }

    @Test
    public void testUnicodeSupport() throws Exception {
        // Test SQL with Unicode characters
        String unicodeSql = "SELECT * FROM employees WHERE name = '张三' AND city = 'São Paulo';";
        terminal.processCommand(unicodeSql);
        
        List<String> commands = terminal.getCommandBuffer();
        assertEquals(1, commands.size());
        assertEquals(unicodeSql, commands.get(0));
    }

    @Test
    public void testLongLines() throws Exception {
        // Create a long SQL query
        StringBuilder longSql = new StringBuilder("SELECT ");
        for (int i = 0; i < 50; i++) {
            if (i > 0) longSql.append(", ");
            longSql.append("column").append(i);
        }
        longSql.append(" FROM very_long_table_name WHERE condition = true;");

        terminal.processCommand(longSql.toString());
        
        List<String> commands = terminal.getCommandBuffer();
        assertEquals(1, commands.size());
        assertEquals(longSql.toString(), commands.get(0));
    }
} 