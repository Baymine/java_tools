package com.rsa;

import com.rsa.conf.DatabaseConfig;
import com.rsa.conf.ServerPortConfig;
import com.rsa.utils.AESUtil;
import config.ConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;

import static org.jd.stream.executor.SqlExecutor.*;

public class JdbcDemo {

    private static final Logger logger = LoggerFactory.getLogger(JdbcDemo.class);

    // JDBC driver name and database URL template
    private static final String DB_URL_TEMPLATE = "jdbc:mysql://%s:%s/%s?useSSL=true&socketKeepAlive=true";

    private static final String SQL_PATH = "src/main/java/com/rsa/run.sql";

    private static final String LOCALHOST = "127.0.0.1";

    private static final ConfigLoader configLoader = ConfigLoader.getInstance();

    public static DatabaseConfig dbConfig = null;

    // Database credentials
    public static String sourceName = "";

    /**
     * 主函数，用于连接到指定的数据库并执行SQL语句。
     * @param args 命令行参数列表。
     * @throws Exception 如果连接数据库或执行SQL语句时发生错误。
     */
    public static void main(String[] args) {
        try {
            logger.debug("Starting JdbcDemo application");

            // 设置JLine的日志级别为DEBUG
            System.setProperty("org.jline.terminal.dumb", "true");
            System.setProperty("org.jline.utils.Log.level", "DEBUG");
            logger.debug("JLine properties set");

            sourceName = "easyolap";  // modify this to load the db configuration information
            logger.debug("Source name set to: {}", sourceName);

            dbConfig = configLoader.getLakehouseDBConfig(sourceName);
            logger.debug("Database configuration loaded");

            // 809: 11.62.28.0
            // 950:11.116.152.155
            // 807: 11.102.220.241
            List<String> feIpList = List.of("drpub1103.olap.jd.com","11.102.221.97", "11.154.208.50");
            int iterateTime = 1;
            int feIndex = 0;
            boolean isInteractive = false;
            boolean errorOnlyMode = false;  // in Interactive mode, if this is set to true, the output of sql will be ignored.

            String cipherText = "local".equals(sourceName) ? "" : AESUtil.getCipherText();
//            cipherText = "4gNni3cdz7m7cNq6JdsqaYZHb735GC7icPiQj2CFhDBLRL8jV4hnRFnUC1DWRUVvJHwyBcIMOnXsKAnHqUy6XmLF9MJR9kLVpwe8S1sFoxrhSvkSyf8zisD/iVHv7GuGWMwdSWg4dNA+M6EAZR7kmZQKtJHLuV86kzu/JE1YtKexHF8Wsx7XrM7Ii5dTUfHk2zdm3ZxvC0jODspkfnpM5zZ43Kue8LUUE0s7st6keHIV30dWV9L4v2gsrzbSRIO5";
//            cipherText = URLEncoder.encode(cipherText, "UTF-8");
            System.out.println("===" + cipherText);

            String port = getPort(feIpList.get(feIndex));

            logger.debug("Port set to: {}", port);

            // Read SQL from file and skip commented lines
            String sql = getSQL();
            logger.debug("SQL loaded from file");

            String userName = dbConfig.getUsername();
            String source = sourceName;
            String password = dbConfig.getPassword();

            Properties properties = new Properties();
            properties.put("user", "local".equals(source) ? userName : userName + "$" + source);
            properties.put("password", password);
            String dbUrl = String.format(DB_URL_TEMPLATE, feIpList.get(feIndex), port, cipherText);
            logger.info("Connecting to database: {}", dbUrl);

            try (Connection connection = DriverManager.getConnection(dbUrl, properties)){
                logger.info("Database connection established");
                if (isInteractive) {
                    logger.debug("Starting interactive mode");
                    try {
                        interactive(connection);
                        logger.debug("Interactive mode completed");
                    } catch (Exception e) {
                        logger.error("Error in interactive mode: ", e);
                    }
                } else {
                    logger.debug("Starting non-interactive mode");
                    for (int i = 0; i < iterateTime; i++) {
                        executeMultiSql(connection, sql, errorOnlyMode);
                        Thread.sleep(3000);
                    }
                    logger.debug("Non-interactive mode completed");
                }
            } catch (SQLException e) {
                logger.error("SQL Error: ", e);
            } catch (InterruptedException e) {
                logger.error("Thread interrupted: ", e);
            }

            logger.debug("JdbcDemo application finished");
            // 添加一个小的延迟，确保所有日志都被输出
            Thread.sleep(1000);
        } catch (Exception e) {
            logger.error("Unexpected error in JdbcDemo: ", e);
        }
    }

    /**
     * 从指定文件路径读取 SQL 语句，并替换其中的 {TX_DATE} 占位符为昨天的日期。
     * @return 替换后的 SQL 语句，若读取文件失败则返回空字符串。
     */
    private static String getSQL() {
        StringBuilder sqlBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(JdbcDemo.SQL_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("--")) {
                    sqlBuilder.append(line).append(" ");
                }
            }

            String sql = sqlBuilder.toString().trim();

            if (sql.contains("{TX_DATE}")) {
                LocalDate yesterday = LocalDate.now().minusDays(1);
                String yesterdayStr = yesterday.format(DateTimeFormatter.ISO_LOCAL_DATE);
                sql = sql.replace("{TX_DATE}", yesterdayStr);
            }

            return sql;
        } catch (IOException e) {
            logger.error("Error reading SQL file: ", e);
            return ""; // Return empty string in case of error
        }
    }

    /**
     * 根据地址获取端口号
     * @param address IP地址或域名
     * @return 对应地址的端口号
     */
    private static String getPort(String address) {
        return ServerPortConfig.getPort(address);
    }
}
