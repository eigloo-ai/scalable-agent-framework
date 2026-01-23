package ai.eigloo.agentic.integration.config;

import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(classes = IntegrationTestConfig.class)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(BaseIntegrationTest.class);


    private static PostgreSQLContainer<?> postgres;

    /*
        Start a shared, singleton PostgreSQL container for integration tests..
     */
    static {
        postgres = new PostgreSQLContainer<>(DockerImageName.parse(IntegrationTestConfig.POSTGRES_IMAGE))
                .withDatabaseName(IntegrationTestConfig.DATABASE_NAME)
                .withUsername(IntegrationTestConfig.USERNAME)
                .withPassword(IntegrationTestConfig.PASSWORD)
                .withReuse(true);
        postgres.start();
        logger.info("Started shared PostgreSQL container: {}:{}",
                postgres.getHost(), postgres.getMappedPort(5432));
    }

} 