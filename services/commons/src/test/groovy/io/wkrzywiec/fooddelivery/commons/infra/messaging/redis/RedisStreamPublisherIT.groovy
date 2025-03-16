package io.wkrzywiec.fooddelivery.commons.infra.messaging.redis

import io.wkrzywiec.fooddelivery.commons.CommonsIntegrationTest
import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody
import io.wkrzywiec.fooddelivery.commons.infra.ObjectMapperConfig
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Header
import io.wkrzywiec.fooddelivery.commons.infra.messaging.IntegrationMessage
import io.wkrzywiec.fooddelivery.commons.infra.messaging.MessagePublisher
import io.wkrzywiec.fooddelivery.commons.infra.RedisStreamTestClient

import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.StringRedisSerializer

import java.time.Instant

import static io.wkrzywiec.fooddelivery.commons.infra.IntegrationTestMessageBody.aSampleEvent

class RedisStreamPublisherIT extends CommonsIntegrationTest {

    private final String testChannel = "testing-channel"

    private MessagePublisher messagePublisher
    private RedisStreamTestClient redis

    def setup() {
        def config = new ObjectMapperConfig()
        def redisTemplate = configRedisTemplate()
        messagePublisher = new RedisStreamPublisher(redisTemplate, config.objectMapper())

        redis = new RedisStreamTestClient(redisTemplate)

        System.out.println("Clearing '$testChannel' stream from old messages")
        redisTemplate.opsForStream().trim(testChannel, 0)
    }

    def "Publish JSON message to Redis stream"() {
        given: "A message"
        UUID itemId = UUID.randomUUID()
        IntegrationMessage message = event(
                itemId,
                aSampleEvent(Instant.now())
        )

        when: "Publish message"
        messagePublisher.send(message)

        then: "Message was published on $testChannel redis stream"
        def publishedMessage = redis.getLatestMessageFromStreamAsJson(testChannel)

        publishedMessage.get("header").get("streamId").asText() == itemId.toString()
        publishedMessage.get("header").get("type").asText() == "IntegrationTestMessageBody"
        publishedMessage.get("body").get("orderId").asText() == message.body().orderId().toString()
    }

    private RedisTemplate configRedisTemplate() {
        RedisStandaloneConfiguration redisConfiguration = new RedisStandaloneConfiguration(REDIS_HOST, REDIS_PORT)
        def redisConnectionFactory = new LettuceConnectionFactory(redisConfiguration)
        redisConnectionFactory.afterPropertiesSet()

        RedisTemplate<String, String> redisTemplate = new RedisTemplate<String, String>()
        redisTemplate.setConnectionFactory(redisConnectionFactory)
        redisTemplate.setKeySerializer(new StringRedisSerializer())
        redisTemplate.setHashKeySerializer(new StringRedisSerializer())
        redisTemplate.setValueSerializer(new StringRedisSerializer())
        redisTemplate.setHashValueSerializer(new StringRedisSerializer())
        redisTemplate.afterPropertiesSet()

        return redisTemplate
    }

    private IntegrationMessage event(UUID itemId, IntegrationMessageBody eventBody) {
        return new IntegrationMessage(eventHeader(itemId, eventBody.getClass().getSimpleName()), eventBody)
    }

    private Header eventHeader(UUID itemId, String messageType) {
        return new Header(UUID.randomUUID(), 1, testChannel, messageType, itemId, Instant.now())
    }
}
