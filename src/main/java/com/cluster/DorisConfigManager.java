package com.cluster;

import com.rsa.conf.DatabaseConfig;
import config.ConfigLoader;
import com.cluster.core.DorisCluster;
import com.cluster.operations.ConfigOperation;
import com.cluster.observers.ConfigurationObserver;
import com.cluster.observers.ConsoleLogger;
import com.cluster.factories.ConfigOperationFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DorisConfigManager {
    private static final Logger LOGGER = LogManager.getLogger(DorisConfigManager.class);
    private static final ConfigLoader configLoader = ConfigLoader.getInstance();

    public static final DatabaseConfig dbConfig = configLoader.getLakehouseDBConfig("local");

    @FunctionalInterface
    private interface ConfigAlignmentWithType {
        void align(DorisCluster cluster, Map<String, String> referenceConfig, String configType) throws SQLException;
    }

    private final List<ConfigurationObserver> observers = new ArrayList<>();
    private final DorisCluster referenceCluster;
    private final List<DorisCluster> targetClusters;

    public DorisConfigManager(DorisCluster referenceCluster, List<DorisCluster> targetClusters) {
        if (referenceCluster == null) {
            throw new IllegalArgumentException("Reference cluster cannot be null.");
        }
        if (targetClusters == null) {
            this.targetClusters = Collections.emptyList();
             LOGGER.warn("Target clusters list was null, using empty list.");
        } else {
             this.targetClusters = targetClusters.stream()
                                                .filter(Objects::nonNull)
                                                .collect(Collectors.toCollection(ArrayList::new));
             if (this.targetClusters.size() != targetClusters.size()) {
                  LOGGER.warn("Null entries found in target clusters list were removed.");
             }
        }
        this.referenceCluster = referenceCluster;

        this.observers.add(new ConsoleLogger());
    }

    public void addObserver(ConfigurationObserver observer) {
        if (observer != null) {
            observers.add(observer);
        }
    }

    private void notifyObservers(String message) {
        new ArrayList<>(observers).forEach(observer -> {
            try {
                observer.onConfigurationChange(message);
            } catch (Exception e) {
                LOGGER.warn("Observer {} failed during notification: {}", observer.getClass().getName(), e.getMessage(), e);
            }
        });
    }

    /**
     * Fetches configurations and prints suggested SET commands to align target clusters
     * with the reference cluster to the standard output. Also prints corresponding
     * rollback commands.
     * @throws SQLException If database errors occur during config fetching.
     */
    public void printAlignmentSuggestions() throws SQLException {
        notifyObservers("Fetching reference configurations from: " + referenceCluster.getName());
        // Fetch reference configurations using the factory
        Map<String, String> referenceVariables = fetchConfigWithRetry(referenceCluster, "variable");
        Map<String, String> referenceFrontendConfigs = fetchConfigWithRetry(referenceCluster, "frontend");
        Map<String, String> referenceBackendConfigs = fetchConfigWithRetry(referenceCluster, "backend");


        ConfigAlignmentWithType printAlignmentLogic = this::printAlignmentCommandsForType;

        for (DorisCluster targetCluster : targetClusters) {
            notifyObservers("Generating alignment suggestions for cluster: " + targetCluster.getName());

            try {
                 printAlignmentLogic.align(targetCluster, referenceVariables, "GLOBAL");
                 printAlignmentLogic.align(targetCluster, referenceFrontendConfigs, "FRONTEND CONFIG");
                 printAlignmentLogic.align(targetCluster, referenceBackendConfigs, "BACKEND CONFIG");
            } catch (SQLException e) {
                 LOGGER.error("Failed to generate alignment suggestions for cluster {}: {}", targetCluster.getName(), e.getMessage(), e);
                 // Decide whether to continue with the next cluster or rethrow/stop
                 // continue; // skip this cluster on error
                 // throw e; // stop processing entirely
            }
        }
    }


    /**
     * Prints SET and Rollback commands for a specific config type (Variable, Frontend, Backend)
     * by comparing a target cluster's current config against a reference config.
     *
     * @param cluster The target cluster.
     * @param referenceConfig The reference configuration map.
     * @param configType A string identifier (e.g., "GLOBAL", "FRONTEND CONFIG", "BACKEND CONFIG").
     * @throws SQLException If fetching the target cluster's current configuration fails.
     */
    private void printAlignmentCommandsForType(DorisCluster cluster, Map<String, String> referenceConfig, String configType)
            throws SQLException { // Let SQLException propagate

        if (configType == null || configType.isBlank()) {
             LOGGER.warn("Config type is null or blank for alignment printing on cluster: {}", cluster.getName());
             return;
        }
         if (referenceConfig == null || referenceConfig.isEmpty()) {
             LOGGER.info("Reference config for type '{}' is null or empty. Skipping alignment printing for cluster {}", configType, cluster.getName());
             return;
         }

        String operationType = configType.toLowerCase()
                                .replace(" config", "")
                                .replace("global", "variable");

        Map<String, String> currentConfig;
        try {
            // Fetch current config using the factory, with retry logic
            currentConfig = fetchConfigWithRetry(cluster, operationType);
            if (currentConfig == null) {
                 LOGGER.error("Failed to fetch current {} config for cluster {} after retries. Skipping alignment commands for this type.", configType, cluster.getName());
                 return;
            }
        } catch (IllegalArgumentException e) {
             // This indicates a programming error (bad configType mapping)
             LOGGER.error("Invalid operation type '{}' derived from config type '{}'. Cannot fetch current config.", operationType, configType, e);
             return; // Cannot proceed
        }

        String configTypeUpper = configType.toUpperCase();
        System.out.println("\n-- === SET " + configTypeUpper + " Commands for cluster " + cluster.getName() + " ===");

        List<String> sortedRefKeys = new ArrayList<>(referenceConfig.keySet());
        Collections.sort(sortedRefKeys);
        boolean setCommandsGenerated = false;

        for(String key : sortedRefKeys) {
            String refValue = referenceConfig.get(key);
            String currentValue = currentConfig.get(key); // Might be null if key doesn't exist in target

            if (!Objects.equals(refValue, currentValue)) {
                 String escapedValue = escapeSqlValue(refValue);
                 String setQuery = String.format("SET %s %s = %s;", configTypeUpper, key, escapedValue);
                 System.out.println(setQuery);
                 setCommandsGenerated = true;
            }
        }
         if (!setCommandsGenerated) {
             System.out.println("-- No SET commands needed for " + configTypeUpper + ".");
         }


        System.out.println("\n-- === Rollback Commands for " + configTypeUpper + " (" + cluster.getName() + ") ===");
        // Use sorted keys from current config for deterministic rollback command output
        List<String> sortedCurrentKeys = new ArrayList<>(currentConfig.keySet());
        Collections.sort(sortedCurrentKeys);
        boolean rollbackCommandsGenerated = false;

        for (String key : sortedCurrentKeys) {
            String currentValue = currentConfig.get(key);
            // Only generate rollback if the key existed in the reference AND the value differs
            if (referenceConfig.containsKey(key) && !Objects.equals(currentValue, referenceConfig.get(key))) {
                 String escapedValue = escapeSqlValue(currentValue);
                 String rollbackQuery = String.format("SET %s %s = %s;", configTypeUpper, key, escapedValue);
                 System.out.println(rollbackQuery);
                 rollbackCommandsGenerated = true;
            }
        }
         if (!rollbackCommandsGenerated) {
             System.out.println("-- No Rollback commands needed for " + configTypeUpper + " (values match reference or key not in reference).");
         }
    }

    /**
     * Escapes a value for safe inclusion in a SQL SET command string.
     * Handles nulls and basic string quoting.
     * Assumes numeric values don't need quotes.
     * WARNING: This is basic and might not cover all edge cases or prevent all SQL injection.
     * Consider using PreparedStatement if executing commands programmatically.
     *
     * @param value The value to escape.
     * @return The escaped value, potentially quoted.
     */
    private String escapeSqlValue(String value) {
        if (value == null) {
            return "NULL";
        }
        // Simple check: if it's not purely numeric (integer or decimal), quote it.
        // This needs refinement for booleans ('true'/'false'), hex, etc. depending on Doris syntax.
        if (!value.matches("-?\\d+(\\.\\d+)?")) {
            // Escape single quotes within the string by doubling them
            return "'" + value.replace("'", "''") + "'";
        }
        return value;
    }


    /**
     * Saves configuration differences between the reference cluster and target clusters to a CSV file.
     * Includes keys present in reference or any target cluster.
     *
     * @param filePath The path where the CSV file will be saved.
     * @throws SQLException If database errors occur during config fetching.
     * @throws IOException If file writing errors occur.
     */
    public void saveDifferencesToCsv(String filePath) throws SQLException, IOException {
        if (filePath == null || filePath.isBlank()) {
            throw new IOException("CSV file path cannot be null or empty.");
        }

        notifyObservers("Fetching reference configurations for CSV report from: " + referenceCluster.getName());
        Map<String, String> referenceVariables = fetchConfigWithRetry(referenceCluster, "variable");
        Map<String, String> referenceFrontendConfigs = fetchConfigWithRetry(referenceCluster, "frontend");
        Map<String, String> referenceBackendConfigs = fetchConfigWithRetry(referenceCluster, "backend");

        List<String[]> csvData = new ArrayList<>();

        List<String> headers = new ArrayList<>();
        headers.add("Config Key");
        headers.add("Config Type");
        headers.add(referenceCluster.getName() + " (Reference)");
        targetClusters.forEach(cluster -> headers.add(cluster.getName()));
        csvData.add(headers.toArray(new String[0]));

        // --- Process differences for each config type ---
        notifyObservers("Processing Variable differences...");
        processConfigDifferencesForCsv(referenceVariables, "Variable", csvData);
        notifyObservers("Processing Frontend differences...");
        processConfigDifferencesForCsv(referenceFrontendConfigs, "Frontend", csvData);
        notifyObservers("Processing Backend differences...");
        processConfigDifferencesForCsv(referenceBackendConfigs, "Backend", csvData /*, referenceBeValueTypes */); // Pass type map if needed

        writeCsvFile(filePath, csvData);
    }

    /**
     * Helper method to process differences for a specific configuration type and add rows to the CSV data.
     *
     * @param referenceConfig The reference configuration map.
     * @param configType The string identifier (e.g., "Variable").
     * @param csvData The list to add CSV row data to.
     * @param refValueTypesOptional Optional map of value types (e.g., from BackendConfigOperation).
     * @throws SQLException If fetching target configurations fails.
     */
    private void processConfigDifferencesForCsv(Map<String, String> referenceConfig, String configType, List<String[]> csvData, Map<String, String>... refValueTypesOptional)
            throws SQLException {

        if (referenceConfig == null) {
             LOGGER.warn("Reference config for type '{}' is null. Skipping CSV processing for this type.", configType);
             referenceConfig = Collections.emptyMap();
        }

         Map<String, String> refValueTypes = (refValueTypesOptional.length > 0 && refValueTypesOptional[0] != null)
                                             ? refValueTypesOptional[0] : Collections.emptyMap();

        String operationType = configType.toLowerCase();

        Set<String> allKeys = new HashSet<>(referenceConfig.keySet());

        Map<DorisCluster, Map<String, String>> targetConfigs = new HashMap<>();
        for (DorisCluster targetCluster : targetClusters) {
            Map<String, String> targetConfig = null;
            try {
                notifyObservers("Fetching " + configType + " config for target cluster " + targetCluster.getName() + " (CSV report)");
                targetConfig = fetchConfigWithRetry(targetCluster, operationType);
                if (targetConfig != null) {
                     targetConfigs.put(targetCluster, targetConfig);
                     allKeys.addAll(targetConfig.keySet());
                 } else {
                     targetConfigs.put(targetCluster, null); // Mark as fetch failure
                     LOGGER.warn("CSV: Could not fetch {} config for target {} after retries.", configType, targetCluster.getName());
                 }
            } catch (SQLException e) {
                 LOGGER.error("CSV: SQL error fetching {} config for target {}: {}. Reporting as N/A.", configType, targetCluster.getName(), e.getMessage(), e);
                 targetConfigs.put(targetCluster, null); // Mark as fetch failure due to SQL error
            } catch (IllegalArgumentException e) {
                 LOGGER.error("CSV: Invalid operation type '{}' for diff processing. Skipping type.", operationType, e);
                 return; // Cannot proceed with this config type
            }
        }

        // Sort keys for consistent CSV output
        List<String> sortedKeys = new ArrayList<>(allKeys);
        Collections.sort(sortedKeys);

        final String NOT_IN_REF = "N/A (Not in Ref)";
        final String NOT_IN_TARGET = "N/A (Not in Target)";
        final String FETCH_FAILED = "N/A (Fetch Failed)";

        for (String key : sortedKeys) {
            String refValue = referenceConfig.get(key);
            String valueType = refValueTypes.getOrDefault(key, "");

            List<String> row = new ArrayList<>();
            row.add(key);
            row.add(configType);

            // Determine reference value representation for CSV
            String refValueCsv = (refValue != null) ? refValue : NOT_IN_REF;
            row.add(refValueCsv);

            boolean rowHasDifference = false;
            boolean presentInAllRef = (refValue != null);
            boolean presentInAllTargets = true;

            for (DorisCluster targetCluster : targetClusters) {
                Map<String, String> targetConfigMap = targetConfigs.get(targetCluster);
                String targetValue = null;
                String targetValueCsv;

                if (targetConfigMap == null) {
                    targetValueCsv = FETCH_FAILED;
                    presentInAllTargets = false;
                } else {
                    targetValue = targetConfigMap.get(key); // Null if key not in this specific target
                    targetValueCsv = (targetValue != null) ? targetValue : NOT_IN_TARGET;
                    if (targetValue == null) {
                        presentInAllTargets = false;
                    }
                }
                row.add(targetValueCsv);

                if (refValue != null && !Objects.equals(refValue, targetValue)) {
                    rowHasDifference = true;
                }

                 if (targetConfigMap != null && ((refValue == null && targetValue != null) || (refValue != null && targetValue == null))) {
                    rowHasDifference = true;
                }
            }

             if (rowHasDifference || !presentInAllRef || !presentInAllTargets) {
                csvData.add(row.toArray(new String[0]));
            }
        }
    }


    /**
     * Writes the collected data rows to a CSV file.
     *
     * @param filePath The path to the output CSV file.
     * @param csvData A list of String arrays, where each array represents a row.
     * @throws IOException If an error occurs during file writing.
     */
    private void writeCsvFile(String filePath, List<String[]> csvData) throws IOException {
         try {
             Path path = Paths.get(filePath);
             Path parentDir = path.getParent();
             if (parentDir != null) {
                 Files.createDirectories(parentDir);
             }
         } catch (IOException e) {
             LOGGER.warn("Failed to create parent directory for CSV file {}: {}", filePath, e.getMessage(), e);
             // Continue attempting to write, maybe the directory already exists or has permissions
         }

        // Use try-with-resources for reliable closing
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
             if (csvData.isEmpty()) {
                 LOGGER.warn("CSV data is empty, writing an empty file to {}", filePath);
                 // Optionally write headers even if empty
             }
            for (String[] line : csvData) {
                writer.write(Arrays.stream(line)
                    .map(this::escapeCsvField)
                    .collect(Collectors.joining(",")));
                writer.newLine();
            }
            notifyObservers("Configuration differences successfully saved to " + filePath);
        } catch (IOException e) {
            LOGGER.error("Error writing CSV file {}: {}", filePath, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Escapes a field for safe inclusion in a CSV row.
     * Handles nulls, commas, double quotes, and newlines.
     *
     * @param field The string field to escape.
     * @return The properly escaped CSV field.
     */
    private String escapeCsvField(String field) {
        final String CSV_QUOTE = "\"";
        final String ESCAPED_QUOTE = "\"\"";
        final String DEFAULT_NULL_CSV = ""; // Represent nulls as empty string in CSV

        if (field == null) {
            return DEFAULT_NULL_CSV;
        }

        if (field.contains(",") || field.contains(CSV_QUOTE) || field.contains("\n") || field.contains("\r")) {
            return CSV_QUOTE + field.replace(CSV_QUOTE, ESCAPED_QUOTE) + CSV_QUOTE;
        }
        return field;
    }

    /**
      * Fetches configuration using the factory with basic retry logic.
      * @param cluster The cluster to fetch from.
      * @param operationType The key for the ConfigOperationFactory (e.g., "variable", "backend").
      * @return The configuration map, or null if fetching failed after retries.
      * @throws SQLException If a non-retryable SQL error occurs.
      * @throws IllegalArgumentException If the operationType is invalid.
      */
     private Map<String, String> fetchConfigWithRetry(DorisCluster cluster, String operationType) throws SQLException {
         int maxRetries = 2;
         long delayMs = 1000;

         for (int attempt = 0; attempt <= maxRetries; attempt++) {
             try {
                 ConfigOperation operation = ConfigOperationFactory.createOperation(operationType);
                 return operation.fetchConfig(cluster);
             } catch (SQLException e) {
                 if (attempt < maxRetries) {
                     LOGGER.warn("Attempt {}: Failed to fetch {} config for {}: {}. Retrying in {}ms...",
                             attempt + 1, operationType, cluster.getName(), e.getMessage(), delayMs * (attempt + 1)); // Log calculated delay
                     try {
                         Thread.sleep(delayMs * (attempt + 1)); // Exponential backoff?
                     } catch (InterruptedException ie) {
                         Thread.currentThread().interrupt();
                         LOGGER.warn("Retry delay interrupted for {}", cluster.getName());
                         throw new SQLException("Config fetch retry interrupted.", e); // Wrap original cause
                     }
                 } else {
                     LOGGER.error("Attempt {}: Failed to fetch {} config for {} after {} retries: {}.",
                              attempt + 1, operationType, cluster.getName(), maxRetries, e.getMessage(), e);
                     throw e;
                 }
             } catch (IllegalArgumentException iae) {
                 LOGGER.error("Invalid operation type provided to factory: {}", operationType, iae);
                 throw iae; // Not retryable
             } catch (Exception ex) {
                  LOGGER.error("Unexpected error fetching {} config for {} on attempt {}: {}.",
                              operationType, cluster.getName(), attempt + 1, ex.getMessage(), ex);
                  if (attempt == maxRetries) {
                       // Wrap in SQLException? Or a custom exception?
                       throw new SQLException("Unexpected error during config fetch: " + ex.getMessage(), ex);
                  }
                  // Optionally retry on unexpected errors too? Risky.
                   try { Thread.sleep(delayMs * (attempt + 1)); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
             }
         }
         // Should not be reached if SQLException is thrown correctly on final attempt
         return null;
     }


    // --- Main Method ---
    public static void main(String[] args) {
        LOGGER.info("Starting Doris Config Manager utility...");

        try {
            // --- Configuration Loading & Validation ---
            // Prioritize environment variables, fallback to ConfigLoader or defaults
            String dbUser = System.getenv("DB_USER");
            String dbPassword = System.getenv("DB_PASSWORD");
            String refClusterHost = System.getenv("REF_CLUSTER_HOST");
            String targetClusterHostsRaw = System.getenv("TARGET_CLUSTER_HOSTS");
            String jdbcPort = System.getenv("DB_PORT") != null ? System.getenv("DB_PORT") : "2000"; // Default domain port
            String outputDir = System.getenv("OUTPUT_DIR") != null ? System.getenv("OUTPUT_DIR") : System.getProperty("user.home") + "/doris_config_reports";

             LOGGER.info("Attempting to load configuration...");
             LOGGER.debug("DB_USER from env: {}", (dbUser != null ? "***" : "null"));
             LOGGER.debug("REF_CLUSTER_HOST from env: {}", refClusterHost);
             LOGGER.debug("TARGET_CLUSTER_HOSTS from env: {}", targetClusterHostsRaw);
             LOGGER.debug("DB_PORT from env: {}", System.getenv("DB_PORT"));
             LOGGER.debug("OUTPUT_DIR from env: {}", System.getenv("OUTPUT_DIR"));


            // Fallback to ConfigLoader if environment variables are missing
            if (dbUser == null || dbPassword == null) {
                LOGGER.info("DB credentials not found in environment variables, attempting fallback via ConfigLoader...");
                if (dbConfig != null) {
                    dbUser = dbConfig.getUsername();
                    dbPassword = dbConfig.getPassword();
                    LOGGER.info("Using DB credentials from ConfigLoader.");
                } else {
                    LOGGER.error("DB credentials not found in environment variables and ConfigLoader failed or returned null. Cannot proceed.");
                    return;
                }
            }

            if (dbUser == null || dbUser.isBlank() || dbPassword == null) {
                 LOGGER.error("Database user is missing or blank. Cannot proceed.");
                 return;
            }

            // Use default hosts if not provided via env vars (provide clear defaults)
             String defaultRefHost = "drts802"; // EXAMPLE:drts802
             String defaultTargetHosts = "drpub821"; // EXAMPLE:"drpub821, drpub1102"
            if (refClusterHost == null || refClusterHost.isBlank()) {
                 LOGGER.warn("REF_CLUSTER_HOST not set, using default: {}", defaultRefHost);
                 refClusterHost = defaultRefHost;
             }
            if (targetClusterHostsRaw == null || targetClusterHostsRaw.isBlank()) {
                 LOGGER.warn("TARGET_CLUSTER_HOSTS not set, using default: {}", defaultTargetHosts);
                 targetClusterHostsRaw = defaultTargetHosts;
             }

            List<String> targetClusterHosts = Arrays.stream(targetClusterHostsRaw.split(","))
                                                 .map(String::trim)
                                                 .filter(s -> !s.isEmpty())
                                                 .distinct() // Avoid duplicate targets
                                                 .collect(Collectors.toList());

            if (targetClusterHosts.isEmpty()) {
                 LOGGER.error("No valid target cluster hosts found after parsing TARGET_CLUSTER_HOSTS. Value was: '{}'", targetClusterHostsRaw);
                 return;
            }

             try { Integer.parseInt(jdbcPort); } catch (NumberFormatException e) {
                  // Use Log4j2 level ERROR
                  LOGGER.error("Invalid JDBC Port specified: '{}'. Must be an integer.", jdbcPort);
                  return;
             }


            // --- Cluster Object Creation ---
             LOGGER.info("Creating cluster objects...");
            DorisCluster referenceCluster = buildCluster(refClusterHost, jdbcPort, dbUser, dbPassword);
            if (referenceCluster == null) return; // Error logged in buildCluster

            String finalDbUser = dbUser;
            String finalDbPassword = dbPassword;
            List<DorisCluster> targetClusters = targetClusterHosts.stream()
                .map(host -> buildCluster(host, jdbcPort, finalDbUser, finalDbPassword))
                .filter(Objects::nonNull) // Filter out clusters that failed to build
                .collect(Collectors.toList());

            if (targetClusters.isEmpty()) {
                 LOGGER.error("No target clusters could be successfully configured. Exiting.");
                 return;
            }

            LOGGER.info("Reference Cluster: {} ({})", referenceCluster.getName(), referenceCluster.getJdbcUrl());
            LOGGER.info("Target Clusters ({}): {}", targetClusters.size(), targetClusters.stream().map(DorisCluster::getName).collect(Collectors.joining(", ")));

            // --- Initialize and Execute ---
            DorisConfigManager manager = new DorisConfigManager(referenceCluster, targetClusters);

            try {
                Path outPath = Paths.get(outputDir);
                Files.createDirectories(outPath);
                LOGGER.info("Output directory set to: {}", outPath.toAbsolutePath());
            } catch (IOException e) {
                LOGGER.error("Failed to create output directory: {} - {}. Check permissions.", outputDir, e.getMessage(), e);
                return;
            }

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String allDiffCsvPath = Paths.get(outputDir, "all_config_diff_" + timestamp + ".csv").toString();

            LOGGER.info("Saving configuration differences to: " + allDiffCsvPath);
            manager.saveDifferencesToCsv(allDiffCsvPath);

            // Optionally, print alignment commands/suggestions to console
            // LOGGER.info("Generating alignment suggestions (output to console)...");
            // manager.printAlignmentSuggestions();

            LOGGER.info("Processing completed successfully.");

        } catch (SQLException e) {
            LOGGER.error("Database error occurred during processing: {}", e.getMessage(), e);
        } catch (IOException e) {
            LOGGER.error("File I/O error occurred during processing: {}", e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.error("An unexpected error occurred: {}", e.getMessage(), e);
        }
    }

     /**
      * Helper method to build a DorisCluster object, handling potential errors.
      */
     private static DorisCluster buildCluster(String host, String port, String user, String password) {
         if (host == null || host.isBlank() || port == null || port.isBlank() || user == null || user.isBlank() || password == null) {
              LOGGER.error("Cannot build cluster object: Missing required parameter (host, port, user, or password was null/blank). Host: {}", host);
              return null;
         }
         try {
              String clusterName = host.split("\\.")[0]; // Basic name extraction
              String jdbcUrl = "jdbc:mysql://" + host + ".olap.jd.com:" + port; // Construct JDBC URL
              return new DorisCluster.Builder()
                  .name(clusterName)
                  .jdbcUrl(jdbcUrl)
                  .user(user)
                  .password(password)
                  .build();
         } catch (Exception e) {
              LOGGER.error("Failed to build DorisCluster object for host: {} - {}", host, e.getMessage(), e);
              return null;
         }
     }

}