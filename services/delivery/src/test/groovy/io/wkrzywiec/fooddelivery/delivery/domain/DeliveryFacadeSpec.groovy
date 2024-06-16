package io.wkrzywiec.fooddelivery.delivery.domain

import io.wkrzywiec.fooddelivery.commons.infra.messaging.FakeMessagePublisher
import io.wkrzywiec.fooddelivery.commons.infra.messaging.IntegrationMessage
import io.wkrzywiec.fooddelivery.commons.infra.store.EventEntity
import io.wkrzywiec.fooddelivery.commons.infra.store.inmemory.InMemoryEventStore
import io.wkrzywiec.fooddelivery.commons.model.*
import io.wkrzywiec.fooddelivery.delivery.domain.incoming.Item
import io.wkrzywiec.fooddelivery.delivery.domain.incoming.OrderCanceled
import io.wkrzywiec.fooddelivery.delivery.domain.incoming.OrderCreated
import io.wkrzywiec.fooddelivery.delivery.domain.incoming.TipAddedToOrder
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

import static io.wkrzywiec.fooddelivery.commons.infra.store.EventEntity.newEventEntity
import static io.wkrzywiec.fooddelivery.delivery.domain.DeliveryFacade.ORDERS_CHANNEL

@Subject(DeliveryFacade)
@Title("Specification for delivery process")
class DeliveryFacadeSpec extends Specification {

    DeliveryFacade facade
    InMemoryEventStore eventStore
    FakeMessagePublisher publisher

    var testTime = Instant.parse("2022-08-08T05:30:24.00Z")
    Clock testClock = Clock.fixed(testTime, ZoneOffset.UTC)

    def setup() {
        eventStore = new InMemoryEventStore()
        publisher = new FakeMessagePublisher()
        facade = new DeliveryFacade(eventStore, publisher, testClock)
    }

    def "Create a delivery"() {
        given:
        var delivery = DeliveryTestData.aDelivery()
                .withItems(
                        ItemTestData.anItem().withName("Pizza").withPricePerItem(2.5),
                        ItemTestData.anItem().withName("Spaghetti").withPricePerItem(3.0)
                )

        var orderCreated = new OrderCreated(
                delivery.getOrderId(), 1, delivery.getCustomerId(),
                delivery.getFarmId(), delivery.getAddress(),
                delivery.getItems().stream().map(i -> new Item(i.name, i.amount, i.pricePerItem)).toList(),
                delivery.getDeliveryCharge(), delivery.getTotal())

        when:
        facade.handle(orderCreated)

        then: "Event is saved in a store"
        def expectedEvent = delivery.deliveryCreatedEvent(testClock)
        def storedEvents = eventStore.fetchEvents(ORDERS_CHANNEL, delivery.getOrderId())
        storedEvents.size() == 1
        def actualEvent = storedEvents[0]
        eventsAreEqualIgnoringId(expectedEvent, actualEvent)

        and: "DeliveryCreated event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) { event ->
            verifyEventHeader(event, delivery.orderId, "DeliveryCreated")
        }
    }

    def "Add tip to delivery"() {
        given:
        var delivery = DeliveryTestData.aDelivery()
        eventStore.store(delivery.deliveryCreatedEvent(testClock))

        and:
        var tipAddedToOrder = new TipAddedToOrder(delivery.orderId, 2, BigDecimal.valueOf(5.55), BigDecimal.valueOf(50))

        when:
        facade.handle(tipAddedToOrder)

        then: "Event is saved in a store"
        //todo extract to method
        def expectedEvent = eventEntity(new DeliveryEvent.TipAddedToDelivery(delivery.getOrderId(), 1, BigDecimal.valueOf(5.55), BigDecimal.valueOf(50)))
        def storedEvents = eventStore.fetchEvents(ORDERS_CHANNEL, delivery.getOrderId())
        storedEvents.size() == 2
        def actualEvent = storedEvents[1]
        eventsAreEqualIgnoringId(expectedEvent, actualEvent)

        and: "TipAddedToDelivery event is published on 'orders' channel"
        //todo extract to method
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->
            verifyEventHeader(event, delivery.orderId, "TipAddedToDelivery")
        }
    }

    def "Cancel a delivery"() {
        given:
        var delivery = DeliveryTestData.aDelivery()
        eventStore.store(delivery.deliveryCreatedEvent(testClock))

        and:
        var cancellationReason = "Not hungry anymore"
        var orderCanceled = new OrderCanceled(delivery.orderId, 2, cancellationReason)

        when:
        facade.handle(orderCanceled)

        then: "Event is saved in a store"
        def expectedEvent = eventEntity(new DeliveryEvent.DeliveryCanceled(delivery.getOrderId(), 1, "Not hungry anymore"))
        def storedEvents = eventStore.fetchEvents(ORDERS_CHANNEL, delivery.getOrderId())
        storedEvents.size() == 2
        def actualEvent = storedEvents[1]
        eventsAreEqualIgnoringId(expectedEvent, actualEvent)

        and: "DeliveryCancelled event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->
            verifyEventHeader(event, delivery.orderId, "DeliveryCanceled")
        }
    }

    def "Food in preparation"() {
        given:
        var delivery = DeliveryTestData.aDelivery()
        eventStore.store(delivery.deliveryCreatedEvent(testClock))

        and:
        var prepareFood = new PrepareFood(delivery.orderId, 2)

        when:
        facade.handle(prepareFood)

        then: "Event is saved in a store"
        def expectedEvent = eventEntity(new DeliveryEvent.FoodInPreparation(delivery.getOrderId(), 1))
        def storedEvents = eventStore.fetchEvents(ORDERS_CHANNEL, delivery.getOrderId())
        storedEvents.size() == 2
        def actualEvent = storedEvents[1]
        eventsAreEqualIgnoringId(expectedEvent, actualEvent)

        and: "FoodInPreparation event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->
            verifyEventHeader(event, delivery.orderId, "FoodInPreparation")
        }
    }

    def "Assign delivery man to delivery"() {
        given:
        var delivery = DeliveryTestData.aDelivery().withDeliveryManId(null)
        eventStore.store(delivery.deliveryCreatedEvent(testClock))

        and:
        var deliveryManId = "any-delivery-man-orderId"
        var assignDeliveryMan = new AssignDeliveryMan(delivery.orderId, 2, deliveryManId)

        when:
        facade.handle(assignDeliveryMan)

        then: "Event is saved in a store"
        def expectedEvent = eventEntity(new DeliveryEvent.DeliveryManAssigned(delivery.getOrderId(), 1, deliveryManId))
        def storedEvents = eventStore.fetchEvents(ORDERS_CHANNEL, delivery.getOrderId())
        storedEvents.size() == 2
        def actualEvent = storedEvents[1]
        eventsAreEqualIgnoringId(expectedEvent, actualEvent)

        and: "DeliveryManAssigned event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->
            verifyEventHeader(event, delivery.orderId, "DeliveryManAssigned")
        }
    }

    def "Un assign delivery man from delivery"() {
        given:
        var deliveryManId = "any-delivery-man-orderId"
        var delivery = DeliveryTestData.aDelivery()
        eventStore.store(delivery.deliveryCreatedEvent(testClock))
        eventStore.store(eventEntity(new DeliveryEvent.DeliveryManAssigned(delivery.getOrderId(), 1, deliveryManId)))

        and:
        var assignDeliveryMan = new UnAssignDeliveryMan(delivery.orderId, 2)

        when:
        facade.handle(assignDeliveryMan)

        then: "Event is saved in a store"
        def expectedEvent = eventEntity(new DeliveryEvent.DeliveryManUnAssigned(delivery.getOrderId(), 2, deliveryManId))
        def storedEvents = eventStore.fetchEvents(ORDERS_CHANNEL, delivery.getOrderId())
        storedEvents.size() == 3
        def actualEvent = storedEvents[2]
        eventsAreEqualIgnoringId(expectedEvent, actualEvent)


        and: "DeliveryManUnAssigned event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->
            verifyEventHeader(event, delivery.orderId, "DeliveryManUnAssigned")
        }
    }

    def "Food is ready"() {
        given:
        var delivery = DeliveryTestData.aDelivery()
        eventStore.store(delivery.deliveryCreatedEvent(testClock))
        eventStore.store(eventEntity(new DeliveryEvent.FoodInPreparation(delivery.getOrderId(), 1)))

        and:
        var foodReady = new FoodReady(delivery.orderId, 2)

        when:
        facade.handle(foodReady)

        then: "Event is saved in a store"
        def expectedEvent = eventEntity(new DeliveryEvent.FoodIsReady(delivery.getOrderId(), 2))
        def storedEvents = eventStore.fetchEvents(ORDERS_CHANNEL, delivery.getOrderId())
        storedEvents.size() == 3
        def actualEvent = storedEvents[2]
        eventsAreEqualIgnoringId(expectedEvent, actualEvent)

        and: "FoodIsRead event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->
            verifyEventHeader(event, delivery.orderId, "FoodIsRead")
        }
    }

    def "Food is picked up"() {
        given:
        var delivery = DeliveryTestData.aDelivery()
        eventStore.store(delivery.deliveryCreatedEvent(testClock))
        eventStore.store(eventEntity(new DeliveryEvent.FoodInPreparation(delivery.getOrderId(), 1)))
        eventStore.store(eventEntity(new DeliveryEvent.FoodIsReady(delivery.getOrderId(), 2)))

        and:
        var pickUpFood = new PickUpFood(delivery.orderId, 3)

        when:
        facade.handle(pickUpFood)

        then: "Event is saved in a store"
        def expectedEvent = eventEntity(new DeliveryEvent.FoodWasPickedUp(delivery.getOrderId(), 3))
        def storedEvents = eventStore.fetchEvents(ORDERS_CHANNEL, delivery.getOrderId())
        storedEvents.size() == 4
        def actualEvent = storedEvents[3]
        eventsAreEqualIgnoringId(expectedEvent, actualEvent)

        and: "FoodIsPickedUp event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->
            verifyEventHeader(event, delivery.orderId, "FoodWasPickedUp")
        }
    }

    def "Food is delivered"() {
        given:
        var delivery = DeliveryTestData.aDelivery()
                .withStatus(DeliveryStatus.FOOD_PICKED)
        eventStore.store(delivery.deliveryCreatedEvent(testClock))
        eventStore.store(eventEntity(new DeliveryEvent.FoodInPreparation(delivery.getOrderId(), 1)))
        eventStore.store(eventEntity(new DeliveryEvent.FoodIsReady(delivery.getOrderId(), 2)))
        eventStore.store(eventEntity(new DeliveryEvent.FoodWasPickedUp(delivery.getOrderId(), 3)))

        and:
        var deliverFood = new DeliverFood(delivery.orderId, 4)

        when:
        facade.handle(deliverFood)

        then: "Event is saved in a store"
        def expectedEvent = eventEntity(new DeliveryEvent.FoodDelivered(delivery.getOrderId(), 4))
        def storedEvents = eventStore.fetchEvents(ORDERS_CHANNEL, delivery.getOrderId())
        storedEvents.size() == 5
        def actualEvent = storedEvents[4]
        eventsAreEqualIgnoringId(expectedEvent, actualEvent)

        and: "FoodDelivered event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->
            verifyEventHeader(event, delivery.orderId, "FoodDelivered")
        }
    }

    private EventEntity eventEntity(DeliveryEvent deliveryEvent) {
        return newEventEntity(deliveryEvent, ORDERS_CHANNEL, testClock)
    }

    private static boolean eventsAreEqualIgnoringId(EventEntity expected, EventEntity actual) {
        expected.properties.findAll { it.key != "id" } == actual.properties.findAll { it.key != "id" }
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
