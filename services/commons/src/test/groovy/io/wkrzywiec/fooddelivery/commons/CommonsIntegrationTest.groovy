package io.wkrzywiec.fooddelivery.commons

import com.fasterxml.jackson.databind.ObjectMapper
import io.wkrzywiec.fooddelivery.commons.infra.ObjectMapperConfig
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.jdbc.BadSqlGrammarException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.util.ResourceUtils
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.shaded.org.apache.commons.io.FileUtils
import org.testcontainers.spock.Testcontainers
import org.testcontainers.utility.DockerImageName
import spock.lang.Shared
import spock.lang.Specification

import java.nio.charset.StandardCharsets

@Testcontainers
abstract class CommonsIntegrationTest extends Specification {

    private static GenericContainer REDIS_CONTAINER
    protected static final String REDIS_HOST = "localhost"
    protected static final Integer REDIS_PORT

    private static PostgreSQLContainer POSTGRE_SQL_CONTAINER
    protected static final String JDBC_URL
    protected static final String DB_NAME = "commons"
    protected static final String DB_USERNAME = "commons"
    protected static final String DB_PASS = "commons"

    @Shared
    protected JdbcTemplate jdbcTemplate

    static boolean useLocalInfrastructure() {
        // change it to `true` in order to use it with infra from docker-compose.yaml
        false
    }

    static {
        if (useLocalInfrastructure()) {
            REDIS_PORT = 6379
            JDBC_URL = "jdbc:postgresql://localhost:5432/commons"
            return
        }

        REDIS_PORT = initRedis()
        JDBC_URL = initPostgres()
    }

    private static int initRedis() {
        REDIS_CONTAINER = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379)
        REDIS_CONTAINER.start()
        return REDIS_CONTAINER.getMappedPort(6379)
    }

    private static String initPostgres() {
        POSTGRE_SQL_CONTAINER = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
                .withDatabaseName(DB_NAME)
                .withUsername(DB_USERNAME)
                .withPassword(DB_PASS)

        POSTGRE_SQL_CONTAINER.start()
        return POSTGRE_SQL_CONTAINER.getJdbcUrl()
    }

    def setupSpec() {
        jdbcTemplate = configJdbcTemplate()
        insertTablesIntoDb(jdbcTemplate)
    }

    def setup() {
        cleanDb()
    }

    private JdbcTemplate configJdbcTemplate() {
        def dataSource = new PGSimpleDataSource()
        dataSource.setUrl(JDBC_URL)
        dataSource.setUser(DB_USERNAME)
        dataSource.setPassword(DB_PASS)

        return new JdbcTemplate(dataSource)
    }

    private void insertTablesIntoDb(JdbcTemplate jdbcTemplate) {
        System.out.println("Creating event store table...")
        File file = ResourceUtils.getFile(
                "classpath:create-event-store.sql")
        String content = FileUtils.readFileToString(file, StandardCharsets.UTF_8)
        jdbcTemplate.execute(content)
    }

    protected void cleanDb() {
        System.out.println("Clearing postgres database")

        try {
            jdbcTemplate.execute("TRUNCATE events")
        } catch (BadSqlGrammarException exception) {
            System.out.println("!!!! BAD SQL GRAMMAR in cleaning database. Msg: " + exception.message)
        }
    }

    protected ObjectMapper objectMapper() {
        ObjectMapperConfig config = new ObjectMapperConfig()
        return config.objectMapper()
    }
}
