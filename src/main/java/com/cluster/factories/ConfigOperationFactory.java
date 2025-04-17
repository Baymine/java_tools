package com.cluster.factories;

import com.cluster.operations.BackendConfigOperation;
import com.cluster.operations.ConfigOperation;
import com.cluster.operations.FrontendConfigOperation;
import com.cluster.operations.VariableConfigOperation;

public class ConfigOperationFactory {
    public static ConfigOperation createOperation(String type) {
        return switch (type.toLowerCase()) {
            case "variable" -> new VariableConfigOperation();
            case "frontend" -> new FrontendConfigOperation();
            case "backend" -> new BackendConfigOperation();
            default -> throw new IllegalArgumentException("Unknown config operation type: " + type);
        };
    }
} 