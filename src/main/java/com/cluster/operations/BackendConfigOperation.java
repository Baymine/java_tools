package com.cluster.operations;

import com.cluster.core.DorisCluster;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Getter
public class BackendConfigOperation implements ConfigOperation {
    private static final Logger LOGGER = Logger.getLogger(BackendConfigOperation.class.getName());
    private final Map<String, String> configTypes = new HashMap<>();
    private static final String BEHttpPort = "8240";
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public Map<String, String> fetchConfig(DorisCluster cluster) throws SQLException {
        List<String> beIp = getBackendIp(cluster, 1);
        if (beIp == null || beIp.isEmpty()) {
            LOGGER.warning("Could not find any active backend node for cluster: " + cluster.getName() + ". Skipping BE config fetch.");
            return new HashMap<>();
        }
        return fetchBeConfig(beIp.get(0));
    }

    public void updateAllBEConfig(DorisCluster cluster, Map<String, String> keyValuePairs) throws SQLException {
        List<String> beIps = getBackendIp(cluster, -1);
        if (beIps == null || beIps.isEmpty()) {
            LOGGER.warning("Could not find any active backend node for cluster: " + cluster.getName() + ". Skipping BE config update.");
            return;
        }
        List<CompletableFuture<Void>> futures = beIps.stream()
                .flatMap(ip -> keyValuePairs.entrySet()
                        .stream()
                        .map(entry -> updateSingleBEConfig(ip, entry.getKey(), entry.getValue())))
                .collect(Collectors.toList());
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    private CompletableFuture<Void> updateSingleBEConfig(String ip, String key, String value) {
        String url = String.format("http://%s:%s/api/update_config?%s=%s", ip, BEHttpPort, key, value);

        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.noBody())
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .thenAccept(response -> {
                    LOGGER.info(String.format("Update config request to %s - Status: %d", url, response.statusCode()));
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        LOGGER.warning("Update config request to " + url + " failed with status: " + response.statusCode());
                    }
                })
                .exceptionally(ex-> {
                    LOGGER.severe(String.format("Failed to send config update request to %s: %s", url, ex.getMessage()));
                    return null;
                });
    }

    /**
     * Retrieves backend IP addresses from the Doris cluster.
     * Logs IPs of backends that are not alive.
     *
     * @param cluster The DorisCluster object.
     * @param count The maximum number of alive backend IPs to return. If -1, return all alive IPs.
     * @return A list of alive backend IP addresses, limited by count. Returns empty list if no alive backends found or an error occurs.
     * @throws SQLException If a database access error occurs.
     */
    private List<String> getBackendIp(DorisCluster cluster, int count) throws SQLException {
        String query = "SHOW BACKENDS";
        List<String> aliveBeIps = new ArrayList<>();
        List<String> deadBeIps = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(
                cluster.getJdbcUrl(), cluster.getUser(), cluster.getPassword());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            ResultSetMetaData metaData = rs.getMetaData();
            String ipColumnName = findIpColumnName(metaData);
            if (ipColumnName == null) {
                LOGGER.warning("Could not determine Backend IP/Host column name from SHOW BACKENDS output for cluster: " + cluster.getName());
                return List.of(); // Return empty list
            }

            boolean hasAliveColumn = hasColumn(metaData, "Alive");

            while (rs.next()) {
                String ip = rs.getString(ipColumnName);
                boolean isAlive = true;

                if (hasAliveColumn) {
                    try {
                        isAlive = rs.getBoolean("Alive");
                    } catch (SQLException e) {
                        try {
                            isAlive = "true".equalsIgnoreCase(rs.getString("Alive"));
                        } catch (SQLException e2) {
                            LOGGER.warning("Could not reliably determine 'Alive' status for backend " + ip + ". Assuming alive. Error: " + e2.getMessage());
                        }
                    }
                }

                if (isAlive) {
                    aliveBeIps.add(ip);
                } else {
                    deadBeIps.add(ip);
                }
            }
        } catch (SQLException e) {
            LOGGER.severe("SQL error getting backend IPs for cluster " + cluster.getName() + ": " + e.getMessage());
            throw e; // Re-throw the exception
        }

        if (!deadBeIps.isEmpty()) {
            LOGGER.warning("The following backend nodes for cluster " + cluster.getName() + " are not alive: " + String.join(", ", deadBeIps));
        }

        if (aliveBeIps.isEmpty()) {
             LOGGER.warning("No alive backend nodes found for cluster: " + cluster.getName());
             return List.of();
        }

        if (count == -1) {
            return aliveBeIps;
        } else if (count > 0) {
            return aliveBeIps.stream().limit(count).collect(Collectors.toList());
        } else {
            return List.of();
        }
    }

    private String findIpColumnName(ResultSetMetaData metaData) throws SQLException {
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            String columnName = metaData.getColumnName(i);
            
            if ("Host".equalsIgnoreCase(columnName) ||
                "IP".equalsIgnoreCase(columnName) ||
                "BackendHost".equalsIgnoreCase(columnName)) {
                return columnName;
            }
        }
        return null;
    }

    private boolean hasColumn(ResultSetMetaData metaData, String columnNameToCheck) throws SQLException {
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            if (columnNameToCheck.equalsIgnoreCase(metaData.getColumnName(i))) {
                return true;
            }
        }
        return false;
    }

    private Map<String, String> fetchBeConfig(String beIp) {
        Map<String, String> config = new HashMap<>();
        
        String apiUrl = String.format("http://%s:%s/api/show_config", beIp, BEHttpPort);

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(apiUrl))
                .timeout(Duration.ofSeconds(15)) // Increased timeout slightly
                .header("Accept", "application/json") 
                .build();

        try {
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == HttpURLConnection.HTTP_OK) {
                String responseBody = response.body();
                if (responseBody != null) {
                    parseBeConfigResponseRegex(responseBody.trim(), config);
                } else {
                    LOGGER.warning("Received empty body from BE config endpoint: " + apiUrl);
                }
            } else {
                LOGGER.warning("HTTP request to " + apiUrl + " failed with response code: " + response.statusCode());
                
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.severe("Error fetching BE config from " + apiUrl + ": " + e.getMessage());
            
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
        return config;
    }

    private void parseBeConfigResponseRegex(String responseBody, Map<String, String> config) throws JsonProcessingException {
        if (responseBody == null || responseBody.length() < 4 || !responseBody.startsWith("[") || !responseBody.endsWith("]")) {
            LOGGER.warning("Unexpected BE config response format: " + responseBody);
            return;
        }
        ObjectMapper mapper = new ObjectMapper();
        List<List<String>> data = mapper.readValue(responseBody, List.class);

        for (List<String> entry : data) {
            String key = entry.get(0);
            String type = entry.get(1);
            String value = entry.get(2);
            if (key != null && !key.isEmpty()) {
                config.put(key, value != null ? value : ""); // Ensure value is not null
                configTypes.put(key, type != null ? type : ""); // Store type
            } else {
                LOGGER.finer("Skipping BE config item with empty key.");
            }
        }

        if (config.isEmpty() && responseBody.length() > 2) { // Only warn if response wasn't just "[]"
            LOGGER.warning("Parsed BE config resulted in an empty map, check format or response body: " + responseBody.substring(0, Math.min(responseBody.length(), 100)));
        }
    }
} 