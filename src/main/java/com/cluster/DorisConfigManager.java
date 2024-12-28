package com.cluster;

import com.rsa.conf.DatabaseConfig;
import config.ConfigLoader;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.*;

public class DorisConfigManager {
    static private String refClusterName = "";
    static private String targetClusterName = "";

    private static final ConfigLoader configLoader = ConfigLoader.getInstance();
    public static final DatabaseConfig dbConfig = configLoader.getLakehouseDBConfig("local");

    static class DorisCluster {
        String name;
        String jdbcUrl;
        String user;
        String password;


        public DorisCluster(String name, String jdbcUrl, String user, String password) {
            this.name = name;
            this.jdbcUrl = jdbcUrl;
            this.user = user;
            this.password = password;
        }
    }

    static class ConfigFetcher {
        private DorisCluster cluster;


        public ConfigFetcher(DorisCluster cluster) {
            this.cluster = cluster;
        }

        public Map<String, String> fetchShowVariables() {
            Map<String, String> variables = new HashMap<>();
            String query = "SHOW VARIABLES";

            try (Connection conn = DriverManager.getConnection(cluster.jdbcUrl, cluster.user, cluster.password);
                 Statement statement = conn.createStatement();
                 ResultSet resultSet = statement.executeQuery(query)
            ){
                while (resultSet.next()) {
                    String variableName = resultSet.getString("Variable_name");
                    String value = resultSet.getString("Value");
                    variables.put(variableName, value);
                }
                return variables;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        public Map<String, String> fetchFrontendConfig() throws SQLException {
            Map<String, String> frontendConfig = new HashMap<>();
            String query = "ADMIN SHOW FRONTEND CONFIG";

            try (Connection conn = DriverManager.getConnection(
                    cluster.jdbcUrl, cluster.user, cluster.password);
                 Statement stmt = conn.createStatement()) {

                boolean hasResultSet = stmt.execute(query);
                if (hasResultSet) {
                    try (ResultSet rs = stmt.getResultSet()) {
                        // Adjust column names based on actual output
                        // Common column names might be "Key" and "Value" or similar
                        // You should verify the actual column names returned by Doris
                        while (rs.next()) {
                            String key = rs.getString("Key");
                            String value = rs.getString("Value");
                            if (key != null && value != null) {
                                frontendConfig.put(key, value);
                            }
                        }
                    }
                } else {
                    // Handle cases where the command doesn't return a ResultSet
                    System.err.println("ADMIN SHOW FRONTEND CONFIG did not return a ResultSet.");
                }
            }

            return frontendConfig;
        }

        static class ConfigComparer {
            private static void compareConfigs(Map<String, String> reference, Map<String, String> target) {
                HashSet<String> allKeys = new HashSet<>();
                allKeys.addAll(reference.keySet());
                allKeys.addAll(target.keySet());

                System.out.println("Differences: ");
                for(String key : allKeys) {
                    String refVal = reference.get(key);
                    String targetVal = target.get(key);

                    if(!Objects.equals(refVal, targetVal)) {
                        System.out.printf("Variable: %s%n %s: %s%n  %s: %s%n%n", key, refClusterName, refVal, targetClusterName,targetVal);
                    }
                }
            }
        }

        // Aligns target cluster's configurations to match the reference cluster
        // TODO: before perform the alignment, save the config into a file
        static class ConfigAligner {
            private DorisCluster targetCluster;

            ConfigAligner(DorisCluster targetCluster) {
                this.targetCluster = targetCluster;
            }

            public void alignConfigs(Map<String, String> referenceConfigs) throws SQLException {
                try (Connection conn = DriverManager.getConnection(
                        targetCluster.jdbcUrl, targetCluster.user, targetCluster.password);
                     Statement stmt = conn.createStatement()) {

                    for (Map.Entry<String, String> entry : referenceConfigs.entrySet()) {
                        String variable = entry.getKey();
                        String value = entry.getValue();
                        String setQuery = String.format("SET GLOBAL %s = '%s';", variable, value);

                        try {
                            stmt.execute(setQuery);
                            System.out.printf("Set %s to %s on cluster %s%n",
                                    variable, value, targetCluster.name);
                        } catch (SQLException e) {
                            System.err.printf("Failed to set %s on cluster %s: %s%n",
                                    variable, targetCluster.name, e.getMessage());
                        }
                    }
                }
            }

            // TODO: before perform the alignment, save the config into a file
            public void alignFrontendConfigs(Map<String, String> referenceFrontendConfigs) throws SQLException {
                // Assuming there's a way to set frontend configs via SQL commands
                // This might need to be adjusted based on Doris's actual capabilities
                try (Connection conn = DriverManager.getConnection(
                        targetCluster.jdbcUrl, targetCluster.user, targetCluster.password);
                     Statement stmt = conn.createStatement()) {

                    for (Map.Entry<String, String> entry : referenceFrontendConfigs.entrySet()) {
                        String key = entry.getKey();
                        String value = entry.getValue();
                        String setQuery = String.format("SET FRONTEND CONFIG %s = '%s';", key, value);

                        try {
                            stmt.execute(setQuery);
                            System.out.printf("Set frontend config %s to %s on cluster %s%n",
                                    key, value, targetCluster.name);
                        } catch (SQLException e) {
                            System.err.printf("Failed to set frontend config %s on cluster %s: %s%n",
                                    key, targetCluster.name, e.getMessage());
                        }
                    }
                }
            }

            public void rollbackConfig(DorisCluster cluster) {
                // TODO: rollback the config
                return ;
            }
        }
    }

    public static void saveBatchDiff(DorisCluster refCluster, List<DorisCluster> targetClusters, String filePath) throws SQLException {
        // Fetch configurations from the reference cluster
        ConfigFetcher refFetcher = new ConfigFetcher(refCluster);
        Map<String, String> refVariables = refFetcher.fetchShowVariables();
        Map<String, String> refFrontendConfigs = refFetcher.fetchFrontendConfig();

        // Merge reference configurations
        Map<String, String> refConfigs = new HashMap<>();
        refConfigs.putAll(refVariables);
        refConfigs.putAll(refFrontendConfigs);

        // Initialize CSV data with headers: Config Key, Config Type, Reference Cluster, Target1, Target2, ...
        List<String> headers = new ArrayList<>();
        headers.add("Config Key");
        headers.add("Config Type"); // Added config type column
        headers.add(refCluster.name);
        for (DorisCluster target : targetClusters) {
            headers.add(target.name);
        }

        List<String[]> csvData = new ArrayList<>();
        csvData.add(headers.toArray(new String[0]));

        // Gather all configuration keys from reference and target clusters
        Set<String> allConfigKeys = new HashSet<>(refConfigs.keySet());
        Map<DorisCluster, Map<String, String>> targetConfigsMap = new HashMap<>();

        for (DorisCluster target : targetClusters) {
            ConfigFetcher targetFetcher = new ConfigFetcher(target);
            Map<String, String> targetVariables = targetFetcher.fetchShowVariables();
            Map<String, String> targetFrontendConfigs = targetFetcher.fetchFrontendConfig();
            Map<String, String> targetConfigs = new HashMap<>();
            targetConfigs.putAll(targetVariables);
            targetConfigs.putAll(targetFrontendConfigs);
            targetConfigsMap.put(target, targetConfigs);
            allConfigKeys.addAll(targetConfigs.keySet());
        }

        // Populate CSV data with configuration comparisons
        for (String configKey : allConfigKeys) {
            String refValue = refConfigs.getOrDefault(configKey, "N/A");
            boolean hasDifference = false;
            
            // Check if there are any differences with target clusters
            for (DorisCluster target : targetClusters) {
                Map<String, String> targetConfigs = targetConfigsMap.get(target);
                String targetValue = targetConfigs.getOrDefault(configKey, "N/A");
                if (!refValue.equals(targetValue)) {
                    hasDifference = true;
                    break;
                }
            }

            // Only add to CSV if there are differences
            if (hasDifference) {
                List<String> row = new ArrayList<>();
                row.add(configKey);
                // Determine config type
                String configType = refVariables.containsKey(configKey) ? "Variable" : "Frontend";
                row.add(configType);
                row.add(refValue);

                for (DorisCluster target : targetClusters) {
                    Map<String, String> targetConfigs = targetConfigsMap.get(target);
                    String targetValue = targetConfigs.getOrDefault(configKey, "N/A");
                    row.add(targetValue);
                }

                csvData.add(row.toArray(new String[0]));
            }
        }

        // Write the CSV data to the specified file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (String[] line : csvData) {
                // Escape double quotes and encapsulate fields containing special characters
                StringBuilder csvLineBuilder = new StringBuilder();
                for (int i = 0; i < line.length; i++) {
                    String field = line[i];
                    if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
                        field = field.replace("\"", "\"\"");
                        field = "\"" + field + "\"";
                    }
                    csvLineBuilder.append(field);
                    if (i < line.length - 1) {
                        csvLineBuilder.append(",");
                    }
                }
                String csvLine = csvLineBuilder.toString();
                writer.write(csvLine);
                writer.newLine();
            }
            System.out.println("Configuration differences saved to " + filePath);
        } catch (IOException e) {
            System.err.println("Failed to write CSV file: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws Exception{
        refClusterName = "drpub1820";
//        List<String> targetClusterNameList =  Arrays.asList("drpub806", "drpub807", "drpub809", "drpub820", "drpub1001","drpub900", "drpub901", "drpub902","drpub920","drpub906", "drpub907","drpub950", "drpub1050", "drpub1150","drpub915", "drpub916", "drpub917","drpub952", "drpub1052", "drpub1152");
        List<String> targetClusterNameList =  Arrays.asList("drpub1101");

        Boolean needAlignment = false;
        Boolean needRollback = false;

        // Define your clusters
        DorisCluster referenceCluster = new DorisCluster(
                refClusterName,
                "jdbc:mysql://" + refClusterName + ".olap.jd.com:2000",
                dbConfig.getUsername(),
                dbConfig.getPassword()
        );
        List<DorisCluster> targetClusters = new ArrayList<>();

        for (String targetClusterName : targetClusterNameList){
            targetClusters.add(new DorisCluster(targetClusterName, "jdbc:mysql://" + targetClusterName +".olap.jd.com:2000", "root", dbConfig.getPassword()));
        }

        System.out.println("Starting saving the diff");
        saveBatchDiff(referenceCluster, targetClusters, "~/Testing/diff.csv");
        System.out.println("Finished saving the diff");

        System.out.println("reference: " + referenceCluster.name + "//target: " + targetClusters.get(0).name);
        // Fetch configurations from reference cluster
        ConfigFetcher referenceFetcher = new ConfigFetcher(referenceCluster);
        Map<String, String> referenceVariables = new HashMap<>();
        Map<String, String> referenceFrontendConfigs = new HashMap<>();

        System.out.println("Fetching configurations from reference cluster...");
        referenceVariables = referenceFetcher.fetchShowVariables();
        referenceFrontendConfigs = referenceFetcher.fetchFrontendConfig();

        // Iterate over target clusters
        for (DorisCluster target : targetClusters) {
            System.out.printf("%nProcessing target cluster: %s%n", target.name);
            ConfigFetcher targetFetcher = new ConfigFetcher(target);
            Map<String, String> targetVariables = new HashMap<>();
            Map<String, String> targetFrontendConfigs = new HashMap<>();

            // Fetch target configurations
            targetVariables = targetFetcher.fetchShowVariables();
            targetFrontendConfigs = targetFetcher.fetchFrontendConfig();

            // Compare configurations
            System.out.println("Comparing SHOW VARIABLES...");
            ConfigFetcher.ConfigComparer.compareConfigs(referenceVariables, targetVariables);

            System.out.println("Comparing FRONTEND CONFIG...");
            ConfigFetcher.ConfigComparer.compareConfigs(referenceFrontendConfigs, targetFrontendConfigs);

            if (needAlignment){
                // Align configurations
                ConfigFetcher.ConfigAligner aligner = new ConfigFetcher.ConfigAligner(target);
                System.out.println("Aligning SHOW VARIABLES...");
                aligner.alignConfigs(referenceVariables);

                System.out.println("Aligning FRONTEND CONFIG...");
                aligner.alignFrontendConfigs(referenceFrontendConfigs);
            }

        }

        System.out.println("\nConfiguration comparison and alignment complete.");
    }
}
