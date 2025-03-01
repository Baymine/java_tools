package com.learn.grammar.effective;

// 1. 使用静态工厂方法代替构造器
public class DatabaseConnection {

    // 使用枚举类型强化Singleton属性
    public enum Singleton {
        INSTANCE;

        private final DatabaseConnection connection;

        Singleton() {
            connection = new DatabaseConnection();
        }

        public DatabaseConnection getConnection() {
            return connection;
        }
    }

    // 私有构造器，防止外部实例化
    private DatabaseConnection(){
        // init database connection
    }
}
