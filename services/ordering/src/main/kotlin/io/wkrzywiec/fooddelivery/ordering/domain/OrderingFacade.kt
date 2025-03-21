package io.wkrzywiec.fooddelivery.ordering.domain

import io.vavr.CheckedRunnable
import io.vavr.control.Try
import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Header
import io.wkrzywiec.fooddelivery.commons.infra.messaging.IntegrationMessage
import io.wkrzywiec.fooddelivery.commons.infra.messaging.MessagePublisher
import io.wkrzywiec.fooddelivery.commons.infra.store.DomainEvent
import io.wkrzywiec.fooddelivery.commons.infra.store.EventEntity
import io.wkrzywiec.fooddelivery.commons.infra.store.EventStore
import io.wkrzywiec.fooddelivery.commons.model.AddTip
import io.wkrzywiec.fooddelivery.commons.model.CancelOrder
import io.wkrzywiec.fooddelivery.commons.model.CreateOrder
import io.wkrzywiec.fooddelivery.ordering.domain.incoming.FoodDelivered
import io.wkrzywiec.fooddelivery.ordering.domain.incoming.FoodInPreparation
import io.wkrzywiec.fooddelivery.ordering.domain.outgoing.OrderProcessingError
import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import org.springframework.stereotype.Component
import java.time.Clock
import java.util.*

@RequiredArgsConstructor
@Slf4j
@Component
class OrderingFacade {
    private val eventStore: EventStore? = null
    private val publisher: MessagePublisher? = null
    private val clock: Clock? = null

    fun handle(createOrder: CreateOrder) {
        process(
            createOrder.orderId,
            { createOrder(createOrder) },
            "Failed to create an order."
        )
    }

    private fun createOrder(createOrder: CreateOrder) {
        OrderingFacade.log.info("Creating a new order: {}", createOrder)

        val newOrder = Order.from(createOrder)
        storeAndPublishEvents(newOrder)
        OrderingFacade.log.info("New order with an id: '{}' was created", newOrder.id)
    }

    fun handle(cancelOrder: CancelOrder) {
        process(
            cancelOrder.orderId,
            { cancelOrder(cancelOrder) },
            "Failed to cancel an order."
        )
    }

    private fun cancelOrder(cancelOrder: CancelOrder) {
        OrderingFacade.log.info("Cancelling an order: {}", cancelOrder.orderId)

        val order = findOrder(cancelOrder.orderId)
        order.cancelOrder(cancelOrder.reason)
        storeAndPublishEvents(order)

        OrderingFacade.log.info("Cancellation of an order '{}' has been completed", order.id)
    }

    fun handle(foodInPreparation: FoodInPreparation) {
        process(
            foodInPreparation.orderId,
            { foodInPreparation(foodInPreparation) },
            "Failed to set an order to IN_PROGRESS state."
        )
    }

    private fun foodInPreparation(foodInPreparation: FoodInPreparation) {
        OrderingFacade.log.info("Setting '{}' order to IN_PROGRESS state", foodInPreparation.orderId)

        val order = findOrder(foodInPreparation.orderId)
        order.setInProgress()
        storeAndPublishEvents(order)

        OrderingFacade.log.info(
            "Setting an '{}' order to IN_PROGRESS state has been completed",
            foodInPreparation.orderId
        )
    }

    fun handle(addTip: AddTip) {
        process(
            addTip.orderId,
            { addTip(addTip) },
            "Failed to add tip to an order."
        )
    }

    private fun addTip(addTip: AddTip) {
        OrderingFacade.log.info("Adding {} tip to '{}' order.", addTip.tip, addTip.orderId)

        val order = findOrder(addTip.orderId)
        order.addTip(addTip.tip)
        storeAndPublishEvents(order)

        OrderingFacade.log.info("Adding a tip to '{}' order has been completed", addTip.orderId)
    }

    fun handle(foodDelivered: FoodDelivered) {
        process(
            foodDelivered.orderId,
            { foodDelivered(foodDelivered) },
            "Failed to complete an order."
        )
    }

    private fun foodDelivered(foodDelivered: FoodDelivered) {
        OrderingFacade.log.info("Setting '{}' order to COMPLETED state", foodDelivered.orderId)

        val order = findOrder(foodDelivered.orderId)
        order.complete()
        storeAndPublishEvents(order)

        OrderingFacade.log.info("Setting an '{}' order to COMPLETED state has been completed", foodDelivered.orderId)
    }

    private fun process(streamId: UUID, runProcess: CheckedRunnable, failureMessage: String) {
        Try.run(runProcess)
            .onFailure { ex: Throwable -> publishingFailureEvent(streamId, failureMessage, ex) }
    }

    private fun publishingFailureEvent(id: UUID, message: String, ex: Throwable) {
        OrderingFacade.log.error("$message Publishing OrderProcessingError event", ex)
        val event = resultingEvent(id, OrderProcessingError(id, -1, message, ex.localizedMessage))
        publisher!!.send(event)
    }

    private fun resultingEvent(orderId: UUID, eventBody: IntegrationMessageBody): IntegrationMessage {
        return IntegrationMessage(eventHeader(orderId, eventBody.javaClass.simpleName, eventBody.version()), eventBody)
    }

    private fun eventHeader(orderId: UUID, type: String, version: Int): Header {
        return Header(UUID.randomUUID(), version, ORDERS_CHANNEL, type, orderId, clock!!.instant())
    }

    private fun findOrder(orderId: UUID): Order {
        val storedEvents = eventStore!!.loadEvents(ORDERS_CHANNEL, orderId)
        if (storedEvents.isEmpty()) {
            throw OrderingException(String.format("There is no order with an orderId '%s'.", orderId))
        }
        return Order.from(storedEvents.stream().map { eventEntity: EventEntity -> eventEntity.data() as OrderingEvent }
            .toList())
    }

    private fun storeAndPublishEvents(order: Order) {
        val eventEntities = storeUncommittedEvents(order)
        prepareAndPublishIntegrationEvents(eventEntities)
    }

    private fun storeUncommittedEvents(order: Order): List<EventEntity> {
        val domainEvents: List<DomainEvent> = order.uncommittedChanges()
        val eventEntities = EventEntity.newEventEntities(domainEvents, ORDERS_CHANNEL, clock)
        eventStore!!.store(eventEntities)
        return eventEntities
    }

    private fun prepareAndPublishIntegrationEvents(eventEntities: List<EventEntity>) {
        val integrationEvents = IntegrationMessage.integrationEvents(eventEntities, OrderingEventMapper.INSTANCE)
        publisher!!.send(integrationEvents)
    }

    companion object {
        const val ORDERS_CHANNEL: String = "orders"
    }
}
