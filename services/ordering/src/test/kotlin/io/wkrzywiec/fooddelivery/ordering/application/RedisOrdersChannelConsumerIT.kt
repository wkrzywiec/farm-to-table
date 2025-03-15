package io.wkrzywiec.fooddelivery.ordering.application

import com.github.javafaker.Faker
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Header
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Message
import io.wkrzywiec.fooddelivery.ordering.IntegrationTest
import io.wkrzywiec.fooddelivery.ordering.domain.ItemTestData
import io.wkrzywiec.fooddelivery.ordering.domain.OrderTestData
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

@ActiveProfiles("redis")
class RedisOrdersChannelConsumerIT: IntegrationTest() {

    @Test
    fun `Message is consumed correctly`() {
        //given:
        val faker = Faker()
        val order = OrderTestData.anOrder()
                .withItems(
                        ItemTestData.anItem().withName(faker.food().dish()).withPricePerItem(2.5),
                        ItemTestData.anItem().withName(faker.food().dish()).withPricePerItem(3.0)
                )
                .withAddress(faker.address().fullAddress())

        val body = order.createOrder()
        val header = Header(
            UUID.randomUUID().toString(),
            "orders",
            body::class.simpleName,
            order.id,
            Instant.now()
        )
        val message = Message(header, body)

        //when:
        redisStreamsClient.publishMessage(message)

        //then:
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted {
                    val event = redisStreamsClient.getLatestMessageFromStreamAsJson("orders")

                    assertEquals(event.get("header").get("itemId").asText(), order.id)
                    assertEquals(event.get("header").get("type").asText(), "OrderCreated")
                }
    }
}