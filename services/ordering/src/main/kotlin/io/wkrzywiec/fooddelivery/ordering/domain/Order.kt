package io.wkrzywiec.fooddelivery.ordering.domain

import io.wkrzywiec.fooddelivery.commons.infra.store.DomainEvent
import io.wkrzywiec.fooddelivery.commons.model.CreateOrder
import io.wkrzywiec.fooddelivery.ordering.domain.outgoing.*
import java.math.BigDecimal
import java.util.*


class Order private constructor(
    val id: UUID,
    version: Int,
    val customerId: String,
    val farmId: String,
    status: OrderStatus,
    val address: String,
    val items: List<Item>,
    val deliveryCharge: BigDecimal,
    tip: BigDecimal,
    val metadata: MutableMap<String, String>
) {

    var version: Int = version
        private set
    var status: OrderStatus = status
        private set
    var tip: BigDecimal = tip
        private set
    var total: BigDecimal = BigDecimal(0)
        private set

    private val changes: List<DomainEvent> = mutableListOf()

    init {
        this.calculateTotal()
    }

    fun uncommittedChanges(): List<DomainEvent> {
        return changes
    }

    private fun calculateTotal() {
        total = items.sumOf { it.pricePerItem.multiply(BigDecimal(it.amount)) }
            .add(deliveryCharge)
            .add(tip)
    }

    fun cancelOrder(reason: String?) {
        if (status != OrderStatus.CREATED) {
            throw OrderingException(
                String.format(
                    "Failed to cancel an %s order. It's not possible to cancel an order with '%s' status",
                    id,
                    status
                )
            )
        }
        this.status = OrderStatus.CANCELED

        if (reason != null) {
            metadata["cancellationReason"] = reason
        }
    }

    fun setInProgress() {
        if (status == OrderStatus.CREATED) {
            this.status = OrderStatus.IN_PROGRESS
            return
        }
        throw OrderingException(
            String.format(
                "Failed to set an '%s' order to IN_PROGRESS. It's not allowed to do it for an order with '%s' status",
                id,
                status
            )
        )
    }

    fun addTip(tip: BigDecimal) {
        this.tip = tip
        this.calculateTotal()
    }

    fun complete() {
        if (status == OrderStatus.IN_PROGRESS) {
            this.status = OrderStatus.COMPLETED
            return
        }
        throw OrderingException(
            String.format(
                "Failed to set an '%s' order to COMPLETED. It's not allowed to do it for an order with '%s' status",
                id,
                status
            )
        )
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
                tip = BigDecimal.ZERO,
                metadata = mutableMapOf()
            )
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
                            tip = BigDecimal.ZERO,
                            metadata = mutableMapOf()
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
