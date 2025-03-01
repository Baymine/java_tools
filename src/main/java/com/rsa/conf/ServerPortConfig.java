package com.rsa.conf;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Configuration class for managing server port mappings.
 * Uses pattern matching for flexible address matching and enum-based port definitions.
 */
public class ServerPortConfig {
    private static final Map<Pattern, ServerPort> PORT_MAPPINGS = new LinkedHashMap<>();
    
    static {
        // Default port mappings - order matters (more specific patterns first)
        addMapping("^127\\.0\\.0\\.1$|^localhost$", ServerPort.LOCALHOST_PORT);
        addMapping(".*orca\\.jd\\.local.*", ServerPort.ORCA_PORT);
        addMapping(".*olap\\.jd\\.com.*", ServerPort.OLAP_PORT);
        addMapping(".*", ServerPort.DEFAULT_IP_PORT); // Default mapping for IP addresses
    }

    /**
     * Adds a new port mapping pattern.
     * @param pattern The regex pattern to match addresses
     * @param port The port enum value to use for matching addresses
     */
    public static void addMapping(String pattern, ServerPort port) {
        PORT_MAPPINGS.put(Pattern.compile(pattern), port);
    }

    /**
     * Gets the port for a given address based on configured patterns.
     * @param address The server address to get port for
     * @return The matching port number as a string
     */
    public static String getPort(String address) {
        return PORT_MAPPINGS.entrySet().stream()
                .filter(entry -> entry.getKey().matcher(address).matches())
                .findFirst()
                .map(entry -> entry.getValue().getPort())
                .orElse(ServerPort.DEFAULT_IP_PORT.getPort());
    }

    /**
     * Gets the port configuration for a given address.
     * @param address The server address to get port configuration for
     * @return The matching ServerPort enum
     */
    public static ServerPort getPortConfig(String address) {
        return PORT_MAPPINGS.entrySet().stream()
                .filter(entry -> entry.getKey().matcher(address).matches())
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(ServerPort.DEFAULT_IP_PORT);
    }

    /**
     * Clears all existing port mappings.
     */
    public static void clearMappings() {
        PORT_MAPPINGS.clear();
    }

    /**
     * Updates an existing port mapping or adds a new one.
     * @param pattern The regex pattern to match addresses
     * @param port The new port configuration
     */
    public static void updateMapping(String pattern, ServerPort port) {
        addMapping(pattern, port);
    }
} 