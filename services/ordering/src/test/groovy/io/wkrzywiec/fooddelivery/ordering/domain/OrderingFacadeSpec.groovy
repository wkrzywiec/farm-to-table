package io.wkrzywiec.fooddelivery.ordering.domain


import io.wkrzywiec.fooddelivery.commons.infra.messaging.FakeMessagePublisher
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Message
import io.wkrzywiec.fooddelivery.commons.model.AddTip
import io.wkrzywiec.fooddelivery.commons.model.CancelOrder
import io.wkrzywiec.fooddelivery.commons.infra.store.inmemory.InMemoryEventStore
import io.wkrzywiec.fooddelivery.ordering.domain.incoming.FoodDelivered
import io.wkrzywiec.fooddelivery.ordering.domain.incoming.FoodInPreparation
import io.wkrzywiec.fooddelivery.ordering.domain.outgoing.OrderCanceled
import io.wkrzywiec.fooddelivery.ordering.domain.outgoing.OrderCompleted
import io.wkrzywiec.fooddelivery.ordering.domain.outgoing.OrderCreated
import io.wkrzywiec.fooddelivery.ordering.domain.outgoing.OrderInProgress
import io.wkrzywiec.fooddelivery.ordering.domain.outgoing.TipAddedToOrder
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

import static io.wkrzywiec.fooddelivery.commons.infra.messaging.Message.firstMessage
import static io.wkrzywiec.fooddelivery.ordering.domain.ItemTestData.anItem
import static io.wkrzywiec.fooddelivery.ordering.domain.OrderTestData.anOrder

@Subject(OrderingFacade)
@Title("Specification for ordering process")
class OrderingFacadeSpec extends Specification {

    private final String ORDERS_CHANNEL = "orders"

    OrderingFacade facade
    InMemoryEventStore eventStore
    FakeMessagePublisher publisher

    var testTime = Instant.parse("2022-08-08T05:30:24.00Z")
    Clock testClock = Clock.fixed(testTime, ZoneOffset.UTC)

    def setup() {
        eventStore = new InMemoryEventStore()
        publisher = new FakeMessagePublisher()
        facade = new OrderingFacade(eventStore, publisher, testClock)
    }

    def "Create an order"() {
        given:
        var order = anOrder()
                .withItems(
                        anItem().withName("Pizza").withPricePerItem(2.5),
                        anItem().withName("Spaghetti").withPricePerItem(3.0)
                )

        when:
        facade.handle(order.createOrder())

        then: "Event is saved in a store"
        def expectedEvent = order.orderCreated()
        def storedEvents = eventStore.getEventsForOrder(order.getId())
        storedEvents.size() == 1
        storedEvents[0].body() == expectedEvent

        and:
        Order.from(storedEvents).getStatus() == OrderStatus.CREATED

        and: "OrderCreated event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->

            verifyEventHeader(event, order.id, "OrderCreated")

            def body = event.body() as OrderCreated
            body == expectedEvent
        }
    }

    def "Cancel an order"() {
        given:
        var order = anOrder()
        eventStore.store(firstMessage("orders", testClock, order.orderCreated()))

        and:
        var cancellationReason = "Not hungry anymore"
        var cancelOrder = new CancelOrder(order.id, 2, cancellationReason)

        when:
        facade.handle(cancelOrder)

        then: "Event is saved in a store"
        def expectedEvent = new OrderCanceled(order.getId(), 3, cancellationReason)
        def storedEvents = eventStore.getEventsForOrder(order.getId())
        storedEvents.size() == 2
        storedEvents[1].body() == expectedEvent

        and:
        Order.from(storedEvents).getStatus() == OrderStatus.CANCELED

        and: "OrderCancelled event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->

            verifyEventHeader(event, order.id, "OrderCancelled")
            def body = event.body() as OrderCanceled
            body == expectedEvent
        }
    }

    def "Set order to IN_PROGRESS"() {
        given:
        var order = anOrder()
        eventStore.store(firstMessage("orders", testClock, order.orderCreated()))

        and:
        var foodInPreparation = new FoodInPreparation(order.id, 2)

        when:
        facade.handle(foodInPreparation)

        then: "Event is saved in a store"
        def expectedEvent = new OrderInProgress(order.getId(), 3)
        def storedEvents = eventStore.getEventsForOrder(order.getId())
        storedEvents.size() == 2
        storedEvents[1].body() == expectedEvent

        and:
        Order.from(storedEvents).getStatus() == OrderStatus.IN_PROGRESS

        and: "OrderInProgress event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->

            verifyEventHeader(event, order.id, "OrderInProgress")

            def body = event.body() as OrderInProgress
            body == expectedEvent
        }
    }

    def "Add tip to an order"() {
        given:
        double itemCost = 10
        double deliveryCharge = 5

        var order = anOrder()
                .withItems(anItem().withPricePerItem(itemCost))
                .withDeliveryCharge(deliveryCharge)
        eventStore.store(firstMessage("orders", testClock, order.orderCreated()))

        and:
        double tip = 20
        var addTip = new AddTip(order.id, 2, new BigDecimal(tip))

        when:
        facade.handle(addTip)

        then: "Tip was added"
        double total = itemCost + deliveryCharge + tip
        def storedEvents = eventStore.getEventsForOrder(order.getId())
        storedEvents.size() == 2
        def tipAdded = storedEvents[1].body() as TipAddedToOrder
        tipAdded.tip().doubleValue() == tip
        tipAdded.total().doubleValue() == total

        and:
        Order.from(storedEvents).getTotal().doubleValue() == total

        and: "TipAddedToOrder event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->

            verifyEventHeader(event, order.id, "TipAddedToOrder")

            def body = event.body() as TipAddedToOrder
            body.orderId() == order.id
            body.tip().doubleValue() == tip
            body.total().doubleValue() == total
        }
    }

    def "Complete an order"() {
        given:
        var order = anOrder()
        eventStore.store(firstMessage("orders", testClock, order.orderCreated()))
        eventStore.store(firstMessage("orders", testClock, new OrderInProgress(order.getId(), 2)))

        and:
        var foodDelivered = new FoodDelivered(order.id, 3)

        when:
        facade.handle(foodDelivered)

        then: "Order is completed"
        def expectedEvent = new OrderCompleted(order.getId(), 4)
        def storedEvents = eventStore.getEventsForOrder(order.getId())
        storedEvents.size() == 3
        storedEvents[2].body() == expectedEvent

        and:
        Order.from(storedEvents).getStatus() == OrderStatus.COMPLETED

        and: "OrderCompleted event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->

            verifyEventHeader(event, order.id, "OrderCompleted")

            def body = event.body() as OrderCompleted
            body.orderId() == order.id
        }
    }

    private void verifyEventHeader(Message event, String orderId, String eventType) {
        def header = event.header()
        header.id() != null
        header.channel() == ORDERS_CHANNEL
        header.type() == eventType
        header.streamId() == orderId
        header.createdAt() == testClock.instant()
    }
}
