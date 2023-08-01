package io.wkrzywiec.fooddelivery.delivery

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class IntegrationTestWithSpring extends IntegrationTest {

    @DynamicPropertySource
    static void registerContainerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", () -> REDIS_HOST)
        registry.add("spring.redis.port", () -> REDIS_PORT)
        registry.add("spring.datasource.url", () -> JDBC_URL)
        registry.add("spring.datasource.username", () -> DB_USERNAME)
        registry.add("spring.datasource.password", () -> DB_PASS)
        registry.add("spring.liquibase.enabled", () -> true)
    }
}
