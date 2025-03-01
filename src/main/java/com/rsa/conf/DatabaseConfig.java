package com.rsa.conf;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DatabaseConfig {
    private String username;
    private String password;
    private String cipherKey;
    private String hadoopUserName;
    private String hadoopUserToken;

    public DatabaseConfig() {}

    public DatabaseConfig(String username, String password, String cipherKey, String hadoopUserName, String hadoopUserToken) {
        this.username = username;
        this.password = password;
        this.cipherKey = cipherKey;
        this.hadoopUserName = hadoopUserName;
        this.hadoopUserToken = hadoopUserToken;
    }

    @Override
    public String toString() {
        return "DatabaseConfig{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", cipherKey='" + cipherKey + '\'' +
                ", hadoopUserName='" + hadoopUserName + '\'' +
                ", hadoopUserToken='" + hadoopUserToken + '\'' +
                '}';
    }
}
