package io.wkrzywiec.fooddelivery.bff


import io.wkrzywiec.fooddelivery.commons.infra.RedisStreamTestClient
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.StringRedisSerializer
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
abstract class IntegrationTest extends Specification {

    private static GenericContainer REDIS_CONTAINER
    protected static final String REDIS_HOST = "localhost"
    protected static final Integer REDIS_PORT

    protected RedisStreamTestClient redisStreamsClient
    private RedisTemplate redisTemplate

    private static PostgreSQLContainer POSTGRE_SQL_CONTAINER
    protected static final String JDBC_URL
    protected static final String DB_NAME = "bff"
    protected static final String DB_USERNAME = "bff"
    protected static final String DB_PASS = "bff"

    @Shared
    protected JdbcTemplate jdbcTemplate

    protected static boolean useLocalInfrastructure() {
        // change it to `true` in order to use it with infra from docker-compose.yaml
        false
    }

    static {

        if (useLocalInfrastructure()) {
            REDIS_PORT = 6379
            JDBC_URL = "jdbc:postgresql://localhost:5432/bff"
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

        File file = ResourceUtils.getFile(
                "classpath:db/changelog/001-create-inbox.sql")
        String content = FileUtils.readFileToString(file, StandardCharsets.UTF_8)
        jdbcTemplate.execute(content)
    }

    def setup() {
        redisTemplate = configRedisTemplate()
        redisStreamsClient = new RedisStreamTestClient(redisTemplate)

        cleanRedisStream()
        cleanDb()
    }

    private void cleanRedisStream() {
        System.out.println("Clearing 'orders' stream from old messages")
        redisTemplate.opsForStream().trim("orders", 0)
        redisTemplate.opsForStream().trim("ordering::any-id", 0)
    }

    private void cleanDb() {
        System.out.println("Clearing postgres database")

        try {
            jdbcTemplate.execute("TRUNCATE inbox")
        } catch (BadSqlGrammarException exception) {
            System.out.println("!!!! BAD SQL GRAMMAR in cleaning database. Msg: " + exception.message)
        }
    }

    private JdbcTemplate configJdbcTemplate() {
        def dataSource = new PGSimpleDataSource()
        dataSource.setUrl(JDBC_URL)
        dataSource.setUser(DB_USERNAME)
        dataSource.setPassword(DB_PASS)

        return new JdbcTemplate(dataSource)
    }

    protected RedisConnectionFactory redisConnectionFactory() {
        def redisConfiguration = new RedisStandaloneConfiguration()
        redisConfiguration.setHostName(REDIS_HOST)
        redisConfiguration.setPort(REDIS_PORT)

        def redisConnectionFactory = new LettuceConnectionFactory(redisConfiguration)
        redisConnectionFactory.afterPropertiesSet()
        return redisConnectionFactory
    }

    private RedisTemplate configRedisTemplate() {
        def redisConnectionFactory = redisConnectionFactory()

        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<String, Object>()
        redisTemplate.setConnectionFactory(redisConnectionFactory)
        redisTemplate.setKeySerializer(new StringRedisSerializer())
        redisTemplate.setHashKeySerializer(new StringRedisSerializer())
        redisTemplate.setValueSerializer(new StringRedisSerializer())
        redisTemplate.setHashValueSerializer(new StringRedisSerializer())
        redisTemplate.afterPropertiesSet()

        return redisTemplate
    }
}
