package com.learn.grammar.effective.chapter1;

import javax.xml.crypto.Data;
import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

enum DatabaseManager {
    INSTANCE;

    private ConnectionPool connectionPool;

    static {
        try {
            INSTANCE.connectionPool = ConnectionPool.create()
                    .withMaxConnections(10)
                    .withDatabaseUrl("jdbc:mysql://localhost:3306/mydb")
                    .withUsername("root")
                    .withPassword("password")
                    .build();
        } catch (SQLException e) {
            throw new ExceptionInInitializerError("Failed to initialize ConnectionPool: " + e.getMessage());
        }
    }

    public Connection getConnection() throws SQLException {
        return connectionPool.getConnection();
    }

    // 禁止实例化
    private DatabaseManager() {}
}

class ConnectionPool{
    private final int maxConnections;
    private final String databaseUrl;
    private final String username;
    private final String password;

    ConnectionPool(Builder builder){
        this.maxConnections = builder.maxConnections;
        this.databaseUrl = builder.databaseUrl;
        this.username = builder.username;
        this.password = builder.password;
    }

    // 1. 使用静态工厂方法代替构造器
    public static Builder create() {
        return new Builder();
    }

    public Connection getConnection() throws SQLException {
        // 6. 避免创建不必要的对象
        return DriverManager.getConnection(databaseUrl, username, password);
    }

    public static class Builder {
        private int maxConnections = 10; // 默认值
        private String databaseUrl;
        private String username;
        private String password;

        public Builder withMaxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
            return this;
        }

        public Builder withDatabaseUrl(String databaseUrl) {
            this.databaseUrl = Objects.requireNonNull(databaseUrl, "databaseUrl cannot be null");
            return this;
        }

        public Builder withUsername(String username) {
            this.username = Objects.requireNonNull(username, "username cannot be null");
            return this;
        }

        public Builder withPassword(String password) {
            this.password = Objects.requireNonNull(password, "password cannot be null");
            return this;
        }

        public ConnectionPool build() throws SQLException {
            // 可以添加校验逻辑
            if (databaseUrl == null || username == null || password == null) {
                throw new IllegalStateException("databaseUrl, username, and password must be provided");
            }
            return new ConnectionPool(this);
        }
    }
}

// 一个不可实例化的工具类
final class Utils {
    // 4. 通过私有构造器强化不可实例化的能力
    private Utils() {
        throw new AssertionError("Cannot instantiate Utils");
    }

    private static void performUtilityTask () {}
}

class ResourceHandler {
    // 9. try-with-resources 优于 try-finally
    public void handleResource() {
        try (CloseableResource resource = new CloseableResource()) {
            resource.use();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class CloseableResource implements Closeable {
    @Override
    public void close() throws IOException {
        System.out.println("Resource closed");
    }

    public void use() throws IOException {
        System.out.println("Using resource");
    }
}

public class EffectiveJavaDemo {
    public static void main(String[] args) {
        try (Connection conn = DatabaseManager.INSTANCE.getConnection()){
            System.out.println("Database connection obtained: " + conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 7. 消除过期的引用对象
    private static void eliminateObsoleteReferences() {
        Object obj = new Object();

        // using obj...

        obj = null; // 使对象称为垃圾回收的候选
    }

    // 6.避免创建不必要的类
    private static void avoidUnnecessaryObjects() {
        String str1 = "Hello";
        String str2 = "Hello";

        System.out.println(str1 == str2);
    }

    static class SingletonThreadSafetyDemo {
        public static void runDemo() {
            Runnable task = () -> {
                DatabaseManager instance = DatabaseManager.INSTANCE;
                System.out.println("Instance hashcode: " + instance.hashCode());
            };

            Thread thread1 = new Thread(task);
            Thread thread2 = new Thread(task);
            thread1.start();
            thread2.start();
        }
    }
}


