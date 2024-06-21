package io.wkrzywiec.fooddelivery.ordering.domain

import io.wkrzywiec.fooddelivery.commons.infra.messaging.FakeMessagePublisher
import io.wkrzywiec.fooddelivery.commons.infra.messaging.IntegrationMessage
import io.wkrzywiec.fooddelivery.commons.infra.store.EventEntity
import io.wkrzywiec.fooddelivery.commons.infra.store.inmemory.InMemoryEventStore
import io.wkrzywiec.fooddelivery.commons.model.AddTip
import io.wkrzywiec.fooddelivery.commons.model.CancelOrder
import io.wkrzywiec.fooddelivery.ordering.domain.incoming.FoodDelivered
import io.wkrzywiec.fooddelivery.ordering.domain.incoming.FoodInPreparation
import io.wkrzywiec.fooddelivery.ordering.domain.outgoing.OrderCompleted
import io.wkrzywiec.fooddelivery.ordering.domain.outgoing.OrderInProgress
import io.wkrzywiec.fooddelivery.ordering.domain.outgoing.TipAddedToOrder
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

import static io.wkrzywiec.fooddelivery.commons.infra.messaging.IntegrationMessage.firstMessage
import static io.wkrzywiec.fooddelivery.commons.infra.store.EventEntity.newEventEntity
import static io.wkrzywiec.fooddelivery.ordering.domain.ItemTestData.anItem
import static io.wkrzywiec.fooddelivery.ordering.domain.OrderTestData.anOrder
import static io.wkrzywiec.fooddelivery.ordering.domain.OrderingFacade.ORDERS_CHANNEL

@Subject(OrderingFacade)
@Title("Specification for ordering process")
class OrderingFacadeSpec extends Specification {

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
        verifyEventInStore(
                order.orderCreated(),
                1
        )

        and: "OrderCreated event is published on 'orders' channel"
        verifyIntegrationEvent(order.getId(), "OrderCreated")
    }

    def "Cancel an order"() {
        given:
        var order = anOrder()
        eventStore.store(order.orderCreatedEntity(testClock))

        and:
        var cancellationReason = "Not hungry anymore"
        var cancelOrder = new CancelOrder(order.id, 2, cancellationReason)

        when:
        facade.handle(cancelOrder)

        then: "Event is saved in a store"
        verifyEventInStore(
                new OrderingEvent.OrderCanceled(order.getId(), 1, "Not hungry anymore"),
                2
        )

        and: "OrderCancelled event is published on 'orders' channel"
        verifyIntegrationEvent(order.getId(), "OrderCancelled")
    }

    def "Set order to IN_PROGRESS"() {
        given:
        var order = anOrder()
        eventStore.store(order.orderCreatedEntity(testClock))

        and:
        var foodInPreparation = new FoodInPreparation(order.id, 2)

        when:
        facade.handle(foodInPreparation)

        then: "Event is saved in a store"
        verifyEventInStore(
                new OrderingEvent.OrderInProgress(order.getId(), 1),
                2
        )

        and: "OrderInProgress event is published on 'orders' channel"
        verifyIntegrationEvent(order.getId(), "OrderInProgress")
    }

    def "Add tip to an order"() {
        given:
        double itemCost = 10
        double deliveryCharge = 5

        var order = anOrder()
                .withItems(anItem().withPricePerItem(itemCost))
                .withDeliveryCharge(deliveryCharge)
        eventStore.store(order.orderCreatedEntity(testClock))

        and:
        double tip = 20
        var addTip = new AddTip(order.id, 2, new BigDecimal(tip))

        when:
        facade.handle(addTip)

        then: "Tip was added"
        double total = itemCost + deliveryCharge + tip
        verifyEventInStore(
                new OrderingEvent.TipAddedToOrder(order.getId(), 1, tip, total),
                2
        )

        and: "TipAddedToOrder event is published on 'orders' channel"
        verifyIntegrationEvent(order.getId(), "TipAddedToOrder")
    }

    def "Complete an order"() {
        given:
        var order = anOrder()
        eventStore.store(order.orderCreatedEntity(testClock))
        eventStore.store(eventEntity(new OrderingEvent.OrderInProgress(order.getId(), 1)))

        and:
        var foodDelivered = new FoodDelivered(order.id, 2)

        when:
        facade.handle(foodDelivered)

        then: "Order is completed"
        verifyEventInStore(
                new OrderingEvent.OrderCompleted(order.getId(), 2),
                3
        )

        and: "OrderCompleted event is published on 'orders' channel"
        verifyIntegrationEvent(order.getId(), "OrderCompleted")
    }

    private void verifyEventInStore(OrderingEvent expectedDomainEvent, int expectedStreamSize) {
        def expectedEvent = eventEntity(expectedDomainEvent)
        def storedEvents = eventStore.fetchEvents(ORDERS_CHANNEL, expectedDomainEvent.streamId())
        assert storedEvents.size() == expectedStreamSize
        def actualEvent = storedEvents.last
        assert eventsAreEqualIgnoringId(expectedEvent, actualEvent)
    }

    private EventEntity eventEntity(OrderingEvent orderingEvent) {
        return newEventEntity(orderingEvent, ORDERS_CHANNEL, testClock)
    }

    private static boolean eventsAreEqualIgnoringId(EventEntity expected, EventEntity actual) {
        expected.properties.findAll { it.key != "id" } == actual.properties.findAll { it.key != "id" }
    }

    private void verifyIntegrationEvent(String orderId, String eventType) {
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) { event ->
            verifyEventHeader(event, orderId, eventType)
        }
    }

    private void verifyEventHeader(IntegrationMessage event, String orderId, String eventType) {
        def header = event.header()
        header.id() != null
        header.channel() == ORDERS_CHANNEL
        header.type() == eventType
        header.streamId() == orderId
        header.createdAt() == testClock.instant()
    }
}
