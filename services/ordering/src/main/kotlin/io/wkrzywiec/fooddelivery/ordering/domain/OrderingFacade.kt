package io.wkrzywiec.fooddelivery.ordering.domain

import io.vavr.control.Try
import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody
import io.wkrzywiec.fooddelivery.commons.incoming.AddTip
import io.wkrzywiec.fooddelivery.commons.incoming.CancelOrder
import io.wkrzywiec.fooddelivery.commons.incoming.CreateOrder
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Header
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Message
import io.wkrzywiec.fooddelivery.commons.infra.messaging.MessagePublisher
import io.wkrzywiec.fooddelivery.commons.infra.repository.EventStore
import io.wkrzywiec.fooddelivery.ordering.domain.Order.Companion.from
import io.wkrzywiec.fooddelivery.ordering.domain.incoming.FoodDelivered
import io.wkrzywiec.fooddelivery.ordering.domain.incoming.FoodInPreparation
import io.wkrzywiec.fooddelivery.ordering.domain.outgoing.*
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
        OrderingFacade.log.info("Creating a new order: {}", createOrder)
        val newOrder = from(createOrder)
        val orderCreated = OrderCreated(
            newOrder.getId(),
            newOrder.getCustomerId(),
            newOrder.getRestaurantId(),
            newOrder.getAddress(),
            createOrder.items,
            newOrder.getDeliveryCharge(),
            newOrder.getTotal()
        )

        val event = resultingEvent(
            newOrder.getId(),
            orderCreated
        )

        eventStore!!.store(event)
        publisher!!.send(event)
        OrderingFacade.log.info("New order with an id: '{}' was created", newOrder.getId())
    }

    fun handle(cancelOrder: CancelOrder) {
        OrderingFacade.log.info("Cancelling an order: {}", cancelOrder.orderId)

        val storedEvents = eventStore!!.getEventsForOrder(cancelOrder.orderId)
        if (storedEvents.size == 0) {
            throw OrderingException(
                String.format(
                    "Failed to cancel an %s order. There is no such order with provided id.",
                    cancelOrder.orderId
                )
            )
        }
        val order = from(storedEvents)!!

        Try.run { order.cancelOrder(cancelOrder.reason) }
            .onSuccess { v: Void? ->
                publishSuccessEvent(
                    order.getId(),
                    OrderCanceled(cancelOrder.orderId, cancelOrder.reason)
                )
            }
            .onFailure { ex: Throwable -> publishingFailureEvent(order.getId(), "Failed to cancel an order.", ex) }
            .andFinally { OrderingFacade.log.info("Cancellation of an order '{}' has been completed", order.getId()) }
    }

    fun handle(foodInPreparation: FoodInPreparation) {
        OrderingFacade.log.info("Setting '{}' order to IN_PROGRESS state", foodInPreparation.orderId)

        val storedEvents = eventStore!!.getEventsForOrder(foodInPreparation.orderId)
        if (storedEvents.size == 0) {
            throw OrderingException(
                String.format(
                    "Failed to set an '%s' order to IN_PROGRESS state. There is no such order with provided id.",
                    foodInPreparation.orderId
                )
            )
        }
        val order = from(storedEvents)!!

        Try.run { order.setInProgress() }
            .onSuccess { v: Void? -> publishSuccessEvent(order.getId(), OrderInProgress(foodInPreparation.orderId)) }
            .onFailure { ex: Throwable ->
                publishingFailureEvent(
                    order.getId(),
                    "Failed to set an order to IN_PROGRESS state.",
                    ex
                )
            }
            .andFinally {
                OrderingFacade.log.info(
                    "Setting an '{}' order to IN_PROGRESS state has been completed",
                    foodInPreparation.orderId
                )
            }
    }

    fun handle(addTip: AddTip) {
        OrderingFacade.log.info("Adding {} tip to '{}' order.", addTip.tip, addTip.orderId)

        val storedEvents = eventStore!!.getEventsForOrder(addTip.orderId)
        if (storedEvents.size == 0) {
            throw OrderingException(
                String.format(
                    "Failed add tip an '%s' order. There is no such order with provided id.",
                    addTip.orderId
                )
            )
        }
        val order = from(storedEvents)!!

        Try.run { order.addTip(addTip.tip) }
            .onSuccess { v: Void? ->
                publishSuccessEvent(
                    order.getId(),
                    TipAddedToOrder(order.getId(), order.getTip(), order.getTotal())
                )
            }
            .onFailure { ex: Throwable -> publishingFailureEvent(order.getId(), "Failed to add tip to an order.", ex) }
            .andFinally { OrderingFacade.log.info("Adding a tip to '{}' order has been completed", addTip.orderId) }
    }

    fun handle(foodDelivered: FoodDelivered) {
        OrderingFacade.log.info("Setting '{}' order to COMPLETED state", foodDelivered.orderId)

        val storedEvents = eventStore!!.getEventsForOrder(foodDelivered.orderId)
        if (storedEvents.size == 0) {
            throw OrderingException(
                String.format(
                    "Failed to complete an '%s' order. There is no such order with provided id.",
                    foodDelivered.orderId
                )
            )
        }
        val order = from(storedEvents)!!

        Try.run { order.complete() }
            .onSuccess { v: Void? -> publishSuccessEvent(order.getId(), OrderCompleted(foodDelivered.orderId)) }
            .onFailure { ex: Throwable -> publishingFailureEvent(order.getId(), "Failed to complete an order.", ex) }
            .andFinally {
                OrderingFacade.log.info(
                    "Setting an '{}' order to COMPLETED state has been completed",
                    foodDelivered.orderId
                )
            }
    }

    private fun publishSuccessEvent(orderId: String, eventObject: DomainMessageBody) {
        OrderingFacade.log.info("Publishing success event: {}", eventObject)
        val event = resultingEvent(orderId, eventObject)
        eventStore!!.store(event)
        publisher!!.send(event)
    }

    private fun publishingFailureEvent(id: String, message: String, ex: Throwable) {
        OrderingFacade.log.error("$message Publishing OrderProcessingError event", ex)
        val event = resultingEvent(id, OrderProcessingError(id, message, ex.localizedMessage))
        publisher!!.send(event)
    }

    private fun resultingEvent(orderId: String, eventBody: DomainMessageBody): Message {
        return Message(eventHeader(orderId, eventBody.javaClass.simpleName), eventBody)
    }

    private fun eventHeader(orderId: String, type: String): Header {
        return Header(UUID.randomUUID().toString(), ORDERS_CHANNEL, type, orderId, clock!!.instant())
    }

    companion object {
        private const val ORDERS_CHANNEL = "orders"
    }
}
