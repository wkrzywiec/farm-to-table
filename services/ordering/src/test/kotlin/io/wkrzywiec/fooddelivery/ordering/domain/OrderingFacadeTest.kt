package io.wkrzywiec.fooddelivery.ordering.domain

import io.wkrzywiec.fooddelivery.commons.infra.messaging.FakeMessagePublisher
import io.wkrzywiec.fooddelivery.commons.infra.messaging.IntegrationMessage
import io.wkrzywiec.fooddelivery.commons.infra.store.EventEntity
import io.wkrzywiec.fooddelivery.commons.infra.store.EventEntity.newEventEntity
import io.wkrzywiec.fooddelivery.commons.infra.store.inmemory.InMemoryEventStore
import io.wkrzywiec.fooddelivery.commons.model.AddTip
import io.wkrzywiec.fooddelivery.commons.model.CancelOrder
import io.wkrzywiec.fooddelivery.ordering.domain.OrderingFacade.Companion.ORDERS_CHANNEL
import io.wkrzywiec.fooddelivery.ordering.domain.incoming.FoodDelivered
import io.wkrzywiec.fooddelivery.ordering.domain.incoming.FoodInPreparation
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.*

class OrderingFacadeTest {

    private lateinit var facade: OrderingFacade
    private lateinit var eventStore: InMemoryEventStore
    private lateinit var publisher: FakeMessagePublisher

    private val testTime: Instant = Instant.parse("2022-08-08T05:30:24.00Z")
    private val testClock: Clock = Clock.fixed(testTime, ZoneOffset.UTC)

    @BeforeEach
    fun before() {
        eventStore = InMemoryEventStore()
        publisher = FakeMessagePublisher()
        facade = OrderingFacade(eventStore, publisher, testClock)
    }

    @Test
    fun `Order is created`() {
        //given
        val order = OrderTestData.anOrder()
            .withItems(
                ItemTestData.anItem().withName("Pizza").withPricePerItem(2.5),
                ItemTestData.anItem().withName("Spaghetti").withPricePerItem(3.0)
            )

        //when
        facade.handle(order.createOrder())

//        then: "Event is saved in a store"
        verifyEventInStore(
            order.orderCreated(),
            1
        )

//        and: "OrderCreated event is published on 'orders' channel"
        verifyIntegrationEvent(order.id, "OrderCreated")
    }

    @Test
    fun `Cancel an order`() {
//        given:
        val order = OrderTestData.anOrder()
        eventStore.store(order.orderCreatedEntity(testClock))

//        and:
        val cancellationReason = "Not hungry anymore"
        val cancelOrder = CancelOrder(order.id, 1, cancellationReason)

//        when:
        facade.handle(cancelOrder)

//        then: "Event is saved in a store"
        verifyEventInStore(
            OrderingEvent.OrderCanceled(order.id, 1, "Not hungry anymore"),
        2
        )

//        and: "OrderCancelled event is published on 'orders' channel"
        verifyIntegrationEvent(order.id, "OrderCanceled")
    }

    @Test
    fun `Set order to IN_PROGRESS`() {
//        given:
        val order = OrderTestData.anOrder()
        eventStore.store(order.orderCreatedEntity(testClock))

//        and:
        val foodInPreparation = FoodInPreparation(order.id, 1)

//        when:
        facade.handle(foodInPreparation)

//        then: "Event is saved in a store"
        verifyEventInStore(
            OrderingEvent.OrderInProgress(order.id, 1),
        2
        )

//        and: "OrderInProgress event is published on 'orders' channel"
        verifyIntegrationEvent(order.id, "OrderInProgress")
    }

    @Test
    fun `Add tip to an order`() {
//        given:
        val itemCost: Double = 10.0
        val deliveryCharge: Double = 5.0

        val order = OrderTestData.anOrder()
            .withItems(ItemTestData.anItem().withPricePerItem(itemCost))
            .withDeliveryCharge(deliveryCharge)
        eventStore.store(order.orderCreatedEntity(testClock))

//        and:
        val tip: Double = 20.0
        val addTip = AddTip(order.id, 1, BigDecimal(tip))

//        when:
        facade.handle(addTip)

//        then: "Tip was added"
        val total: Double = 35.0
        verifyEventInStore(
            OrderingEvent.TipAddedToOrder(order.id, 1, BigDecimal(tip), BigDecimal(total)),
        2
        )

//        and: "TipAddedToOrder event is published on 'orders' channel"
        verifyIntegrationEvent(order.id, "TipAddedToOrder")
    }

    @Test
    fun `Complete an order`() {
//        given:
        val order = OrderTestData.anOrder()
        eventStore.store(order.orderCreatedEntity(testClock))
        eventStore.store(eventEntity( OrderingEvent.OrderInProgress(order.id, 1)))

//        and:
        val foodDelivered = FoodDelivered(order.id, 2)

//        when:
        facade.handle(foodDelivered)

//        then: "Order is completed"
        verifyEventInStore(
            OrderingEvent.OrderCompleted(order.id, 2),
        3
        )

//        and: "OrderCompleted event is published on 'orders' channel"
        verifyIntegrationEvent(order.id, "OrderCompleted")
    }

    private fun verifyEventInStore(expectedDomainEvent: OrderingEvent, expectedStreamSize: Int) {
        val expectedEvent = eventEntity(expectedDomainEvent)
        val storedEvents = eventStore.loadEvents(ORDERS_CHANNEL, expectedDomainEvent.streamId())
        assertEquals(storedEvents.size, expectedStreamSize)

        val actualEvent = storedEvents.last()
        eventsAreEqualIgnoringId(expectedEvent, actualEvent)
    }

    private fun eventEntity(orderingEvent: OrderingEvent): EventEntity {
        return newEventEntity(orderingEvent, ORDERS_CHANNEL, testClock)
    }

    private fun eventsAreEqualIgnoringId(expected: EventEntity, actual: EventEntity) {
        Assertions.assertThat(actual)
            .usingComparatorForType(BigDecimal::compareTo, BigDecimal::class.java)
            .usingRecursiveComparison()
            .ignoringFields("id")
            .isEqualTo(expected)
    }

    private fun verifyIntegrationEvent(orderId: UUID, expectedEventType: String) {
        val event = publisher.messages[ORDERS_CHANNEL]?.get(0)
            ?: throw IllegalStateException("No messages were published!")
        verifyEventHeader(event, orderId, expectedEventType)
    }

    private fun verifyEventHeader(event: IntegrationMessage, orderId: UUID, eventType: String) {
        val header = event.header()
        assertNotNull(header.id())
        assertEquals(header.channel(), ORDERS_CHANNEL)
        assertEquals(header.type(),eventType)
        assertEquals(header.streamId(), orderId)
        assertEquals(header.createdAt(), testClock.instant())
    }
}