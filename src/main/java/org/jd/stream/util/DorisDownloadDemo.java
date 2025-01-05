package org.jd.stream.util;

import config.ConfigLoader;
import org.jd.stream.constant.DataDownloadConstant;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import com.rsa.conf.DatabaseConfig;

import java.sql.*;

public class DorisDownloadDemo {
    private static final int FETCH_SIZE = 5000;
    private static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    public static String sourceName = "easyolap";
    private static final ConfigLoader configLoader = ConfigLoader.getInstance();
    public static final DatabaseConfig dbConfig = configLoader.getLakehouseDBConfig(sourceName);


    public static final String USER = dbConfig.getUsername() + "$" + sourceName;
    public static final String PASSWORD = dbConfig.getPassword();
    public static final String HOST_PORT = "drpub931.olap.jd.com:2000";
    public static final String DB = "qC/OvDU6AhrwhPCTR/6IdzLvgWYDgHcMC0EhHVwLVbGVDs/zMx/kJL3j3qh+MbiDfYDaY0+/fs5gcl17RjfZ+Q1fuXNhiEsN0XtPHZ4KgQcC+rIiA+yipwT5i+ZTolDDt7AeQ3zbNDUpCEB9PE2fXEbi9AdL3SQ37hfSD2DgzBQ6Gz9bjqrPhbeLkNZNFCJFL94hN0zu4KQLu6cv5PsvJSlPpWuIXNjP99MllJ6Lc7o=";



    public static void main(String[] args) throws SQLException {
        String CONFIG = "useSSL=false";
        String jdbcUrl = String.format("jdbc:mysql://%s/%s?%s",
                HOST_PORT, DB, CONFIG);

        Connection connection = DriverManager.getConnection(jdbcUrl, USER, PASSWORD);
        DorisDownloadFileHandler dorisDownloadFileHandler = null;
        String fileName = "123.txt";
        try {
            Statement stmt = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY,
                    ResultSet.CONCUR_READ_ONLY);
            stmt.setFetchSize(Integer.MIN_VALUE);

            String sql = "SELECT * FROM hive.dev.app_cs_cse_laga_all_process_sum_d LIMIT 2000000";

            System.out.println("Start task...");
            long start = System.currentTimeMillis();
            ResultSet resultSet = stmt.executeQuery(sql);
            File txtFile = new File(DataDownloadConstant.DOWNLOAD_FILE_DIR, fileName);
            dorisDownloadFileHandler = new DorisDownloadFileHandler(txtFile);

            while (resultSet.next()) {
                dorisDownloadFileHandler.processRow(resultSet);
            }
            dorisDownloadFileHandler.finish();
            System.out.println("Total " + (System.currentTimeMillis() - start) / 1000.0);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}

