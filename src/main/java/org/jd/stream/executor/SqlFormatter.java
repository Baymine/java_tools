package org.jd.stream.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Types;
import java.util.regex.Pattern;

/**
 * Utility class for formatting SQL output with color support.
 */
public class SqlFormatter {
    private static final Logger logger = LoggerFactory.getLogger(SqlFormatter.class);

    // ANSI color codes
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String PURPLE = "\u001B[35m";
    private static final String CYAN = "\u001B[36m";
    private static final String BOLD = "\u001B[1m";

    // Patterns for type detection
    private static final Pattern NUMBER_PATTERN = Pattern.compile("^-?\\d*\\.?\\d+$");
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "^\\d{4}-\\d{2}-\\d{2}([ T]\\d{2}:\\d{2}(:\\d{2}(\\.\\d+)?)?)?([+-]\\d{2}:?\\d{2}|Z)?$");
    private static final Pattern BOOLEAN_PATTERN = Pattern.compile("^(true|false)$", Pattern.CASE_INSENSITIVE);

    /**
     * Format a value based on its SQL type with appropriate coloring.
     *
     * @param value The value to format
     * @param sqlType The SQL type from java.sql.Types
     * @return The formatted value with color codes
     */
    public static String formatValue(String value, int sqlType) {
        if (value == null) {
            return formatNull();
        }

        try {
            switch (sqlType) {
                case Types.INTEGER:
                case Types.BIGINT:
                case Types.DECIMAL:
                case Types.DOUBLE:
                case Types.FLOAT:
                case Types.NUMERIC:
                case Types.REAL:
                case Types.SMALLINT:
                case Types.TINYINT:
                    return formatNumber(value);

                case Types.DATE:
                case Types.TIME:
                case Types.TIMESTAMP:
                    return formatDate(value);

                case Types.BOOLEAN:
                case Types.BIT:
                    return formatBoolean(value);

                case Types.VARCHAR:
                case Types.CHAR:
                case Types.LONGVARCHAR:
                case Types.NCHAR:
                case Types.NVARCHAR:
                case Types.LONGNVARCHAR:
                    return formatString(value);

                default:
                    return value;
            }
        } catch (Exception e) {
            logger.warn("Error formatting value: {}", e.getMessage());
            return value;
        }
    }

    /**
     * Format a value based on its content (auto-detect type).
     *
     * @param value The value to format
     * @return The formatted value with color codes
     */
    public static String formatValue(String value) {
        if (value == null) {
            return formatNull();
        }

        if (NUMBER_PATTERN.matcher(value).matches()) {
            return formatNumber(value);
        }

        if (DATE_PATTERN.matcher(value).matches()) {
            return formatDate(value);
        }

        if (BOOLEAN_PATTERN.matcher(value).matches()) {
            return formatBoolean(value);
        }

        return formatString(value);
    }

    /**
     * Format column headers with bold style.
     *
     * @param header The column header to format
     * @return The formatted header
     */
    public static String formatHeader(String header) {
        return BOLD + header + RESET;
    }

    private static String formatNull() {
        return RED + "NULL" + RESET;
    }

    private static String formatNumber(String value) {
        return BLUE + value + RESET;
    }

    private static String formatDate(String value) {
        return YELLOW + value + RESET;
    }

    private static String formatString(String value) {
        return GREEN + value + RESET;
    }

    private static String formatBoolean(String value) {
        return PURPLE + value + RESET;
    }

    /**
     * Check if the terminal supports ANSI colors.
     *
     * @return true if colors are supported
     */
    public static boolean supportsColors() {
        String term = System.getenv("TERM");
        return term != null && (
                term.contains("color") ||
                term.contains("xterm") ||
                term.contains("ansi") ||
                term.contains("vt100")
        );
    }

    /**
     * Enable or disable color output.
     * When disabled, all format methods will return the original string.
     *
     * @param enabled true to enable colors, false to disable
     */
    public static void setColorEnabled(boolean enabled) {
        // Implementation for color toggle support
        // This will be used when we add configuration options
    }
} 