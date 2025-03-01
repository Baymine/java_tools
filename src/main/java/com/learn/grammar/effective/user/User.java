package com.learn.grammar.effective.user;

// 应用第2条：遇到多个构造器参数时要考虑使用构建器
// 应用第68条：遵守普遍接受的命名惯例
public class User {
    private final String username;
    private final String email;
    private final String password;
    private final int age;
    private final String address;

    private User(Builder builder) {
        this.username = builder.username;
        this.email = builder.email;
        this.password = builder.password;
        this.age = builder.age;
        this.address = builder.address;
    }

    public static class Builder {
        // 必需参数
        private final String username;
        private final String email;
        private final String password;

        // 可选参数
        private int age = 0;
        private String address = "";

        public Builder(String username, String email, String password) {
            this.username = username;
            this.email = email;
            this.password = password;
        }

        public Builder age(int age) {
            this.age = age;
            return this;
        }

        public Builder address(String address) {
            this.address = address;
            return this;
        }

        public User build() {
            return new User(this);
        }
    }

    // Getters
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public int getAge() { return age; }
    public String getAddress() { return address; }

    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", age=" + age +
                ", address='" + address + '\'' +
                '}';
    }
}
