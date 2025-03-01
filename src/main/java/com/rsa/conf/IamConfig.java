package com.rsa.conf;

import lombok.Data;

@Data
public class IamConfig {
    private String IAMEndpoint;

    private String defaultSource;

    private String IAMToken;

    private String erp;

    private String hadoopUserName;

    public IamConfig(String IAMEndpoint, String defaultSource, String IAMToken, String erp, String hadoopUserName) {
        this.IAMEndpoint = IAMEndpoint;
        this.defaultSource = defaultSource;
        this.IAMToken = IAMToken;
        this.erp = erp;
        this.hadoopUserName = hadoopUserName;
    }


    @Override
    public String toString() {
        return "IamConfig{" +
                "IAMEndpoint='" + IAMEndpoint + '\'' +
                ", defaultSource='" + defaultSource + '\'' +
                ", IAMToken='" + IAMToken + '\'' +
                '}';
    }
}
