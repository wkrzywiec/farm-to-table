package io.wkrzywiec.fooddelivery.ordering

import org.junit.jupiter.api.BeforeAll
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class IntegrationTest {

    companion object {
        private const val USE_LOCAL_INFRA = false

        private const val REDIS_IMAGE = "redis:7-alpine"
        private const val REDIS_PORT = 6379


        private val redisContainer = GenericContainer<Nothing>(REDIS_IMAGE).apply {
            withExposedPorts(REDIS_PORT)
            withReuse(true)
        }

        @JvmStatic
        @BeforeAll
        fun startContainer() {
            if (USE_LOCAL_INFRA) return

            if (!redisContainer.isRunning) {
                redisContainer.start()
                redisContainer.setWaitStrategy(HostPortWaitStrategy())
            }
        }

        @JvmStatic
        @DynamicPropertySource
        fun registerRedisProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.redis.host") { redisContainer.host }
            registry.add("spring.data.redis.port") { redisContainer.getMappedPort(REDIS_PORT) }
        }
    }
}