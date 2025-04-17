package com.cluster.alignment;

import com.cluster.core.DorisCluster;

import java.sql.SQLException;
import java.util.Map;

public interface ConfigAlignment {
    void align(DorisCluster cluster, Map<String, String> referenceConfig) throws SQLException;
} 