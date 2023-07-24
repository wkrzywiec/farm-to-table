package io.wkrzywiec.fooddelivery.bff.inbox.redis

import io.wkrzywiec.fooddelivery.bff.controller.model.AddTipDTO
import io.wkrzywiec.fooddelivery.commons.IntegrationTest
import io.wkrzywiec.fooddelivery.commons.infra.RedisStreamTestClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import spock.lang.Subject

import java.util.concurrent.TimeUnit

import static org.awaitility.Awaitility.await

@Subject([RedisInbox, RedisInboxListener])
@ActiveProfiles(["redis-inbox", "redis-stream"])
@ContextConfiguration(initializers = IntegrationTestContainerInitializer)
class RedisInboxIT extends IntegrationTest {

    private static final GenericContainer REDIS
    protected static final String REDIS_HOST
    protected static final Integer REDIS_PORT

    protected RedisStreamTestClient redisStreamsClient

    @Autowired
    private RedisTemplate redisTemplate

    static {

        if (useLocalInfrastructure()) {
            REDIS_HOST = "localhost"
            REDIS_PORT = 6379
            return
        }

        REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379)
        REDIS.start()
        REDIS_HOST = REDIS.getHost()
        REDIS_PORT = REDIS.getMappedPort(6379)
    }

    def setup() {
        redisStreamsClient = new RedisStreamTestClient(redisTemplate)

        System.out.println("Clearing 'orders' stream from old messages")
        redisTemplate.opsForStream().trim("orders", 0)
        redisTemplate.opsForStream().trim("ordering::any-id", 0)
    }

    @Autowired
    private RedisInbox redisInboxPublisher

    def "Store object in inbox and then published"() {
        given:
        def addTip = new AddTipDTO("any-order-id", BigDecimal.valueOf(10))

        when:
        redisInboxPublisher.storeMessage("ordering-inbox:tip", addTip)

        then:
        await().atMost(5, TimeUnit.SECONDS)
                .until {
                    def event = redisStreamsClient.getLatestMessageFromStreamAsJson("orders")
                    event.get("header").get("messageId").asText() != null
                    event.get("header").get("channel").asText() == "orders"
                    event.get("header").get("type").asText() == "AddTip"
                    event.get("header").get("itemId").asText() == "any-order-id"
                    event.get("header").get("createdAt").asText() != null
                    event.get("body").get("orderId").asText() == "any-order-id"
                }
    }

    static class IntegrationTestContainerInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertyValues values = TestPropertyValues.of(
                    "spring.redis.host=" + REDIS_HOST,
                    "spring.redis.port=" + REDIS_PORT
            )

            values.applyTo(applicationContext)
        }
    }
}
