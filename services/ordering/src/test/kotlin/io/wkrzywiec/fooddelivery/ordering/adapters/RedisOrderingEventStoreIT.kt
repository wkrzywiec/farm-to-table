package io.wkrzywiec.fooddelivery.ordering.adapters

import io.wkrzywiec.fooddelivery.commons.infra.messaging.Header
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Message
import io.wkrzywiec.fooddelivery.commons.infra.repository.RedisEventStore
import io.wkrzywiec.fooddelivery.ordering.IntegrationTest
import io.wkrzywiec.fooddelivery.ordering.domain.outgoing.OrderCompleted
import io.wkrzywiec.fooddelivery.ordering.domain.outgoing.OrderInProgress
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals

@ActiveProfiles("redis")
class RedisOrderingEventStoreIT: IntegrationTest() {

    @Autowired
    private lateinit var eventStore: RedisEventStore

    @Test
    fun `Store an event in event store`() {
        //given:
        val eventBody = OrderInProgress("any-id")
        val eventHeader = Header(UUID.randomUUID().toString(), "orders", eventBody::class.simpleName, eventBody.id, Instant.now())

        //when:
        eventStore.store(Message(eventHeader, eventBody))

        //then:
        val savedEvents = redisStreamsClient.getLatestMessageFromStreamAsJson("ordering::any-id")
        assertEquals( savedEvents.get("header").get("type").asText(), "OrderInProgress")
        assertEquals(savedEvents.get("body").get("id").asText(), "any-id")
    }

    @Test
    fun `Get all events from event store`() {
        //given:
        val orderId = "any-id"

        //and: "First Event"
        val firstEventBody = OrderInProgress("any-id")
        val firstEventHeader = Header(UUID.randomUUID().toString(), "orders", firstEventBody::class.simpleName, firstEventBody.id, Instant.now())
        val firstEvent = Message(firstEventHeader, firstEventBody)

        //and: "Second Event"
        val secondEventBody = OrderCompleted("any-id")
        val secondEventHeader = Header(UUID.randomUUID().toString(), "orders", secondEventBody::class.simpleName, secondEventBody.id, Instant.now())
        val secondEvent = Message(secondEventHeader, secondEventBody)

        //and: "Both events are stored"
        redisStreamsClient.publishMessage("ordering::any-id", firstEvent)
        redisStreamsClient.publishMessage("ordering::any-id", secondEvent)

        //when:
        val storedEvents = eventStore.getEventsForOrder(orderId)

        //then:
        assertEquals(storedEvents.size, 2)
    }
}