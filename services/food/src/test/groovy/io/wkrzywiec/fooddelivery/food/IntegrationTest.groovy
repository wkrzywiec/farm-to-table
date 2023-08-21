package io.wkrzywiec.fooddelivery.food

import io.wkrzywiec.fooddelivery.commons.infra.RedisStreamTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.spock.Testcontainers
import org.testcontainers.utility.DockerImageName
import spock.lang.Specification

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
abstract class IntegrationTest extends Specification {

    private static GenericContainer REDIS_CONTAINER
    protected static final String REDIS_HOST = "localhost"
    protected static final Integer REDIS_PORT

    protected RedisStreamTestClient redisStreamsClient
    private RedisTemplate redisTemplate

    protected static boolean useLocalInfrastructure() {
        // change it to `true` in order to use it with infra from docker-compose.yaml
        false
    }

    static {

        if (useLocalInfrastructure()) {
            REDIS_PORT = 6379
            return
        }

        REDIS_PORT = initRedis()
    }

    private static int initRedis() {
        REDIS_CONTAINER = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379)
        REDIS_CONTAINER.start()
        return REDIS_CONTAINER.getMappedPort(6379)
    }

    def setup() {
        redisTemplate = configRedisTemplate()
        redisStreamsClient = new RedisStreamTestClient(redisTemplate)

        cleanRedisStream()
    }

    private RedisTemplate configRedisTemplate() {
        RedisStandaloneConfiguration redisConfiguration = new RedisStandaloneConfiguration()
        redisConfiguration.setHostName(REDIS_HOST)
        redisConfiguration.setPort(REDIS_PORT)

        def redisConnectionFactory = new LettuceConnectionFactory(redisConfiguration)
        redisConnectionFactory.afterPropertiesSet()

        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<String, Object>()
        redisTemplate.setConnectionFactory(redisConnectionFactory)
        redisTemplate.setKeySerializer(new StringRedisSerializer())
        redisTemplate.setHashKeySerializer(new StringRedisSerializer())
        redisTemplate.setValueSerializer(new StringRedisSerializer())
        redisTemplate.setHashValueSerializer(new StringRedisSerializer())
        redisTemplate.afterPropertiesSet()

        return redisTemplate
    }

    private void cleanRedisStream() {
        System.out.println("Clearing 'orders' stream from old messages")
        redisTemplate.opsForStream().trim("orders", 0)
        redisTemplate.opsForStream().trim("delivery::any-id", 0)
    }

    @DynamicPropertySource
    static void registerContainerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", () -> REDIS_HOST)
        registry.add("spring.redis.port", () -> REDIS_PORT)
    }
}
