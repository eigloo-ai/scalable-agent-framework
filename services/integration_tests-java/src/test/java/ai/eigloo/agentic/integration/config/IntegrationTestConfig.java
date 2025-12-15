package ai.eigloo.agentic.integration.config;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.ActiveProfiles;

/**
 * Simple configuration class for integration tests.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
// @ComponentScan(basePackages = {
//         "ai.eigloo.agentic"
// })
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:tc:" + IntegrationTestConfig.POSTGRES_IMAGE + ":///" + IntegrationTestConfig.DATABASE_NAME,
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.datasource.username=" + IntegrationTestConfig.USERNAME,
        "spring.datasource.password=" + IntegrationTestConfig.PASSWORD
})
@ActiveProfiles("test")
public class IntegrationTestConfig {



    public static final String POSTGRES_IMAGE = "postgres:16-alpine";
    public static final String DATABASE_NAME = "agentic_test";
    public static final String USERNAME = "test_user";
    public static final String PASSWORD = "test_password";
} 