package com.rsa.conf;

/**
 * Enum representing different server ports used in the application.
 * Each port has a value and description for better documentation.
 */
public enum ServerPort {
    LOCALHOST_PORT("9030", "Default port for localhost connections"),
    ORCA_PORT("2006", "Port for orca.jd.local services"),
    OLAP_PORT("2000", "Port for olap.jd.com services"),
    DEFAULT_IP_PORT("9230", "Default port for IP addresses");

    private final String port;
    private final String description;

    ServerPort(String port, String description) {
        this.port = port;
        this.description = description;
    }

    public String getPort() {
        return port;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", port, description);
    }
} 