package config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import rsa.conf.DatabaseConfig;

public class ConfigLoader {
    private Config config;
    private static ConfigLoader instance;


    public ConfigLoader() {
        this.config = ConfigFactory.load();
    }

    public static ConfigLoader getInstance() {
        if (instance == null) {
            synchronized (ConfigLoader.class) {
                if (instance == null) {
                    instance = new ConfigLoader();
                }
            }
        }
        return instance;
    }

    public DatabaseConfig getLakehouseDBConfig(String sourceName) {
        try {
            Config sourceConfig = config.getConfig("lakehouse." + sourceName);

            String username = sourceConfig.getString("username");
            String password = sourceConfig.getString("password");
            String cipherKey = sourceConfig.getString("cipherKey");
            String hadoopUserName = sourceConfig.getString("hadoopUserName");
            String hadoopUserToken = sourceConfig.getString("hadoopUserToken");

            return new DatabaseConfig(username, password, cipherKey, hadoopUserName, hadoopUserToken);
        } catch (ConfigException.Missing e) {
            throw new IllegalArgumentException("Configuration for group '" + sourceName + "' not found.", e);
        }
    }
}
