package com.cluster.operations;

import com.cluster.core.DorisCluster;

import java.sql.SQLException;
import java.util.Map;

public interface ConfigOperation {
    Map<String, String> fetchConfig(DorisCluster cluster) throws SQLException;
} 