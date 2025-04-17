package com.cluster.commands;

import com.cluster.core.DorisCluster;
import com.cluster.alignment.ConfigAlignment;

import java.sql.SQLException;
import java.util.Map;

public class AlignConfigCommand implements ConfigCommand {
    private final DorisCluster targetCluster;
    private final Map<String, String> referenceConfig;
    private final ConfigAlignment alignment;

    public AlignConfigCommand(DorisCluster targetCluster, Map<String, String> referenceConfig, ConfigAlignment alignment) {
        this.targetCluster = targetCluster;
        this.referenceConfig = referenceConfig;
        this.alignment = alignment;
    }

    @Override
    public void execute() throws SQLException {
        alignment.align(targetCluster, referenceConfig);
    }
} 