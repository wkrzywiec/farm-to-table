package io.wkrzywiec.fooddelivery.bff.inbox.redis

import io.wkrzywiec.fooddelivery.bff.IntegrationTestWithSpring
import io.wkrzywiec.fooddelivery.bff.application.controller.model.AddTipDTO
import io.wkrzywiec.fooddelivery.bff.domain.inbox.redis.RedisInbox
import io.wkrzywiec.fooddelivery.bff.domain.inbox.redis.RedisInboxListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Subject

import java.util.concurrent.TimeUnit

import static org.awaitility.Awaitility.await

@Subject([RedisInbox, RedisInboxListener])
@ActiveProfiles(["redis-inbox", "redis-search"])
@ContextConfiguration(initializers = IntegrationTestContainerInitializer)
class RedisInboxIT extends IntegrationTestWithSpring {

    @Autowired
    private RedisInbox redisInboxPublisher

    def "Store object in inbox and then published"() {
        given:
        def addTip = new AddTipDTO("any-order-id", 2, BigDecimal.valueOf(10))

        when:
        redisInboxPublisher.storeMessage("ordering-inbox:tip", addTip)

        then:
        await().atMost(5, TimeUnit.SECONDS)
                .until {
                    def event = redisStreamsClient.getLatestMessageFromStreamAsJson("orders")
                    event.get("header").get("id").asText() != null
                    event.get("header").get("channel").asText() == "orders"
                    event.get("header").get("type").asText() == "AddTip"
                    event.get("header").get("streamId").asText() == "any-order-id"
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
