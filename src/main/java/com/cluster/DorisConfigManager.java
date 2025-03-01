package com.cluster;

import com.rsa.conf.DatabaseConfig;
import config.ConfigLoader;
import lombok.Getter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

public class DorisConfigManager {
    private static final Logger LOGGER = Logger.getLogger(DorisConfigManager.class.getName());
    private static final ConfigLoader configLoader = ConfigLoader.getInstance();
    public static final DatabaseConfig dbConfig = configLoader.getLakehouseDBConfig("local");

    // Builder Pattern for DorisCluster
    @Getter
    public static class DorisCluster {
        private final String name;
        private final String jdbcUrl;
        private final String user;
        private final String password;

        private DorisCluster(Builder builder) {
            this.name = builder.name;
            this.jdbcUrl = builder.jdbcUrl;
            this.user = builder.user;
            this.password = builder.password;
        }

        public static class Builder {
            private String name;
            private String jdbcUrl;
            private String user;
            private String password;

            public Builder name(String name) {
                this.name = name;
                return this;
            }

            public Builder jdbcUrl(String jdbcUrl) {
                this.jdbcUrl = jdbcUrl;
                return this;
            }

            public Builder user(String user) {
                this.user = user;
                return this;
            }

            public Builder password(String password) {
                this.password = password;
                return this;
            }

            public DorisCluster build() {
                return new DorisCluster(this);
            }
        }

    }

    // Interface segregation principle
    interface ConfigOperation {
        Map<String, String> fetchConfig(DorisCluster cluster) throws SQLException;
    }

    interface ConfigAlignment {
        void align(DorisCluster cluster, Map<String, String> referenceConfig) throws SQLException;
    }

    // Strategy Pattern for different config operations
    public static class VariableConfigOperation implements ConfigOperation {
        @Override
        public Map<String, String> fetchConfig(DorisCluster cluster) throws SQLException {
            return getStringStringMap(cluster.getJdbcUrl(), cluster.getUser(), cluster.getPassword(), cluster);
        }

        public static Map<String, String> getStringStringMap(String jdbcUrl, String user, String password, DorisCluster cluster) throws SQLException {
            Map<String, String> variables = new HashMap<>();
            String query = "SHOW VARIABLES";

            try (Connection conn = DriverManager.getConnection(
                    jdbcUrl, user, password);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    variables.put(rs.getString("Variable_name"), rs.getString("Value"));
                }
            }
            return variables;
        }
    }

    static class FrontendConfigOperation implements ConfigOperation {
        @Override
        public Map<String, String> fetchConfig(DorisCluster cluster) throws SQLException {
            Map<String, String> config = new HashMap<>();
            String query = "ADMIN SHOW FRONTEND CONFIG";

            try (Connection conn = DriverManager.getConnection(
                    cluster.getJdbcUrl(), cluster.getUser(), cluster.getPassword());
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    config.put(rs.getString("Key"), rs.getString("Value"));
                }
            }
            return config;
        }
    }

    // Command Pattern for configuration actions
    interface ConfigCommand {
        void execute() throws SQLException;
    }

    static class AlignConfigCommand implements ConfigCommand {
        private final DorisCluster targetCluster;
        private final Map<String, String> referenceConfig;
        private final ConfigAlignment alignment;

        AlignConfigCommand(DorisCluster targetCluster, Map<String, String> referenceConfig, ConfigAlignment alignment) {
            this.targetCluster = targetCluster;
            this.referenceConfig = referenceConfig;
            this.alignment = alignment;
        }

        @Override
        public void execute() throws SQLException {
            alignment.align(targetCluster, referenceConfig);
        }
    }

    // Observer Pattern for logging
    interface ConfigurationObserver {
        void onConfigurationChange(String message);
    }

    static class ConsoleLogger implements ConfigurationObserver {
        @Override
        public void onConfigurationChange(String message) {
            LOGGER.info(message);
        }
    }

    // Factory Pattern for config operations
    static class ConfigOperationFactory {
        public static ConfigOperation createOperation(String type) {
            return switch (type.toLowerCase()) {
                case "variable" -> new VariableConfigOperation();
                case "frontend" -> new FrontendConfigOperation();
                default -> throw new IllegalArgumentException("Unknown config operation type: " + type);
            };
        }
    }

    // Main configuration manager implementation
    private final List<ConfigurationObserver> observers = new ArrayList<>();
    private final DorisCluster referenceCluster;
    private final List<DorisCluster> targetClusters;

    public DorisConfigManager(DorisCluster referenceCluster, List<DorisCluster> targetClusters) {
        this.referenceCluster = referenceCluster;
        this.targetClusters = new ArrayList<>(targetClusters);
        this.observers.add(new ConsoleLogger());
    }

    public void addObserver(ConfigurationObserver observer) {
        observers.add(observer);
    }

    private void notifyObservers(String message) {
        observers.forEach(observer -> observer.onConfigurationChange(message));
    }

    public void compareAndAlignConfigurations() throws SQLException {
        Map<String, String> referenceVariables = ConfigOperationFactory.createOperation("variable")
                .fetchConfig(referenceCluster);
        Map<String, String> referenceFrontendConfigs = ConfigOperationFactory.createOperation("frontend")
                .fetchConfig(referenceCluster);

        for (DorisCluster targetCluster : targetClusters) {
            notifyObservers("Processing cluster: " + targetCluster.getName());
            
            // Compare and align variables
            new AlignConfigCommand(targetCluster, referenceVariables, 
                (cluster, config) -> printAlignmentCommands(cluster, config, "GLOBAL"))
                .execute();

            // Compare and align frontend configs
            new AlignConfigCommand(targetCluster, referenceFrontendConfigs,
                (cluster, config) -> printAlignmentCommands(cluster, config, "FRONTEND CONFIG"))
                .execute();
        }
    }

    private void printAlignmentCommands(DorisCluster cluster, Map<String, String> referenceConfig, String configType) 
            throws SQLException {
        Map<String, String> currentConfig = configType.equals("GLOBAL") ?
                ConfigOperationFactory.createOperation("variable").fetchConfig(cluster) :
                ConfigOperationFactory.createOperation("frontend").fetchConfig(cluster);

        System.out.println("\n=== SET " + configType + " Commands for cluster " + cluster.getName() + " ===");
        
        // Print set commands
        referenceConfig.forEach((key, value) -> {
            String currentValue = currentConfig.get(key);
            if (!Objects.equals(value, currentValue)) {
                String setQuery = String.format("SET %s %s = '%s';", configType, key, value);
                System.out.println(setQuery);
            }
        });

        // Print rollback commands
        System.out.println("\n=== Rollback Commands for " + configType + " ===");
        currentConfig.forEach((key, value) -> {
            if (referenceConfig.containsKey(key)) {
                String rollbackQuery = String.format("SET %s %s = '%s';", configType, key, value);
                System.out.println(rollbackQuery);
            }
        });
    }

    public void saveDifferencesToCsv(String filePath) throws SQLException, IOException {
        Map<String, String> referenceVariables = ConfigOperationFactory.createOperation("variable")
                .fetchConfig(referenceCluster);
        Map<String, String> referenceFrontendConfigs = ConfigOperationFactory.createOperation("frontend")
                .fetchConfig(referenceCluster);

        List<String[]> csvData = new ArrayList<>();
        
        // Add headers
        List<String> headers = new ArrayList<>();
        headers.add("Config Key");
        headers.add("Config Type");
        headers.add(referenceCluster.getName());
        targetClusters.forEach(cluster -> headers.add(cluster.getName()));
        csvData.add(headers.toArray(new String[0]));

        // Process variables and frontend configs
        processConfigDifferences(referenceVariables, "Variable", csvData);
        processConfigDifferences(referenceFrontendConfigs, "Frontend", csvData);

        // Write to CSV
        writeCsvFile(filePath, csvData);
    }

    private void processConfigDifferences(Map<String, String> referenceConfig, String configType, List<String[]> csvData) 
            throws SQLException {
        for (Map.Entry<String, String> entry : referenceConfig.entrySet()) {
            String key = entry.getKey();
            String refValue = entry.getValue();
            
            List<String> row = new ArrayList<>();
            row.add(key);
            row.add(configType);
            row.add(refValue);

            boolean hasDifference = false;
            for (DorisCluster targetCluster : targetClusters) {
                Map<String, String> targetConfig = ConfigOperationFactory
                    .createOperation(configType.toLowerCase())
                    .fetchConfig(targetCluster);
                String targetValue = targetConfig.getOrDefault(key, "N/A");
                row.add(targetValue);
                
                if (!Objects.equals(refValue, targetValue)) {
                    hasDifference = true;
                }
            }

            if (hasDifference) {
                csvData.add(row.toArray(new String[0]));
            }
        }
    }

    private void writeCsvFile(String filePath, List<String[]> csvData) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (String[] line : csvData) {
                writer.write(String.join(",", Arrays.stream(line)
                    .map(this::escapeCsvField)
                    .toArray(String[]::new)));
                writer.newLine();
            }
            notifyObservers("Configuration differences saved to " + filePath);
        }
    }

    private String escapeCsvField(String field) {
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    public static void main(String[] args) {
        try {
            String refClusterName = "drpub808";
            List<String> targetClusterNames = Arrays.asList(
                "drpub949"
            );
//            List<String> targetClusterNames = Arrays.asList(
//                    "drpub807","drpub808","drpub810","drpub805","drpub806","drpub820","drpub952","drpub1052","drpub1152"
//                    ,"drpub1101","drpub1102","drpub1103","drpub1001","drpub950","drpub1050","drpub1150","drpub900","drpub901"
//                    ,"drpub902","drpub903","drpub906","drpub907","drpub940","drpub941","drpub942","drpub943","drpub944","drpub945"
//            );

            // Create reference cluster using Builder pattern
            DorisCluster referenceCluster = new DorisCluster.Builder()
                .name(refClusterName)
                .jdbcUrl("jdbc:mysql://" + refClusterName + ".olap.jd.com:2000")
                .user(dbConfig.getUsername())
                .password(dbConfig.getPassword())
                .build();

            // Create target clusters
            List<DorisCluster> targetClusters = targetClusterNames.stream()
                .map(name -> new DorisCluster.Builder()
                    .name(name)
                    .jdbcUrl("jdbc:mysql://" + name + ".olap.jd.com:2000")
                    .user("root")
                    .password(dbConfig.getPassword())
                    .build())
                .toList();

            // Initialize config manager
            DorisConfigManager manager = new DorisConfigManager(referenceCluster, targetClusters);

            // Save differences to CSV
            manager.saveDifferencesToCsv("/data3/caokaihua1/Testing/diff_test.csv");

            // Compare and align configurations
//            manager.compareAndAlignConfigurations();

        } catch (Exception e) {
            LOGGER.severe("Error in main: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
