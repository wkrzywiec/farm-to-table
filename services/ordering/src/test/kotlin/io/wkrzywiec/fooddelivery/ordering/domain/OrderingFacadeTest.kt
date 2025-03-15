package io.wkrzywiec.fooddelivery.ordering.domain

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody
import io.wkrzywiec.fooddelivery.commons.incoming.AddTip
import io.wkrzywiec.fooddelivery.commons.incoming.CancelOrder
import io.wkrzywiec.fooddelivery.commons.infra.messaging.FakeMessagePublisher
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Message
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Message.message
import io.wkrzywiec.fooddelivery.commons.infra.repository.InMemoryEventStore
import io.wkrzywiec.fooddelivery.ordering.domain.incoming.FoodDelivered
import io.wkrzywiec.fooddelivery.ordering.domain.incoming.FoodInPreparation
import io.wkrzywiec.fooddelivery.ordering.domain.outgoing.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class OrderingFacadeTest {

    private val ordersChannel = "orders"

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

        //then
        val expectedEvent = order.orderCreated()
        val storedEvents = eventStore.getEventsForOrder(order.id)
        assertEquals(storedEvents.size,  1)
        assertEquals(storedEvents[0].body(), expectedEvent)

        assertOrderStatus(storedEvents, OrderStatus.CREATED)

        //OrderCreated event is published on 'orders' channel
        verifyThatEventWasPublished(order, expectedEvent)
    }

    @Test
    fun `Order is cancelled`() {
        //given
        val order = OrderTestData.anOrder()
        eventStore.store(message("orders", testClock, order.orderCreated()))

        val cancellationReason = "Not hungry anymore"
        val cancelOrder = CancelOrder(order.id, cancellationReason)

        //when
        facade.handle(cancelOrder)

        //then: "Event is saved in a store"
        val expectedEvent = OrderCanceled(order.id, cancellationReason)
        val storedEvents = eventStore.getEventsForOrder(order.id)
        assertEquals(storedEvents.size, 2)
        assertEquals(storedEvents[1].body(), expectedEvent)

        //and
        assertOrderStatus(storedEvents, OrderStatus.CANCELED)

        //and: "OrderCancelled event is published on 'orders' channel"
        verifyThatEventWasPublished(order, expectedEvent)
    }

    @Test
    fun `Order is set to IN_PROGRESS`() {
        //given:
        val order = OrderTestData.anOrder()
        eventStore.store(message("orders", testClock, order.orderCreated()))

        //and:
        val foodInPreparation = FoodInPreparation(order.id)

        //when:
        facade.handle(foodInPreparation)

        //then: "Event is saved in a store"
        val expectedEvent = OrderInProgress(order.id)
        val storedEvents = eventStore.getEventsForOrder(order.id)
        assertEquals(storedEvents.size, 2)
        assertEquals(storedEvents[1].body(), expectedEvent)

        //and
        assertOrderStatus(storedEvents, OrderStatus.IN_PROGRESS)

        //and: "OrderInProgress event is published on 'orders' channel"
        verifyThatEventWasPublished(order, expectedEvent)
    }

    @Test
    fun `A tip is added to an order`() {
        //given:
        val itemCost = 10.0
        val deliveryCharge = 5.0

        val order = OrderTestData.anOrder()
                .withItems(ItemTestData.anItem().withPricePerItem(itemCost))
                .withDeliveryCharge(deliveryCharge)
        eventStore.store(message("orders", testClock, order.orderCreated()))

        //and:
        val tip = 20.0
        val addTip = AddTip(order.id, BigDecimal(tip))

        //when:
        facade.handle(addTip)

        //then: "Tip was added"
        val total = itemCost + deliveryCharge + tip
        val expectedEvent = TipAddedToOrder(order.id, BigDecimal(tip), BigDecimal(total.toDouble()))
        val storedEvents = eventStore.getEventsForOrder(order.id)
        assertEquals(storedEvents.size, 2)
        val tipAdded = storedEvents[1].body() as TipAddedToOrder
        assertEquals(tipAdded.tip.toDouble(), tip)
        assertEquals(tipAdded.total.toDouble(), total)

        assertEquals( Order.from(storedEvents).total.toDouble(), total)

        //and: "TipAddedToOrder event is published on 'orders' channel"
        val event = publisher.messages[ordersChannel]?.get(0)
            ?: throw IllegalStateException("No messages were published!")

        verifyEventHeader(
            event,
            expectedOrderId = order.id,
            expectedEventClass = expectedEvent::class)
        val body = event.body as TipAddedToOrder
        assertEquals(expectedEvent.id, body.orderId())
        assertEquals(expectedEvent.tip, body.tip )
        assertEquals(expectedEvent.total.toDouble(), body.total.toDouble())
    }

    @Test
    fun `Order is completed`() {
        //given:
        val order = OrderTestData.anOrder()
        eventStore.store(message("orders", testClock, order.orderCreated()))
        eventStore.store(message("orders", testClock, OrderInProgress(order.id)))

        //and:
        val foodDelivered = FoodDelivered(order.id)

        //when:
        facade.handle(foodDelivered)

        //then: "Order is completed"
        val expectedEvent = OrderCompleted(order.id)
        val storedEvents = eventStore.getEventsForOrder(order.id)
        assertEquals(storedEvents.size, 3)
        assertEquals(storedEvents[2].body(), expectedEvent)

        //and:
        assertOrderStatus(storedEvents, OrderStatus.COMPLETED)

        //and: "OrderCompleted event is published on 'orders' channel"
        verifyThatEventWasPublished(order, expectedEvent)
    }

    private fun assertOrderStatus(storedEvents: List<Message>, expectedStatus: OrderStatus) {
        assertEquals(Order.from(storedEvents).status, expectedStatus)
    }

    private fun verifyThatEventWasPublished(
        order: OrderTestData,
        expectedEvent: DomainMessageBody
    ) {
        val event = publisher.messages[ordersChannel]?.get(0)
            ?: throw IllegalStateException("No messages were published!")

        verifyEventHeader(
            event,
            expectedOrderId = order.id,
            expectedEventClass = expectedEvent::class)
        verifyBody(event, expectedEvent)
    }

    private fun<T : DomainMessageBody> verifyEventHeader(
        event: Message, expectedOrderId: String,
        expectedEventClass: KClass<T>
    ) {
        val header = event.header()
        assertNotNull(header.messageId())
        assertEquals(ordersChannel, header.channel())
        assertEquals(expectedEventClass.simpleName, header.type())
        assertEquals(expectedOrderId, header.itemId())
        assertEquals(testClock.instant(), header.createdAt())
    }

    private fun verifyBody(
        event: Message,
        expectedEvent: DomainMessageBody
    ) {
        val body = event.body()
        assertEquals(body, expectedEvent)
    }
}