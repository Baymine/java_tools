package org.jd.stream.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for handling pager functionality in the SQL terminal.
 * Uses the system's 'less' command for paging output.
 */
public class PagerUtil {
    private static final Logger logger = LoggerFactory.getLogger(PagerUtil.class);
    private static final String LESS_CMD = "less";
    private static final String[] LESS_OPTS = {"-S", "-R", "-F", "-X"};

    /**
     * Displays the given content using a custom pager command.
     *
     * @param content The content to display
     * @param pagerCommand The pager command to use (e.g., "less -S", "more")
     * @throws IOException if an I/O error occurs
     */
    public static void displayWithCustomPager(String content, String pagerCommand) throws IOException {
        logger.debug("Displaying content with custom pager: {}", pagerCommand);
        
        // Split the pager command into command and arguments
        String[] cmdParts = pagerCommand.trim().split("\\s+");
        
        // Create process builder with command and arguments
        ProcessBuilder pb = new ProcessBuilder(cmdParts);
        
        // Inherit error stream
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        
        Process process = null;
        try {
            process = pb.start();
            
            // Write content to pager
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(process.getOutputStream()))) {
                writer.write(content);
            }
            
            // Wait for pager to complete
            process.waitFor();
            
        } catch (InterruptedException e) {
            logger.error("Pager process interrupted", e);
            Thread.currentThread().interrupt();
        } finally {
            if (process != null && process.isAlive()) {
                process.destroy();
            }
        }
    }

    /**
     * Displays the given content using the system's 'less' command.
     *
     * @param content The content to display
     * @throws IOException if an I/O error occurs
     */
    public static void displayWithPager(String content) throws IOException {
        logger.debug("Displaying content with default pager");
        
        // Create less process
        ProcessBuilder pb = new ProcessBuilder(LESS_CMD);
        for (String opt : LESS_OPTS) {
            pb.command().add(opt);
        }
        
        // Inherit error stream
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        
        Process process = null;
        try {
            process = pb.start();
            
            // Write content to less
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(process.getOutputStream()))) {
                writer.write(content);
            }
            
            // Wait for less to complete
            process.waitFor();
            
        } catch (InterruptedException e) {
            logger.error("Pager process interrupted", e);
            Thread.currentThread().interrupt();
        } finally {
            if (process != null && process.isAlive()) {
                process.destroy();
            }
        }
    }

    /**
     * Checks if the system has the 'less' command available.
     *
     * @return true if 'less' is available, false otherwise
     */
    public static boolean isPagerAvailable() {
        try {
            Process process = new ProcessBuilder(LESS_CMD, "--version")
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();
            
            boolean completed = process.waitFor(5, TimeUnit.SECONDS);
            if (!completed) {
                process.destroy();
                return false;
            }
            
            return process.exitValue() == 0;
        } catch (IOException | InterruptedException e) {
            logger.debug("Pager not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Creates a temporary file with the given content and opens it in the specified pager.
     * Useful for very large content that shouldn't be held in memory.
     *
     * @param content The content to display
     * @param prefix The prefix for the temporary file name
     * @param pagerCommand The pager command to use (optional, uses less if null)
     * @throws IOException if an I/O error occurs
     */
    public static void displayLargeContent(String content, String prefix, String pagerCommand) throws IOException {
        File tempFile = null;
        try {
            tempFile = File.createTempFile(prefix, ".tmp");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
                writer.write(content);
            }
            
            ProcessBuilder pb;
            if (pagerCommand != null) {
                String[] cmdParts = pagerCommand.trim().split("\\s+");
                pb = new ProcessBuilder(cmdParts);
                pb.command().add(tempFile.getAbsolutePath());
            } else {
                pb = new ProcessBuilder(LESS_CMD);
                for (String opt : LESS_OPTS) {
                    pb.command().add(opt);
                }
                pb.command().add(tempFile.getAbsolutePath());
            }
            
            Process process = pb.inheritIO().start();
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                logger.error("Pager process interrupted", e);
                Thread.currentThread().interrupt();
            }
        } finally {
            if (tempFile != null && tempFile.exists()) {
                if (!tempFile.delete()) {
                    logger.warn("Failed to delete temporary file: {}", tempFile);
                    tempFile.deleteOnExit();
                }
            }
        }
    }
} 