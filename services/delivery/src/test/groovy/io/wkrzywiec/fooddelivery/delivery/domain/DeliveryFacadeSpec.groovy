package io.wkrzywiec.fooddelivery.delivery.domain


import io.wkrzywiec.fooddelivery.commons.model.AssignDeliveryMan
import io.wkrzywiec.fooddelivery.commons.infra.store.inmemory.InMemoryEventStore
import io.wkrzywiec.fooddelivery.delivery.domain.incoming.Item
import io.wkrzywiec.fooddelivery.commons.model.DeliverFood
import io.wkrzywiec.fooddelivery.commons.model.FoodReady
import io.wkrzywiec.fooddelivery.delivery.domain.incoming.OrderCanceled
import io.wkrzywiec.fooddelivery.delivery.domain.incoming.OrderCreated
import io.wkrzywiec.fooddelivery.commons.model.PickUpFood
import io.wkrzywiec.fooddelivery.commons.model.PrepareFood
import io.wkrzywiec.fooddelivery.commons.model.UnAssignDeliveryMan
import io.wkrzywiec.fooddelivery.delivery.domain.incoming.TipAddedToOrder
import io.wkrzywiec.fooddelivery.delivery.domain.outgoing.DeliveryCanceled
import io.wkrzywiec.fooddelivery.delivery.domain.outgoing.DeliveryCreated
import io.wkrzywiec.fooddelivery.delivery.domain.outgoing.DeliveryManAssigned
import io.wkrzywiec.fooddelivery.delivery.domain.outgoing.DeliveryManUnAssigned
import io.wkrzywiec.fooddelivery.delivery.domain.outgoing.FoodDelivered
import io.wkrzywiec.fooddelivery.delivery.domain.outgoing.FoodInPreparation
import io.wkrzywiec.fooddelivery.delivery.domain.outgoing.FoodWasPickedUp
import io.wkrzywiec.fooddelivery.delivery.domain.outgoing.FoodIsReady
import io.wkrzywiec.fooddelivery.commons.infra.messaging.FakeMessagePublisher
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Message
import io.wkrzywiec.fooddelivery.delivery.domain.outgoing.TipAddedToDelivery
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

import static io.wkrzywiec.fooddelivery.commons.infra.messaging.Message.firstMessage

@Subject(DeliveryFacade)
@Title("Specification for delivery process")
class DeliveryFacadeSpec extends Specification {

    private final String ORDERS_CHANNEL = "orders"

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
        def expectedEvent = delivery.deliveryCreated()
        def storedEvents = eventStore.getEventsForOrder(delivery.getOrderId())
        storedEvents.size() == 1
        storedEvents[0].body() == expectedEvent

        and:
        Delivery.from(storedEvents).getStatus() == DeliveryStatus.CREATED

        and: "DeliveryCreated event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->

            verifyEventHeader(event, delivery.orderId, "DeliveryCreated")

            def body = event.body() as DeliveryCreated
            body == expectedEvent
        }
    }

    def "Add tip to delivery"() {
        given:
        var delivery = DeliveryTestData.aDelivery()
        eventStore.store(firstMessage("orders", testClock, delivery.deliveryCreated()))

        and:
        var tipAddedToOrder = new TipAddedToOrder(delivery.orderId, 2, BigDecimal.valueOf(5.55), BigDecimal.valueOf(50))

        when:
        facade.handle(tipAddedToOrder)

        then: "Event is saved in a store"
        def expectedEvent = new TipAddedToDelivery(delivery.getOrderId(), 2, BigDecimal.valueOf(5.55), BigDecimal.valueOf(50))
        def storedEvents = eventStore.getEventsForOrder(delivery.getOrderId())
        storedEvents.size() == 2
        storedEvents[1].body() == expectedEvent

        and: "TipAddedToDelivery event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->

            verifyEventHeader(event, delivery.orderId, "TipAddedToDelivery")

            def body = event.body() as TipAddedToDelivery
            body == expectedEvent
        }
    }

    def "Cancel a delivery"() {
        given:
        var delivery = DeliveryTestData.aDelivery()
        eventStore.store(firstMessage("orders", testClock, delivery.deliveryCreated()))

        and:
        var cancellationReason = "Not hungry anymore"
        var orderCanceled = new OrderCanceled(delivery.orderId, 2, cancellationReason)

        when:
        facade.handle(orderCanceled)

        then: "Event is saved in a store"
        def expectedEvent = new DeliveryCanceled(delivery.getOrderId(), 2, "Not hungry anymore")
        def storedEvents = eventStore.getEventsForOrder(delivery.getOrderId())
        storedEvents.size() == 2
        storedEvents[1].body() == expectedEvent

        and:
        Delivery.from(storedEvents).getStatus() == DeliveryStatus.CANCELED

        and: "DeliveryCancelled event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->

            verifyEventHeader(event, delivery.orderId, "DeliveryCanceled")

            def body = event.body() as DeliveryCanceled
            body == expectedEvent
        }
    }

    def "Food in preparation"() {
        given:
        var delivery = DeliveryTestData.aDelivery()
        eventStore.store(firstMessage("orders", testClock, delivery.deliveryCreated()))

        and:
        var prepareFood = new PrepareFood(delivery.orderId, 2)

        when:
        facade.handle(prepareFood)

        then: "Event is saved in a store"
        def expectedEvent = new FoodInPreparation(delivery.getOrderId(), 3)
        def storedEvents = eventStore.getEventsForOrder(delivery.getOrderId())
        storedEvents.size() == 2
        storedEvents[1].body() == expectedEvent

        and:
        Delivery.from(storedEvents).getStatus() == DeliveryStatus.FOOD_IN_PREPARATION

        and: "FoodInPreparation event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->

            verifyEventHeader(event, delivery.orderId, "FoodInPreparation")

            def body = event.body() as FoodInPreparation
            body == expectedEvent
        }
    }

    def "Assign delivery man to delivery"() {
        given:
        var delivery = DeliveryTestData.aDelivery().withDeliveryManId(null)
        eventStore.store(firstMessage("orders", testClock, delivery.deliveryCreated()))

        and:
        var deliveryManId = "any-delivery-man-orderId"
        var assignDeliveryMan = new AssignDeliveryMan(delivery.orderId, 2, deliveryManId)

        when:
        facade.handle(assignDeliveryMan)

        then: "Event is saved in a store"
        def expectedEvent = new DeliveryManAssigned(delivery.getOrderId(), 3, deliveryManId)
        def storedEvents = eventStore.getEventsForOrder(delivery.getOrderId())
        storedEvents.size() == 2
        storedEvents[1].body() == expectedEvent

        and: "DeliveryManAssigned event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->

            verifyEventHeader(event, delivery.orderId, "DeliveryManAssigned")

            def body = event.body() as DeliveryManAssigned
            body == expectedEvent
        }
    }

    def "Un assign delivery man from delivery"() {
        given:
        var deliveryManId = "any-delivery-man-orderId"
        var delivery = DeliveryTestData.aDelivery()
        eventStore.store(firstMessage("orders", testClock, delivery.deliveryCreated()))
        eventStore.store(firstMessage("orders", testClock, new DeliveryManAssigned(delivery.getOrderId(), 2, deliveryManId)))

        and:
        var assignDeliveryMan = new UnAssignDeliveryMan(delivery.orderId, 2)

        when:
        facade.handle(assignDeliveryMan)

        then: "Event is saved in a store"
        def expectedEvent = new DeliveryManUnAssigned(delivery.getOrderId(), 3, deliveryManId)
        def storedEvents = eventStore.getEventsForOrder(delivery.getOrderId())
        storedEvents.size() == 3
        storedEvents[2].body() == expectedEvent


        and: "DeliveryManUnAssigned event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->

            verifyEventHeader(event, delivery.orderId, "DeliveryManUnAssigned")

            def body = event.body() as DeliveryManUnAssigned
            body == expectedEvent
        }
    }

    def "Food is ready"() {
        given:
        var delivery = DeliveryTestData.aDelivery()
        eventStore.store(firstMessage("orders", testClock, delivery.deliveryCreated()))
        eventStore.store(firstMessage("orders", testClock, new FoodInPreparation(delivery.getOrderId(), 2)))

        and:
        var foodReady = new FoodReady(delivery.orderId, 2)

        when:
        facade.handle(foodReady)

        then: "Event is saved in a store"
        def expectedEvent = new FoodIsReady(delivery.getOrderId(), 3)
        def storedEvents = eventStore.getEventsForOrder(delivery.getOrderId())
        storedEvents.size() == 3
        storedEvents[2].body() == expectedEvent

        and: "FoodIsRead event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->

            verifyEventHeader(event, delivery.orderId, "FoodIsRead")

            def body = event.body() as FoodIsReady
            body == expectedEvent
        }
    }

    def "Food is picked up"() {
        given:
        var delivery = DeliveryTestData.aDelivery()
        eventStore.store(firstMessage("orders", testClock, delivery.deliveryCreated()))
        eventStore.store(firstMessage("orders", testClock, new FoodInPreparation(delivery.getOrderId(), 2)))
        eventStore.store(firstMessage("orders", testClock, new FoodIsReady(delivery.getOrderId(), 3)))

        and:
        var pickUpFood = new PickUpFood(delivery.orderId, 3)

        when:
        facade.handle(pickUpFood)

        then: "Event is saved in a store"
        def expectedEvent = new FoodWasPickedUp(delivery.getOrderId(), 4)
        def storedEvents = eventStore.getEventsForOrder(delivery.getOrderId())
        storedEvents.size() == 4
        storedEvents[3].body() == expectedEvent

        and: "FoodIsPickedUp event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->

            verifyEventHeader(event, delivery.orderId, "FoodWasPickedUp")

            def body = event.body() as FoodWasPickedUp
            body == expectedEvent
        }
    }

    def "Food is delivered"() {
        given:
        var delivery = DeliveryTestData.aDelivery()
                .withStatus(DeliveryStatus.FOOD_PICKED)
        eventStore.store(firstMessage("orders", testClock, delivery.deliveryCreated()))
        eventStore.store(firstMessage("orders", testClock, new FoodInPreparation(delivery.getOrderId(), 2)))
        eventStore.store(firstMessage("orders", testClock, new FoodIsReady(delivery.getOrderId(), 3)))
        eventStore.store(firstMessage("orders", testClock, new FoodWasPickedUp(delivery.getOrderId(), 4)))

        and:
        var deliverFood = new DeliverFood(delivery.orderId, 4)

        when:
        facade.handle(deliverFood)

        then: "Event is saved in a store"
        def expectedEvent = new FoodDelivered(delivery.getOrderId(), 5)
        def storedEvents = eventStore.getEventsForOrder(delivery.getOrderId())
        storedEvents.size() == 5
        storedEvents[4].body() == expectedEvent

        and: "FoodDelivered event is published on 'orders' channel"
        with(publisher.messages.get(ORDERS_CHANNEL).get(0)) {event ->

            verifyEventHeader(event, delivery.orderId, "FoodDelivered")

            def body = event.body() as FoodDelivered
            body == expectedEvent
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
