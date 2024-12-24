
import config.ConfigLoader;
import config.ConfigurationNotFoundException;
import rsa.conf.DatabaseConfig;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigLoaderTest {

    private ConfigLoader configLoader;

    @BeforeEach
    void setUp() {
        configLoader = ConfigLoader.getInstance();
    }

    @Test
    void testSingletonInstance() {
        ConfigLoader anotherInstance = ConfigLoader.getInstance();
        assertSame(configLoader, anotherInstance, "ConfigLoader should be a singleton");
    }

    @Test
    void testGetLakehouseDBConfig_ValidConfig() {
        // Assuming you have a valid configuration in your application.conf
        DatabaseConfig dbConfig = configLoader.getLakehouseDBConfig("validSourceName");
        assertNotNull(dbConfig, "DatabaseConfig should not be null");
        assertEquals("expectedUsername", dbConfig.getUsername());
        assertEquals("expectedPassword", dbConfig.getPassword());
    }

    @Test
    void testGetLakehouseDBConfig_MissingConfig() {
        Exception exception = assertThrows(ConfigurationNotFoundException.class, () -> {
            configLoader.getLakehouseDBConfig("invalidSourceName");
        });
        assertEquals("Configuration for group 'invalidSourceName' not found.", exception.getMessage());
    }

    @Test
    void testGetLakehouseDBConfig_MissingFields() {
        // Create a mock or a temporary config file with missing fields
        Config mockConfig = ConfigFactory.parseString("lakehouse.validSourceName { }");
        // You may need to set this mock config in your ConfigLoader if it allows for it

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            configLoader.getLakehouseDBConfig("validSourceName");
        });
        assertEquals("Required configuration fields are missing.", exception.getMessage());
    }
}