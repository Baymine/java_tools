package org.jd.stream.executor;

import org.jline.reader.*;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
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
    private String currentPagerCommand = null;  // Store current pager command

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
                if (line.isEmpty()) {
                    continue;
                }

                // Handle special commands first
                if (line.startsWith("\\")) {
                    String[] parts = line.split("\\s+", 2);
                    handleSpecialCommand(parts[0]);
                    if (parts.length > 1 && !parts[1].trim().isEmpty()) {
                        // Process remaining part as SQL
                        sqlBuffer.append(parts[1].trim());
                    }
                }
                // Handle pager command
                else if (line.toLowerCase().startsWith("pager")) {
                    String[] parts = line.split("\\s+", 3);
                    if (parts.length >= 2) {
                        // Set pager command (parts[1] and optionally parts[2] for additional args)
                        String pagerCmd = parts.length > 2 ? parts[1] + " " + parts[2] : parts[1];
                        handlePagerCommand("pager " + pagerCmd);
                    } else {
                        handlePagerCommand(line);
                    }
                }
                // Handle other commands
                else {
                    sqlBuffer.append(line);
                    if (line.endsWith(";")) {
                        String sql = sqlBuffer.toString().trim();
                        if (sql.equalsIgnoreCase("help;")) {
                            displayHelp();
                        } else if (sql.equalsIgnoreCase("history;")) {
                            displayHistory();
                        } else if (sql.equalsIgnoreCase("clear;")) {
                            lineReader.getTerminal().writer().print("\033[H\033[2J");
                            lineReader.getTerminal().writer().flush();
                        } else if (sql.equalsIgnoreCase("nopager;")) {
                            currentPagerCommand = null;
                            System.out.println("Pager disabled.");
                        } else {
                            processCommand(sql);
                        }
                        sqlBuffer.setLength(0);
                    } else {
                        sqlBuffer.append(" ");
                    }
                }
            } catch (UserInterruptException e) {
                // Ctrl+C
                logger.debug("User interrupted input");
                sqlBuffer.setLength(0);
            } catch (EndOfFileException e) {
                // Ctrl+D
                logger.info("End of file reached. Exiting SQL client.");
                System.out.println("\nExiting SQL client.");
                stop();
                break;
            } catch (Exception e) {
                logger.error("Error processing input: ", e);
                System.err.println("Error: " + e.getMessage());
                sqlBuffer.setLength(0);
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
        command = command.trim();
        
        // Handle empty commands
        if (command.isEmpty()) {
            return;
        }

        // Add to command buffer for testing
        commandBuffer.add(command);

        // Handle special commands
        if (command.startsWith("\\")) {
            handleSpecialCommand(command);
            return;
        }

        // Handle pager command
        if (command.toLowerCase().startsWith("pager")) {
            handlePagerCommand(command);
            return;
        }

        // Handle built-in commands
        switch (command.toLowerCase()) {
            case "help":
                displayHelp();
                return;
            case "history":
                displayHistory();
                return;
            case "clear":
                lineReader.getTerminal().writer().print("\033[H\033[2J");
                lineReader.getTerminal().writer().flush();
                return;
            case "nopager":
                currentPagerCommand = null;
                System.out.println("Pager disabled.");
                return;
        }

        // Execute SQL command
        executeSql(command);
    }

    private void handleSpecialCommand(String command) {
        switch (command.toLowerCase()) {
            case "\\n":
                currentPagerCommand = null;
                System.out.println("Pager disabled.");
                break;
            case "\\p":
                if (currentPagerCommand != null) {
                    System.out.println("Current pager: " + currentPagerCommand);
                } else {
                    System.out.println("No pager set.");
                }
                break;
            default:
                System.out.println("Unknown command: " + command);
                break;
        }
    }

    private void handlePagerCommand(String command) {
        String[] parts = command.split("\\s+", 2);
        if (parts.length > 1) {
            // Extract only the pager command part, not any SQL that might follow
            String pagerCmd = parts[1].split(";")[0].trim();
            currentPagerCommand = pagerCmd;
            System.out.println("Pager set to: " + currentPagerCommand);
        } else {
            currentPagerCommand = "less -S";  // Default pager command
            System.out.println("Pager set to default: less -S");
        }
    }

    private void executeSql(String sql) {
        logger.debug("Executing SQL: {}", sql);
        long startTime = System.currentTimeMillis();
        try {
            // Capture the output in a string builder
            StringBuilder output = new StringBuilder();
            PrintStream originalOut = System.out;
            PrintStream originalErr = System.err;
            
            try {
                // Redirect System.out to our string builder
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(baos);
                System.setOut(ps);
                System.setErr(ps);
                
                // Execute the SQL command
                SqlExecutor.executeMultiSql(connection, sql, false);
                
                // Get the output
                ps.flush();
                output.append(baos.toString());
                
                long endTime = System.currentTimeMillis();
                output.append(String.format("%nQuery executed in %d ms%n", endTime - startTime));
                
            } finally {
                // Restore original output streams
                System.setOut(originalOut);
                System.setErr(originalErr);
            }
            
            // Display the output using current pager if set
            try {
                if (currentPagerCommand != null) {
                    PagerUtil.displayWithCustomPager(output.toString(), currentPagerCommand);
                } else if (PagerUtil.isPagerAvailable()) {
                    PagerUtil.displayWithPager(output.toString());
                } else {
                    System.out.print(output);
                }
            } catch (IOException e) {
                logger.error("Error displaying results with pager", e);
                System.out.print(output);
            }
            
        } catch (Exception e) {
            logger.error("SQL Error: ", e);
            System.err.println("SQL Error: " + e.getMessage());
        }
    }

    private void displayHelp() {
        logger.debug("Displaying help information");
        StringBuilder help = new StringBuilder();
        help.append("Available commands:\n");
        help.append("  exit                - Exit the SQL terminal\n");
        help.append("  help                - Display this help message\n");
        help.append("  history             - Show command history\n");
        help.append("  clear               - Clear the screen\n");
        help.append("  pager [command]     - Set output pager (default: less -S)\n");
        help.append("  nopager             - Disable pager\n");
        help.append("  \\n                  - Disable pager (alternative)\n");
        help.append("  \\p                  - Show current pager setting\n");
        help.append("\nSQL commands:\n");
        help.append("  Enter your SQL commands ending with a semicolon (;)\n");
        help.append("  Multi-line commands are supported\n");
        help.append("\nKeyboard shortcuts:\n");
        help.append("  Up/Down             - Navigate through command history\n");
        help.append("  Left/Right          - Move cursor within line\n");
        help.append("  Ctrl+Left/Right     - Move cursor by word\n");
        help.append("  Home/End            - Move to start/end of line\n");
        help.append("  Ctrl+C              - Cancel current input\n");
        help.append("  Ctrl+D              - Exit the terminal\n");
        
        try {
            if (currentPagerCommand != null) {
                PagerUtil.displayWithCustomPager(help.toString(), currentPagerCommand);
            } else if (PagerUtil.isPagerAvailable()) {
                PagerUtil.displayWithPager(help.toString());
            } else {
                System.out.print(help);
            }
        } catch (IOException e) {
            logger.error("Error displaying help with pager", e);
            System.out.print(help);
        }
    }

    private void displayHistory() {
        logger.debug("Displaying command history");
        StringBuilder content = new StringBuilder();
        content.append("Command History:\n\n");
        for (History.Entry entry : history) {
            content.append(String.format("%5d  %s%n", entry.index() + 1, entry.line()));
        }
        
        try {
            if (PagerUtil.isPagerAvailable()) {
                PagerUtil.displayWithPager(content.toString());
            } else {
                // Fallback to direct output if pager is not available
                System.out.print(content);
            }
        } catch (IOException e) {
            logger.error("Error displaying history with pager", e);
            // Fallback to direct output
            System.out.print(content);
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
