package com.cluster.observers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConsoleLogger implements ConfigurationObserver {
    private static final Logger LOGGER = LogManager.getLogger(ConsoleLogger.class.getName());

    @Override
    public void onConfigurationChange(String message) {
        LOGGER.info(message);
    }
} 