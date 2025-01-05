package org.jd.stream.executor;

import org.jline.reader.*;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced SQL terminal with command history and line editing support.
 */
public class SqlTerminal {
    private static final Logger logger = LoggerFactory.getLogger(SqlTerminal.class);
    private final Connection connection;
    private final LineReader lineReader;
    private final History history;
    private final List<String> commandBuffer;
    private volatile boolean isRunning;
    private final Thread inputThread;
    private static final String PROMPT = "sql> ";
    private static final String DEFAULT_HISTORY_FILE = ".sql_history";

    public SqlTerminal(Connection connection) throws IOException {
        this(connection, Paths.get(System.getProperty("user.home"), DEFAULT_HISTORY_FILE));
    }

    public SqlTerminal(Connection connection, Path historyPath) throws IOException {
        Terminal terminal = null;
        try {
            // Try to create a native terminal first
            terminal = TerminalBuilder.builder()
                    .name("SQL-Terminal")
                    .system(true)
                    .nativeSignals(true)
                    .signalHandler(Terminal.SignalHandler.SIG_IGN)
                    .build();
            logger.debug("Created native terminal successfully");
        } catch (IOException e) {
            logger.warn("Unable to create a native terminal, trying jansi fallback", e);
            try {
                // Try with jansi support
                terminal = TerminalBuilder.builder()
                        .name("SQL-Terminal")
                        .jansi(true)
                        .build();
                logger.debug("Created jansi terminal successfully");
            } catch (IOException e2) {
                logger.warn("Unable to create a jansi terminal, falling back to a dumb terminal", e2);
                terminal = TerminalBuilder.builder()
                        .name("SQL-Terminal")
                        .dumb(true)
                        .build();
                logger.debug("Created dumb terminal");
            }
        }

        this.connection = connection;
        this.commandBuffer = new ArrayList<>();
        this.isRunning = true;

        DefaultParser parser = new DefaultParser();
        parser.setEofOnUnclosedQuote(true);
        parser.setEscapeChars(null);
        parser.setQuoteChars(new char[]{'\'', '"'});
        
        this.lineReader = LineReaderBuilder.builder()
                .terminal(terminal)
                .parser(parser)
                .variable(LineReader.HISTORY_FILE, historyPath)
                .variable(LineReader.HISTORY_SIZE, 1000)
                .variable(LineReader.EDITING_MODE, "emacs")
                .variable(LineReader.WORDCHARS, "*?_-.[]~=/&;!#$%^(){}<>")
                .option(LineReader.Option.AUTO_REMOVE_SLASH, true)
                .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
                .option(LineReader.Option.INSERT_TAB, false)
                .option(LineReader.Option.BRACKETED_PASTE, true)
                .build();

        this.history = new DefaultHistory();
        ((DefaultHistory) this.history).attach(this.lineReader);
        
        this.inputThread = new Thread(this::readInput, "SQL-Terminal-Input");
        this.inputThread.setDaemon(true);
    }

    private void readInput() {
        StringBuilder sqlBuffer = new StringBuilder();
        while (isRunning) {
            try {
                String line = lineReader.readLine(PROMPT);
                if (line == null || "exit".equalsIgnoreCase(line.trim())) {
                    logger.info("Exiting SQL client.");
                    System.out.println("Exiting SQL client.");
                    stop();
                    break;
                }

                line = line.trim();
                if (!line.isEmpty()) {
                    if ("help".equalsIgnoreCase(line)) {
                        displayHelp();
                    } else {
                        sqlBuffer.append(line);
                        if (line.endsWith(";")) {
                            String sql = sqlBuffer.toString().trim();
                            processCommand(sql);
                            sqlBuffer.setLength(0); // 清空缓冲区
                        } else {
                            sqlBuffer.append(" ");
                        }
                    }
                } else if (sqlBuffer.length() > 0) {
                    // 如果缓冲区不为空，则执行缓冲区中的SQL
                    String sql = sqlBuffer.toString().trim();
                    if (sql.endsWith(";")) {
                        processCommand(sql);
                        sqlBuffer.setLength(0); // 清空缓冲区
                    }
                }
            } catch (UserInterruptException e) {
                // Ctrl+C
                logger.debug("User interrupted input");
                sqlBuffer.setLength(0); // 清空缓冲区
            } catch (EndOfFileException e) {
                // Ctrl+D
                logger.info("End of file reached. Exiting SQL client.");
                System.out.println("\nExiting SQL client.");
                stop();
                break;
            } catch (Exception e) {
                logger.error("Error processing input: ", e);
                System.err.println("Error: " + e.getMessage());
                sqlBuffer.setLength(0); // 清空缓冲区
            }
        }
    }

    public void start() {
        logger.info("Starting SQL Terminal");
        System.out.println("Connected to the database successfully.");
        System.out.println("Enhanced SQL Terminal (type 'exit' to quit, 'help' for commands)");
        
        // Ensure clean terminal state
        lineReader.getTerminal().writer().println();
        lineReader.getTerminal().writer().flush();
        
        inputThread.start();
        logger.debug("Input thread started");
    }

    public void processCommand(String command) {
        logger.debug("Processing command: {}", command);
        commandBuffer.add(command); // For testing

        if ("help".equalsIgnoreCase(command.trim())) {
            displayHelp();
            return;
        }

        if ("history".equalsIgnoreCase(command.trim())) {
            displayHistory();
            return;
        }

        if ("clear".equalsIgnoreCase(command.trim())) {
            lineReader.getTerminal().writer().print("\033[H\033[2J");
            lineReader.getTerminal().writer().flush();
            return;
        }

        // Execute the SQL command
        executeSql(command);
    }

    private void executeSql(String sql) {
        logger.debug("Executing SQL: {}", sql);
        long startTime = System.currentTimeMillis();
        try {
            SqlExecutor.executeMultiSql(connection, sql, false);
            long endTime = System.currentTimeMillis();
            logger.info("SQL executed in {} ms", endTime - startTime);
        } catch (Exception e) {
            logger.error("SQL Error: ", e);
            System.err.println("SQL Error: " + e.getMessage());
        }
    }

    private void displayHelp() {
        logger.debug("Displaying help information");
        System.out.println("Available commands:");
        System.out.println("  exit                - Exit the SQL terminal");
        System.out.println("  help                - Display this help message");
        System.out.println("  history             - Show command history");
        System.out.println("  clear               - Clear the screen");
        System.out.println("\nSQL commands:");
        System.out.println("  Enter your SQL commands ending with a semicolon (;)");
        System.out.println("  Multi-line commands are supported");
        System.out.println("\nKeyboard shortcuts:");
        System.out.println("  Up/Down             - Navigate through command history");
        System.out.println("  Left/Right          - Move cursor within line");
        System.out.println("  Ctrl+Left/Right     - Move cursor by word");
        System.out.println("  Home/End            - Move to start/end of line");
        System.out.println("  Ctrl+C              - Cancel current input");
        System.out.println("  Ctrl+D              - Exit the terminal");
    }

    private void displayHistory() {
        logger.debug("Displaying command history");
        for (History.Entry entry : history) {
            System.out.printf("%5d  %s%n", entry.index() + 1, entry.line());
        }
    }

    // Test hooks
    public List<String> getCommandBuffer() {
        return new ArrayList<>(commandBuffer);
    }

    public void stop() {
        logger.info("Stopping SQL Terminal");
        isRunning = false;
        try {
            // Save history before stopping
            ((DefaultHistory) history).save();
            
            // Force the terminal to redraw
            lineReader.getTerminal().writer().println();
            lineReader.getTerminal().writer().flush();
            
            // Interrupt the input thread if it's blocked
            inputThread.interrupt();
            
            // Wait for input thread to finish
            if (Thread.currentThread() != inputThread) {
                inputThread.join(1000);
            }
            
            // Close the terminal
            lineReader.getTerminal().close();
        } catch (Exception e) {
            logger.error("Error during shutdown: ", e);
        }
    }

    public History getHistory() {
        return history;
    }
}
