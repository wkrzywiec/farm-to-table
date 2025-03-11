package io.wkrzywiec.fooddelivery.ordering.domain

import io.github.oshai.kotlinlogging.KotlinLogging
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
import org.springframework.stereotype.Component
import java.time.Clock
import java.util.*

private val logger = KotlinLogging.logger {}

@Component
class OrderingFacade {
    private val eventStore: EventStore? = null
    private val publisher: MessagePublisher? = null
    private val clock: Clock? = null

    companion object {
        private const val ORDERS_CHANNEL = "orders"
    }

    fun handle(createOrder: CreateOrder) {
        logger.info {"Creating a new order: $createOrder"}
        val newOrder = from(createOrder)
        val orderCreated = OrderCreated(
            id = newOrder.id,
            customerId = newOrder.customerId,
            restaurantId = newOrder.restaurantId,
            address = newOrder.address,
            items = createOrder.items,
            deliveryCharge = newOrder.deliveryCharge,
            total = newOrder.total
        )

        val event = resultingEvent(
            newOrder.id,
            orderCreated
        )

        eventStore!!.store(event)
        publisher!!.send(event)
        logger.info {"New order with an id: '${newOrder.id}' was created"}
    }

    fun handle(cancelOrder: CancelOrder) {
        logger.info { "Cancelling an order: ${cancelOrder.orderId}" }

        val storedEvents = eventStore!!.getEventsForOrder(cancelOrder.orderId)
        if (storedEvents.size == 0) {
            throw OrderingException(
                String.format(
                    "Failed to cancel an %s order. There is no such order with provided id.",
                    cancelOrder.orderId
                )
            )
        }
        val order = from(storedEvents)

        Try.run { order.cancelOrder(cancelOrder.reason) }
            .onSuccess { v: Void? ->
                publishSuccessEvent(
                    order.id,
                    OrderCanceled(cancelOrder.orderId, cancelOrder.reason)
                )
            }
            .onFailure { ex: Throwable -> publishingFailureEvent(order.id, "Failed to cancel an order.", ex) }
            .andFinally {
                logger.info { "Cancellation of an order '${order.id}' has been completed" }
            }
    }

    fun handle(foodInPreparation: FoodInPreparation) {
        logger.info {"Setting '${foodInPreparation.orderId}' order to IN_PROGRESS state" }

        val storedEvents = eventStore!!.getEventsForOrder(foodInPreparation.orderId)
        if (storedEvents.size == 0) {
            throw OrderingException(
                String.format(
                    "Failed to set an '%s' order to IN_PROGRESS state. There is no such order with provided id.",
                    foodInPreparation.orderId
                )
            )
        }
        val order = from(storedEvents)

        Try.run ({ order.setInProgress() })
            .onSuccess { v: Void? -> publishSuccessEvent(order.id, OrderInProgress(foodInPreparation.orderId)) }
            .onFailure { ex: Throwable ->
                publishingFailureEvent(
                    order.id,
                    "Failed to set an order to IN_PROGRESS state.",
                    ex
                )
            }
            .andFinally {
                logger.info { "Setting an '${foodInPreparation.orderId}' order to IN_PROGRESS state has been completed"}
            }
    }

    fun handle(addTip: AddTip) {
        logger.info { "Adding ${addTip.tip} tip to '${addTip.orderId}' order." }

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
                    order.id,
                    TipAddedToOrder(order.id, order.tip, order.total)
                )
            }
            .onFailure { ex: Throwable -> publishingFailureEvent(order.id, "Failed to add tip to an order.", ex) }
            .andFinally {
                logger.info { "Adding a tip to '${addTip.orderId}' order has been completed" }
            }
    }

    fun handle(foodDelivered: FoodDelivered) {
        logger.info { "Setting '${foodDelivered.orderId}' order to COMPLETED state" }

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
            .onSuccess { v: Void? -> publishSuccessEvent(order.id, OrderCompleted(foodDelivered.orderId)) }
            .onFailure { ex: Throwable -> publishingFailureEvent(order.id, "Failed to complete an order.", ex) }
            .andFinally {
                logger.info {
                    "Setting an '${foodDelivered.orderId}' order to COMPLETED state has been completed"
                }
            }
    }

    private fun publishSuccessEvent(orderId: String, eventObject: DomainMessageBody) {
        logger.info {"Publishing success event: $eventObject" }
        val event = resultingEvent(orderId, eventObject)
        eventStore!!.store(event)
        publisher!!.send(event)
    }

    private fun publishingFailureEvent(id: String, message: String, ex: Throwable) {
        logger.error(ex) { "$message Publishing OrderProcessingError event" }
        val event = resultingEvent(id, OrderProcessingError(id, message, ex.localizedMessage))
        publisher!!.send(event)
    }

    private fun resultingEvent(orderId: String, eventBody: DomainMessageBody): Message {
        return Message(eventHeader(orderId, eventBody.javaClass.simpleName), eventBody)
    }

    private fun eventHeader(orderId: String, type: String): Header {
        return Header(UUID.randomUUID().toString(), ORDERS_CHANNEL, type, orderId, clock!!.instant())
    }
}


