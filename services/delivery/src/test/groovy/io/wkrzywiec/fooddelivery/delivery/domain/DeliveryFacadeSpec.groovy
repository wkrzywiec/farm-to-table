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
import static io.wkrzywiec.fooddelivery.delivery.domain.DeliveryFacade.DELIVERY_CHANNEL

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
        verifyEventInStore(
                delivery.deliveryCreated(),
                1
        )

        and: "DeliveryCreated event is published on 'orders' channel"
        verifyIntegrationEvent(delivery.getOrderId(), "DeliveryCreated")
    }

    def "Add tip to delivery"() {
        given:
        var delivery = DeliveryTestData.aDelivery()
        eventStore.store(delivery.deliveryCreatedEntity(testClock))

        and:
        var tipAddedToOrder = new TipAddedToOrder(delivery.orderId, 2, BigDecimal.valueOf(5.55), BigDecimal.valueOf(50))

        when:
        facade.handle(tipAddedToOrder)

        then: "Event is saved in a store"
        verifyEventInStore(
                new DeliveryEvent.TipAddedToDelivery(delivery.getOrderId(), 1, BigDecimal.valueOf(5.55), BigDecimal.valueOf(50)),
                2
        )

        and: "TipAddedToDelivery event is published on 'orders' channel"
        verifyIntegrationEvent(delivery.getOrderId(), "TipAddedToDelivery")
    }

    def "Cancel a delivery"() {
        given:
        var delivery = DeliveryTestData.aDelivery()
        eventStore.store(delivery.deliveryCreatedEntity(testClock))

        and:
        var cancellationReason = "Not hungry anymore"
        var orderCanceled = new OrderCanceled(delivery.orderId, 2, cancellationReason)

        when:
        facade.handle(orderCanceled)

        then: "Event is saved in a store"
        verifyEventInStore(
                new DeliveryEvent.DeliveryCanceled(delivery.getOrderId(), 1, "Not hungry anymore"),
                2
        )

        and: "DeliveryCancelled event is published on 'orders' channel"
        verifyIntegrationEvent(delivery.getOrderId(), "DeliveryCanceled")
    }

    def "Food in preparation"() {
        given:
        var delivery = DeliveryTestData.aDelivery()
        eventStore.store(delivery.deliveryCreatedEntity(testClock))

        and:
        var prepareFood = new PrepareFood(delivery.orderId, 2)

        when:
        facade.handle(prepareFood)

        then: "Event is saved in a store"
        verifyEventInStore(
                new DeliveryEvent.FoodInPreparation(delivery.getOrderId(), 1),
                2
        )

        and: "FoodInPreparation event is published on 'orders' channel"
        verifyIntegrationEvent(delivery.getOrderId(), "FoodInPreparation")
    }

    def "Assign delivery man to delivery"() {
        given:
        var delivery = DeliveryTestData.aDelivery().withDeliveryManId(null)
        eventStore.store(delivery.deliveryCreatedEntity(testClock))

        and:
        var deliveryManId = "any-delivery-man-orderId"
        var assignDeliveryMan = new AssignDeliveryMan(delivery.orderId, 2, deliveryManId)

        when:
        facade.handle(assignDeliveryMan)

        then: "Event is saved in a store"
        verifyEventInStore(
                new DeliveryEvent.DeliveryManAssigned(delivery.getOrderId(), 1, deliveryManId),
                2
        )

        and: "DeliveryManAssigned event is published on 'orders' channel"
        verifyIntegrationEvent(delivery.getOrderId(), "DeliveryManAssigned")
    }

    def "Un assign delivery man from delivery"() {
        given:
        var deliveryManId = "any-delivery-man-orderId"
        var delivery = DeliveryTestData.aDelivery()
        eventStore.store(delivery.deliveryCreatedEntity(testClock))
        eventStore.store(eventEntity(new DeliveryEvent.DeliveryManAssigned(delivery.getOrderId(), 1, deliveryManId)))

        and:
        var assignDeliveryMan = new UnAssignDeliveryMan(delivery.orderId, 2)

        when:
        facade.handle(assignDeliveryMan)

        then: "Event is saved in a store"
        verifyEventInStore(
                new DeliveryEvent.DeliveryManUnAssigned(delivery.getOrderId(), 2, deliveryManId),
                3
        )


        and: "DeliveryManUnAssigned event is published on 'orders' channel"
        verifyIntegrationEvent(delivery.getOrderId(), "DeliveryManUnAssigned")
    }

    def "Food is ready"() {
        given:
        var delivery = DeliveryTestData.aDelivery()
        eventStore.store(delivery.deliveryCreatedEntity(testClock))
        eventStore.store(eventEntity(new DeliveryEvent.FoodInPreparation(delivery.getOrderId(), 1)))

        and:
        var foodReady = new FoodReady(delivery.orderId, 2)

        when:
        facade.handle(foodReady)

        then: "Event is saved in a store"
        verifyEventInStore(
                new DeliveryEvent.FoodIsReady(delivery.getOrderId(), 2),
                3
        )

        and: "FoodIsRead event is published on 'orders' channel"
        verifyIntegrationEvent(delivery.getOrderId(), "FoodIsRead")
    }

    def "Food is picked up"() {
        given:
        var delivery = DeliveryTestData.aDelivery()
        eventStore.store(delivery.deliveryCreatedEntity(testClock))
        eventStore.store(eventEntity(new DeliveryEvent.FoodInPreparation(delivery.getOrderId(), 1)))
        eventStore.store(eventEntity(new DeliveryEvent.FoodIsReady(delivery.getOrderId(), 2)))

        and:
        var pickUpFood = new PickUpFood(delivery.orderId, 3)

        when:
        facade.handle(pickUpFood)

        then: "Event is saved in a store"
        verifyEventInStore(
                new DeliveryEvent.FoodWasPickedUp(delivery.getOrderId(), 3),
                4
        )

        and: "FoodIsPickedUp event is published on 'orders' channel"
        verifyIntegrationEvent(delivery.getOrderId(), "FoodWasPickedUp")
    }

    def "Food is delivered"() {
        given:
        var delivery = DeliveryTestData.aDelivery()
                .withStatus(DeliveryStatus.FOOD_PICKED)
        eventStore.store(delivery.deliveryCreatedEntity(testClock))
        eventStore.store(eventEntity(new DeliveryEvent.FoodInPreparation(delivery.getOrderId(), 1)))
        eventStore.store(eventEntity(new DeliveryEvent.FoodIsReady(delivery.getOrderId(), 2)))
        eventStore.store(eventEntity(new DeliveryEvent.FoodWasPickedUp(delivery.getOrderId(), 3)))

        and:
        var deliverFood = new DeliverFood(delivery.orderId, 4)

        when:
        facade.handle(deliverFood)

        then: "Event is saved in a store"
        verifyEventInStore(
                new DeliveryEvent.FoodDelivered(delivery.getOrderId(), 4),
                5
        )

        and: "FoodDelivered event is published on 'orders' channel"
        verifyIntegrationEvent(delivery.getOrderId(), "FoodDelivered")
    }

    private void verifyEventInStore(DeliveryEvent expectedDomainEvent, int expectedStreamSize) {
        def expectedEvent = eventEntity(expectedDomainEvent)
        def storedEvents = eventStore.loadEvents(DELIVERY_CHANNEL, expectedDomainEvent.streamId())
        assert storedEvents.size() == expectedStreamSize
        def actualEvent = storedEvents.last
        assert eventsAreEqualIgnoringId(expectedEvent, actualEvent)
    }

    private EventEntity eventEntity(DeliveryEvent deliveryEvent) {
        return newEventEntity(deliveryEvent, DELIVERY_CHANNEL, testClock)
    }

    private static boolean eventsAreEqualIgnoringId(EventEntity expected, EventEntity actual) {
        expected.properties.findAll { it.key != "id" } == actual.properties.findAll { it.key != "id" }
    }

    private void verifyIntegrationEvent(UUID orderId, String eventType) {
        with(publisher.messages.get(DELIVERY_CHANNEL).get(0)) { event ->
            verifyEventHeader(event, orderId, eventType)
        }
    }

    private void verifyEventHeader(IntegrationMessage event, UUID orderId, String eventType) {
        def header = event.header()
        header.id() != null
        header.channel() == DELIVERY_CHANNEL
        header.type() == eventType
        header.streamId() == orderId
        header.createdAt() == testClock.instant()
    }
}
