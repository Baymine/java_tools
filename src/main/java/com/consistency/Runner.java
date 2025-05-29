package com.consistency;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SQL一致性验证工具的主类
 * 从CSV文件读取SQL查询，在Doris集群中执行，并比较结果集行数
 */
public class Runner {
    private static final Logger LOGGER = Logger.getLogger(Runner.class.getName());
    private static final int DEFAULT_THREAD_COUNT = 10;
    
    public static void main(String[] args) {
//        if (args.length < 1) {
//            System.out.println("Usage: java -cp <classpath> com.consistency.runner <csv_file_path> [thread_count] [doris_host]");
//            System.out.println("Example: java -cp target/java_tools-1.0-SNAPSHOT.jar com.consistency.runner src/main/java/com/consistency/sql_data/data.csv 20 doris.example.com");
//            return;
//        }
//
//        String csvFilePath = args[0];
//        int threadCount = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_THREAD_COUNT;
//        String dorisHost = args.length > 2 ? args[2] : "localhost";
        String csvFilePath = "/data3/caokaihua1/Testing/data_test.csv";
        int threadCount = 2;
        String dorisHost = "6.6.162.139";

        try {
            SqlValidator validator = new SqlValidator(csvFilePath, threadCount, dorisHost, true);
            validator.validateSql();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error executing SQL validation", e);
        }
    }
}
