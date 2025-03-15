package io.wkrzywiec.fooddelivery.ordering

import io.restassured.RestAssured
import io.restassured.module.mockmvc.RestAssuredMockMvc
import io.wkrzywiec.fooddelivery.commons.infra.RedisStreamTestClient
import io.wkrzywiec.fooddelivery.commons.infra.messaging.redis.RedisMessagePublisherConfig
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.context.WebApplicationContext
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class IntegrationTest {

    protected lateinit var redisStreamsClient: RedisStreamTestClient

    @Autowired
    private lateinit var context: WebApplicationContext

    @LocalServerPort
    private var port: Int = 0

    @BeforeEach
    fun setup() {
        RestAssuredMockMvc.webAppContextSetup(context)
        RestAssured.port = port

        val config = RedisMessagePublisherConfig()
        val redisStandaloneConfig = RedisStandaloneConfiguration(REDIS_HOST, REDIS_PORT)
        val connectionFactory = LettuceConnectionFactory(redisStandaloneConfig)
        connectionFactory.afterPropertiesSet()
        val redisTemplate = config.redisTemplate(connectionFactory)
        redisStreamsClient = RedisStreamTestClient(redisTemplate)

        println("Clearing 'orders' stream from old messages")
        redisTemplate.opsForStream<String, Any>().trim("orders", 0)
        redisTemplate.opsForStream<String, Any>().trim("ordering::any-id", 0)
    }

    companion object {
        private const val USE_LOCAL_INFRA = false

        private const val REDIS_IMAGE = "redis:7-alpine"
        private var REDIS_HOST = "localhost"
        private var REDIS_PORT = 6379
        private const val REDIS_DEFAULT_PORT = 6379

        private val redisContainer = GenericContainer<Nothing>(REDIS_IMAGE).apply {
            withExposedPorts(REDIS_DEFAULT_PORT)
            withReuse(true)
        }

        @JvmStatic
        @BeforeAll
        fun startContainer() {
            if (USE_LOCAL_INFRA) return

            if (!redisContainer.isRunning) {
                redisContainer.start()
                redisContainer.setWaitStrategy(HostPortWaitStrategy())

                REDIS_HOST = redisContainer.host
                REDIS_PORT = redisContainer.getMappedPort(REDIS_DEFAULT_PORT)
            }
        }

        @JvmStatic
        @DynamicPropertySource
        fun registerRedisProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.redis.host") { redisContainer.host }
            registry.add("spring.data.redis.port") { redisContainer.getMappedPort(REDIS_DEFAULT_PORT) }
        }
    }
}