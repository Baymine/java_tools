package com.cluster.core;

import lombok.Getter;

@Getter
public class DorisCluster {
    private final String name;
    private final String jdbcUrl;
    private final String user;
    private final String password;

    private DorisCluster(Builder builder) {
        this.name = builder.name;
        this.jdbcUrl = builder.jdbcUrl;
        this.user = builder.user;
        this.password = builder.password;
    }

    public static class Builder {
        private String name;
        private String jdbcUrl;
        private String user;
        private String password;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder jdbcUrl(String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
            return this;
        }

        public Builder user(String user) {
            this.user = user;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public DorisCluster build() {
            // Add validation if needed
            return new DorisCluster(this);
        }
    }
} 