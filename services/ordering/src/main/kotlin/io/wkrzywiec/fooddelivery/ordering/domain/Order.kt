package io.wkrzywiec.fooddelivery.ordering.domain

import io.wkrzywiec.fooddelivery.commons.infra.store.DomainEvent
import io.wkrzywiec.fooddelivery.commons.model.CreateOrder
import java.math.BigDecimal
import java.util.*


class Order private constructor(
    val id: UUID = UUID.randomUUID(),
    version: Int,
    val customerId: String,
    val farmId: String,
    status: OrderStatus = OrderStatus.CREATED,
    val address: String,
    val items: List<Item>,
    val deliveryCharge: BigDecimal,
    tip: BigDecimal
) {

    var version: Int = version
        private set
    var status: OrderStatus = status
        private set
    var tip: BigDecimal = tip
        private set
    var total: BigDecimal = BigDecimal(0)
        private set

    private val changes: MutableList<DomainEvent> = mutableListOf()

    init {
        total = calculateTotal()
    }

    private fun calculateTotal(): BigDecimal {
        return items.sumOf { it.pricePerItem.multiply(BigDecimal(it.amount)) }
            .add(deliveryCharge)
            .add(tip)
    }

    fun uncommittedChanges(): List<DomainEvent> {
        return changes
    }

    fun cancelOrder(reason: String) {
        if (status != OrderStatus.CREATED) {
            throw OrderingException("Failed to cancel an $id order. It's not possible to cancel an order with '$status' status")
        }
        status = OrderStatus.CANCELED

        increaseVersion()
        changes.add(OrderingEvent.OrderCanceled(id, version, reason))
    }

    fun setInProgress() {
        if (status == OrderStatus.CREATED) {
            status = OrderStatus.IN_PROGRESS

            increaseVersion()
            changes.add(OrderingEvent.OrderInProgress(id, version))
            return
        }
        throw OrderingException(
            "Failed to set an '$id' order to IN_PROGRESS. It's not allowed to do it for an order with '$status' status"
        )
    }

    fun addTip(tip: BigDecimal) {
        this.tip = tip
        total = calculateTotal()

        increaseVersion()
        changes.add(OrderingEvent.TipAddedToOrder(id, version, tip, total))
    }

    fun complete() {
        if (status == OrderStatus.IN_PROGRESS) {
            status = OrderStatus.COMPLETED

            increaseVersion()
            changes.add(OrderingEvent.OrderCompleted(id, version))
            return
        }
        throw OrderingException(
            "Failed to set an '$id' order to COMPLETED. It's not allowed to do it for an order with $status' status"
        )
    }

    private fun increaseVersion() {
        version += 1
    }

    companion object {

        @JvmStatic
        fun from(createOrder: CreateOrder): Order {
            val order = Order(
                id = createOrder.orderId,
                version = 0,
                customerId = createOrder.customerId,
                farmId = createOrder.farmId,
                status = OrderStatus.CREATED,
                address = createOrder.address,
                items = mapItems(createOrder.items),
                deliveryCharge = createOrder.deliveryCharge,
                tip = BigDecimal.ZERO
            )
            order.changes.add(OrderingEvent.OrderCreated(
                order.id, 0,
                order.customerId, order.farmId,
                order.address, order.items,
                order.deliveryCharge, order.total
            ))
            return order
        }

        private fun mapItems(items: List<io.wkrzywiec.fooddelivery.commons.model.Item>): List<Item> {
            return items.stream()
                .map{
                    Item(name = it.name, amount = it.amount, pricePerItem = it.pricePerItem)
                }
                .toList()
        }

        @JvmStatic
        fun from(events: List<OrderingEvent>): Order {
            lateinit var order: Order
            for (event in events) {
                when (event) {
                    is OrderingEvent.OrderCreated -> {
                        order = Order(
                            id = event.orderId,
                            version = 0,
                            customerId = event.customerId,
                            farmId = event.farmId,
                            status = OrderStatus.CREATED,
                            address = event.address,
                            items = event.items,
                            deliveryCharge = event.deliveryCharge,
                            tip = BigDecimal.ZERO
                        )
                    }

                    is OrderingEvent.OrderCanceled -> {
                        order.status = OrderStatus.CANCELED
                        order.version = event.version
                    }

                    is OrderingEvent.OrderInProgress -> {
                        order.status = OrderStatus.IN_PROGRESS
                        order.version = event.version
                    }

                    is OrderingEvent.TipAddedToOrder -> {
                        order.tip = event.tip
                        order.version = event.version
                    }

                    is OrderingEvent.OrderCompleted -> {
                        order.status = OrderStatus.COMPLETED
                        order.version = event.version
                    }

                    else -> throw IllegalArgumentException("Failed to replay events to build order object. Unhandled events: ${event::class.simpleName}")
                }
            }
            return order
            }
    }
}
