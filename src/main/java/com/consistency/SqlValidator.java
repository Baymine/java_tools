package com.consistency;

import com.rsa.conf.DatabaseConfig;
import com.rsa.conf.ServerPort;
import com.rsa.conf.ServerPortConfig;
import com.rsa.utils.AESUtil;
import config.ConfigLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SQL验证器类，负责从CSV文件读取SQL并在Doris集群上执行验证
 */
public class SqlValidator {
    private static final Logger LOGGER = Logger.getLogger(SqlValidator.class.getName());
    private static final ConfigLoader configLoader = ConfigLoader.getInstance();
    private static final String SOURCE_NAME = "easyolap";
    private static final DatabaseConfig dbConfig = configLoader.getLakehouseDBConfig(SOURCE_NAME);
    private static final String DB_URL_TEMPLATE = "jdbc:mysql://%s:%s/%s?useSSL=true&socketKeepAlive=true";

    private final String csvFilePath;
    private final int threadCount;
    private final String dorisHost;
    private final String port;
    private final Properties connectionProperties;
    private final AtomicInteger processedCount = new AtomicInteger(0);
    private final AtomicInteger mismatchCount = new AtomicInteger(0);
    private final boolean useDatabase;
    
    public SqlValidator(String csvFilePath, int threadCount, String dorisHost, boolean useDatabase) {
        this.csvFilePath = csvFilePath;
        this.threadCount = threadCount;
        this.dorisHost = dorisHost;
        this.useDatabase = useDatabase;
        
        // 获取正确的端口号
        this.port = ServerPortConfig.getPort(dorisHost);
        
        // 设置连接属性
        this.connectionProperties = new Properties();
        String userName = dbConfig.getUsername();
        String password = dbConfig.getPassword();
        
        // 根据是否是本地连接设置用户名格式
        connectionProperties.put("user", userName + "$" + SOURCE_NAME);
        connectionProperties.put("password", password);
    }
    
    /**
     * 执行SQL验证
     */
    public void validateSql() {
        LOGGER.info("Starting SQL validation with " + threadCount + " threads");
        LOGGER.info("Connecting to Doris cluster at: " + dorisHost + ":" + port);
        
        List<SqlRecord> records;
        try {
            if (useDatabase) {
                LOGGER.info("Fetching SQL queries from database");
                records = fetchSqlRecordsFromDatabase();
            } else {
                LOGGER.info("Reading SQL queries from: " + csvFilePath);
                records = readSqlRecordsFromCsv(csvFilePath);
            }
            LOGGER.info("Found " + records.size() + " SQL queries to validate");
            
            // 创建输出目录
            String outputDir = "sql_validation_results";
            Path outputPath = Paths.get(outputDir);
            if (!Files.exists(outputPath)) {
                Files.createDirectory(outputPath);
            }
            
            // 创建结果文件
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String mismatchFilePath = outputDir + "/mismatched_sql_" + timestamp + ".sql";
            
            // 批量处理SQL验证，控制并发数量
            int batchSize = 100; // 每批处理的SQL数量
            int totalRecords = records.size();
            
            for (int i = 0; i < totalRecords; i += batchSize) {
                int endIndex = Math.min(i + batchSize, totalRecords);
                List<SqlRecord> batch = records.subList(i, endIndex);
                
                LOGGER.info("Processing batch " + (i / batchSize + 1) + " of " + 
                           (int) Math.ceil((double) totalRecords / batchSize) + 
                           " (records " + i + " to " + (endIndex - 1) + ")");
                
                // 为每批创建Future列表
                List<CompletableFuture<Void>> futures = new ArrayList<>();
                
                // 并发处理当前批次
                for (SqlRecord record : batch) {
                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        try {
                            ValidationResult result = validateSqlRecord(record);
                            
                            int current = processedCount.incrementAndGet();
                            if (current % 100 == 0) {
                                LOGGER.info("Processed " + current + " out of " + records.size() + " SQL queries");
                            }
                            
                            if (!result.isMatched()) {
                                mismatchCount.incrementAndGet();
                                synchronized (this) {
                                    appendToFile(mismatchFilePath, formatMismatchedSql(record, result));
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Error validating SQL: " + record.getPrestoSql(), e);
                            synchronized (this) {
                                try {
                                    appendToFile(mismatchFilePath, formatErrorSql(record, e));
                                } catch (IOException ioEx) {
                                    LOGGER.log(Level.SEVERE, "Error writing to mismatch file", ioEx);
                                }
                            }
                        }
                    });
                    
                    futures.add(future);
                }
                
                // 等待当前批次完成
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                
                // 每批完成后打印进度
                LOGGER.info("Batch " + (i / batchSize + 1) + " completed. " +
                           "Processed " + processedCount.get() + " out of " + totalRecords + " SQL queries.");
            }
            
            LOGGER.info("SQL validation completed");
            LOGGER.info("Total SQL queries processed: " + processedCount.get());
            LOGGER.info("Mismatched SQL queries: " + mismatchCount.get());
            LOGGER.info("Mismatched SQL queries saved to: " + mismatchFilePath);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during SQL validation", e);
        }
    }
    
    /**
     * 验证单条SQL记录
     */
    private ValidationResult validateSqlRecord(SqlRecord record) throws Exception {
        String sql = record.getPrestoSql();
        int expectedCount = record.getPrestoCountResult();
        
        // 构建JDBC URL
        String cipherText = AESUtil.getCipherText();
        String jdbcUrl = String.format(DB_URL_TEMPLATE, dorisHost, port, cipherText);
        
        try (Connection conn = DriverManager.getConnection(jdbcUrl, connectionProperties);
             Statement stmt = conn.createStatement()) {
            
            // 设置查询超时（秒）
            stmt.setQueryTimeout(300);
            
            try (ResultSet rs = stmt.executeQuery(sql)) {
                int actualCount = 0;
                while (rs.next()) {
                    actualCount++;
                }
                
                return new ValidationResult(expectedCount, actualCount);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "执行SQL时发生错误: " + sql, e);
            return new ValidationResult(expectedCount, -1); // 使用-1表示执行失败
        }
    }
    
    /**
     * 从CSV文件读取SQL记录
     * 处理多行SQL查询的情况
     */
    private List<SqlRecord> readSqlRecordsFromCsv(String filePath) throws IOException {
        List<SqlRecord> records = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            // 读取并跳过头行，处理可能存在的BOM标记
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IOException("CSV file is empty");
            }
            
            // 处理BOM标记
            if (headerLine.startsWith("\uFEFF")) {
                headerLine = headerLine.substring(1);
            }
            
            LOGGER.info("处理CSV文件，使用自定义解析器");
            
            // 使用自定义CSV解析器处理多行SQL
            String line;
            StringBuilder currentRecord = new StringBuilder();
            String currentId = null;
            String currentPrestoCount = null;
            String currentDorisCount = null;
            boolean inQuotedField = false;
            int lineNumber = 1;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                
                // 如果当前行以UUID格式开头，说明这是一条新记录的开始
                if (line.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}.*")) {
                    // 处理之前积累的记录（如果有）
                    if (currentId != null) {
                        try {
                            processRecord(records, currentId, currentPrestoCount, currentDorisCount, currentRecord.toString());
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "处理记录时出错: " + e.getMessage(), e);
                        }
                    }
                    
                    // 开始新记录
                    currentRecord = new StringBuilder();
                    String[] parts = line.split("\t", 4); // 只分割前3个字段
                    if (parts.length >= 4) {
                        currentId = parts[0];
                        currentPrestoCount = parts[1];
                        currentDorisCount = parts[2];
                        currentRecord.append(parts[3]);
                        
                        // 检查是否进入引号字段
                        inQuotedField = countQuotes(parts[3]) % 2 != 0;
                    } else {
                        LOGGER.warning("行格式不正确: " + line);
                    }
                } else {
                    // 继续积累当前记录
                    if (currentRecord.length() > 0) {
                        currentRecord.append("\n").append(line);
                        
                        // 更新引号状态
                        if (line.contains("\"")) {
                            inQuotedField = !inQuotedField;
                        }
                    }
                }
            }
            
            // 处理最后一条记录
            if (currentId != null) {
                try {
                    processRecord(records, currentId, currentPrestoCount, currentDorisCount, currentRecord.toString());
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "处理最后一条记录时出错: " + e.getMessage(), e);
                }
            }
            
            LOGGER.info("成功从CSV文件读取了 " + records.size() + " 条记录");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "处理CSV文件时出错", e);
            throw new IOException("处理CSV文件时出错", e);
        }
        
        return records;
    }
    
    /**
     * 处理单条记录
     */
    private void processRecord(List<SqlRecord> records, String id, String prestoCountStr, String dorisCountStr, String sqlPart) {
        try {
            // 解析SQL部分
            int tabIndex = sqlPart.lastIndexOf("\t");
            if (tabIndex == -1) {
                LOGGER.warning("SQL部分格式不正确，缺少分隔符: " + sqlPart);
                return;
            }
            
            String prestoSql = sqlPart.substring(0, tabIndex);
            String dorisSql = sqlPart.substring(tabIndex + 1);
            
            // 去除引号
            if (prestoSql.startsWith("\"") && prestoSql.endsWith("\"")) {
                prestoSql = prestoSql.substring(1, prestoSql.length() - 1);
            }
            if (dorisSql.startsWith("\"") && dorisSql.endsWith("\"")) {
                dorisSql = dorisSql.substring(1, dorisSql.length() - 1);
            }
            
            int prestoCount = Integer.parseInt(prestoCountStr);
            int dorisCount = Integer.parseInt(dorisCountStr);
            
            SqlRecord record = new SqlRecord(id, prestoCount, dorisCount, prestoSql, dorisSql);
            records.add(record);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "处理记录时出错: " + e.getMessage(), e);
        }
    }
    
    /**
     * 计算字符串中引号的数量
     */
    private int countQuotes(String str) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == '"') {
                count++;
            }
        }
        return count;
    }
    
    /**
     * 将不匹配的SQL追加到文件
     */
    private void appendToFile(String filePath, String content) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            writer.write(content);
            writer.newLine();
            writer.newLine();
        }
    }
    
    /**
     * 格式化不匹配的SQL记录
     */
    private String formatMismatchedSql(SqlRecord record, ValidationResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("-- Relation: ").append(record.getPrestoDorisRel()).append("\n");
        sb.append("-- Expected count: ").append(record.getPrestoCountResult()).append("\n");
        sb.append("-- Actual count: ").append(result.getActualCount()).append("\n");
        sb.append(record.getPrestoSql()).append(";");
        return sb.toString();
    }
    
    /**
     * 格式化执行出错的SQL记录
     */
    private String formatErrorSql(SqlRecord record, Exception error) {
        StringBuilder sb = new StringBuilder();
        sb.append("-- Relation: ").append(record.getPrestoDorisRel()).append("\n");
        sb.append("-- Expected count: ").append(record.getPrestoCountResult()).append("\n");
        sb.append("-- Error: ").append(error.getMessage()).append("\n");
        sb.append(record.getPrestoSql()).append(";");
        return sb.toString();
    }

    /**
     * 从数据库中获取SQL记录
     */
    private List<SqlRecord> fetchSqlRecordsFromDatabase() throws Exception {
        List<SqlRecord> records = new ArrayList<>();
        String query = "SELECT b.sql_content, a.presto_cnt_result[1] as presto_cnt_result " +
                       "FROM dmc_dev.dmc_dev_bi_sql_execute_agg_by_pd_log_i_d a " +
                       "LEFT JOIN dmc_dev.dmc_dev_bi_sql_execute_full_log_i_d b " +
                       "ON a.presto_doris_rel = b.presto_doris_rel " +
                       "WHERE a.doris_exe_cnt = 1 " +
                       "AND a.presto_exe_cnt = 1 " +
                       "AND a.presto_exe_result[1] = 0 " +
                       "AND a.doris_exe_result[1] = 0 " +
                       "AND a.presto_cache_result[1] = 0 " +
                       "AND a.doris_cache_result[1] = 0 " +
                       "AND cardinality(a.doris_cnt_result) = 1 " +
                       "AND cardinality(a.presto_cnt_result) = 1 " +
                       "AND a.presto_cnt_result[1] != a.doris_cnt_result[1]";

        String jdbcUrl = String.format(DB_URL_TEMPLATE, dorisHost, port, AESUtil.getCipherText());
        try (Connection conn = DriverManager.getConnection(jdbcUrl, connectionProperties);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                String sqlContent = rs.getString("sql_content");
                int prestoCntResult = rs.getInt("presto_cnt_result");
                records.add(new SqlRecord(null, prestoCntResult, 0, sqlContent, null));
            }
        }
        
        return records;
    }
}
