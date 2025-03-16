package io.wkrzywiec.fooddelivery.ordering


import io.wkrzywiec.fooddelivery.commons.infra.RedisStreamTestClient
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.springframework.jdbc.BadSqlGrammarException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.util.ResourceUtils
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.shaded.org.apache.commons.io.FileUtils
import org.testcontainers.utility.DockerImageName
import java.nio.charset.StandardCharsets


@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class IntegrationTest {

    protected lateinit var redisStreamsClient: RedisStreamTestClient
    protected lateinit var redisTemplate: RedisTemplate<String, Any>

    @BeforeEach
    fun beforeEachTest() {
        redisTemplate = configRedisTemplate()
        redisStreamsClient = RedisStreamTestClient(redisTemplate)

        cleanRedisStream()
        cleanDb()
    }

    companion object {
        private const val USE_LOCAL_INFRA = false

        private const val REDIS_IMAGE = "redis:7-alpine"
        private var REDIS_HOST = "localhost"
        private var REDIS_PORT = 6379
        private const val REDIS_DEFAULT_PORT = 6379

        private const val DB_NAME = "ordering"
        private const val DB_USERNAME = "ordering"
        private const val DB_PASS = "ordering"
        private var JDBC_URL = "jdbc:postgresql://localhost:5432/ordering"

        protected lateinit var jdbcTemplate: JdbcTemplate

        private val redisContainer = GenericContainer<Nothing>(REDIS_IMAGE).apply {
            withExposedPorts(REDIS_DEFAULT_PORT)
            withReuse(true)
        }

        private val postgresContainer = PostgreSQLContainer(DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName(DB_NAME)
            .withUsername(DB_USERNAME)
            .withPassword(DB_PASS)

        @JvmStatic
        @BeforeAll
        fun startContainers() {
            if (USE_LOCAL_INFRA) return

            startRedis()

            startPostgres()
            jdbcTemplate = configJdbcTemplate()
            prepareDb()
        }

        private fun startRedis() {
            redisContainer.start()
            redisContainer.setWaitStrategy(HostPortWaitStrategy())

            REDIS_HOST = redisContainer.host
            REDIS_PORT = redisContainer.getMappedPort(REDIS_DEFAULT_PORT)
        }

        private fun startPostgres() {
            postgresContainer.start()
            postgresContainer.setWaitStrategy(HostPortWaitStrategy())

            JDBC_URL = postgresContainer.jdbcUrl
        }

        private fun configJdbcTemplate(): JdbcTemplate {
            val dataSource = PGSimpleDataSource()
            dataSource.setUrl(JDBC_URL)
            dataSource.user = DB_USERNAME
            dataSource.password = DB_PASS

            return JdbcTemplate(dataSource)
        }

        private fun prepareDb() {
            val file = ResourceUtils.getFile(
                "classpath:db/changelog/001-create-events.sql"
            )
            val content = FileUtils.readFileToString(file, StandardCharsets.UTF_8)
            jdbcTemplate.execute(content)
        }

        @JvmStatic
        @DynamicPropertySource
        fun registerRedisProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.redis.host") { redisContainer.host }
            registry.add("spring.data.redis.port") { redisContainer.getMappedPort(REDIS_DEFAULT_PORT) }
            registry.add("spring.datasource.url") { JDBC_URL}
            registry.add("spring.datasource.username") { DB_USERNAME }
            registry.add("spring.datasource.password") { DB_PASS }
            registry.add("spring.liquibase.enabled") { true }
        }
    }



    private fun cleanDb() {
        println("Clearing postgres database")

        try {
            jdbcTemplate.execute("TRUNCATE events")
        } catch (exception: BadSqlGrammarException) {
            println("!!!! BAD SQL GRAMMAR in cleaning database. Msg: ${exception.message}")
        }
    }

    private fun configRedisTemplate(): RedisTemplate<String, Any> {
        val redisConfiguration = RedisStandaloneConfiguration()
        redisConfiguration.hostName = REDIS_HOST
        redisConfiguration.port = REDIS_PORT

        val redisConnectionFactory = LettuceConnectionFactory(redisConfiguration)
        redisConnectionFactory.afterPropertiesSet()

        val redisTemplate =  RedisTemplate<String, Any>()
        redisTemplate.connectionFactory = redisConnectionFactory
        redisTemplate.keySerializer = StringRedisSerializer()
        redisTemplate.hashKeySerializer = StringRedisSerializer()
        redisTemplate.valueSerializer = StringRedisSerializer()
        redisTemplate.hashValueSerializer = StringRedisSerializer()
        redisTemplate.afterPropertiesSet()

        return redisTemplate
    }

    private fun cleanRedisStream() {
        println("Clearing 'orders' stream from old messages")
        redisTemplate.opsForStream<String, Any>().trim("orders", 0)
        redisTemplate.opsForStream<String, Any>().trim("ordering::any-id", 0)
    }
}