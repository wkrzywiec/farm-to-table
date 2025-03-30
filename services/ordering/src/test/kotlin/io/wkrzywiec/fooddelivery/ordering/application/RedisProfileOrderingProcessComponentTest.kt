package io.wkrzywiec.fooddelivery.ordering.application

import com.github.javafaker.Faker
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Header
import io.wkrzywiec.fooddelivery.commons.infra.messaging.IntegrationMessage
import io.wkrzywiec.fooddelivery.commons.infra.store.EventStore
import io.wkrzywiec.fooddelivery.ordering.IntegrationTest
import io.wkrzywiec.fooddelivery.ordering.domain.ItemTestData
import io.wkrzywiec.fooddelivery.ordering.domain.OrderTestData
import io.wkrzywiec.fooddelivery.ordering.domain.OrderingEvent
import io.wkrzywiec.fooddelivery.ordering.domain.OrderingFacade.Companion.ORDERS_CHANNEL
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

@DirtiesContext
@ActiveProfiles(profiles = ["redis-stream", "redis-event-store"])
//@Subject([RedisOrdersChannelConsumer, OrderingFacade, RedisEventStore])
class RedisProfileOrderingProcessComponentTest: IntegrationTest() {

    @Autowired
    private lateinit var eventStore: EventStore

    @Test
    fun `Message is consumed and processed correctly`() {
//        given: "CreateOrder command"
        val faker = Faker()
        val order = OrderTestData.anOrder()
            .withItems(
                ItemTestData.anItem().withName(faker.food().dish()).withPricePerItem(2.5),
                ItemTestData.anItem().withName(faker.food().dish()).withPricePerItem(3.0)
            )
            .withAddress(faker.address().fullAddress())

        val body = order.createOrder()
        val header = Header(UUID.randomUUID(), 1, "orders", body::class.simpleName, order.id, Instant.now())
        val message = IntegrationMessage(header, body)

//        when: "is published"
        redisStreamsClient.publishMessage(message)

//        then: "resulting event is published"
        await().atMost(5, TimeUnit.SECONDS)
            .until {
                val event = redisStreamsClient.getLatestMessageFromStreamAsJson("orders")

                order.id.toString() == event.get("header").get("streamId").asText()
                        &&
                        "OrderCreated" == event.get("header").get("type").asText()
            }

//        and: "event is saved in event store"
        val events = eventStore.loadEvents(ORDERS_CHANNEL, order.id)
        assertEquals(1, events.size)
        assertEquals("OrderCreated", events[0].type())

        val eventBody = events[0].data()
        assertTrue(eventBody is OrderingEvent.OrderCreated)

        val expectedEvent = OrderingEvent.OrderCreated(
            order.id, 0, order.customerId,
            order.farmId, order.address,
            order.items.map {i -> i.entity()},
            order.deliveryCharge, order.total())
        assertEquals(expectedEvent, eventBody)
    }
}