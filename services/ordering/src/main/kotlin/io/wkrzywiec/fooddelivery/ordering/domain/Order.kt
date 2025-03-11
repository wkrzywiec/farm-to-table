package io.wkrzywiec.fooddelivery.ordering.domain

import io.wkrzywiec.fooddelivery.commons.incoming.CreateOrder
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Message
import io.wkrzywiec.fooddelivery.ordering.domain.outgoing.*
import java.math.BigDecimal
import java.util.*

class Order {

    val id: String
    val customerId: String
    val restaurantId: String
    var status: OrderStatus
        private set
    val address: String
    val items: List<Item>
    val deliveryCharge: BigDecimal
    var tip: BigDecimal = BigDecimal(0)
        private set
    var total: BigDecimal = BigDecimal(0)
        private set
    val metadata: MutableMap<String, String>

    private constructor(
        id: String?,
        customerId: String,
        restaurantId: String,
        status: OrderStatus,
        address: String,
        items: List<Item>,
        deliveryCharge: BigDecimal,
        tip: BigDecimal,
        metadata: MutableMap<String, String>
    ) {
        if (id == null) {
            this.id = UUID.randomUUID().toString()
        } else {
            this.id = id
        }
        this.customerId = customerId
        this.restaurantId = restaurantId
        this.status = status
        this.address = address
        this.items = items
        this.deliveryCharge = deliveryCharge
        this.tip = tip
        this.metadata = metadata
        this.calculateTotal()
    }

    fun calculateTotal() {
        total = items!!.sumOf { it.pricePerItem.multiply(BigDecimal(it.amount)) }
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
            metadata!!["cancellationReason"] = reason
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
                customerId = createOrder.customerId,
                restaurantId = createOrder.restaurantId,
                status = OrderStatus.CREATED,
                address = createOrder.address,
                items = mapItems(createOrder.items),
                deliveryCharge = createOrder.deliveryCharge,
                tip = BigDecimal.ZERO,
                metadata = mutableMapOf<String, String>()
            )
            return order
        }

        private fun mapItems(items: List<io.wkrzywiec.fooddelivery.commons.incoming.Item>): List<Item> {
            return items.stream()
                .map{
                    Item(name = it.name, amount = it.amount, pricePerItem = it.pricePerItem)
                }
                .toList()
        }

        fun from(events: List<Message>): Order {
            lateinit var order: Order
            for (event in events) {
                val body = event.body
                if (body is OrderCreated) {
                    order = Order(
                        id = body.id,
                        customerId = body.customerId,
                        restaurantId = body.restaurantId,
                        status = OrderStatus.CREATED,
                        address = body.address,
                        items = mapItems(body.items),
                        deliveryCharge = body.deliveryCharge,
                        tip = BigDecimal.ZERO,
                        metadata = mutableMapOf<String, String>()
                    )
                }

                if (body is OrderCanceled) {
                    val meta: MutableMap<String, String> = order.metadata
                    meta["cancellationReason"] = body.reason
                    order = Order(
                        id = order.id,
                        customerId = order.customerId,
                        restaurantId = order.restaurantId,
                        status = OrderStatus.CANCELED,
                        address = order.address,
                        items = order.items,
                        deliveryCharge = order.deliveryCharge,
                        tip = order.tip,
                        metadata = meta
                    )
                }

                if (body is OrderInProgress) {
                    order = Order(
                        id = order.id,
                        customerId = order.customerId,
                        restaurantId = order.restaurantId,
                        status = OrderStatus.IN_PROGRESS,
                        address = order.address,
                        items = order.items,
                        deliveryCharge = order.deliveryCharge,
                        tip = order.tip,
                        metadata = order.metadata
                    )
                }

                if (body is TipAddedToOrder) {
                    order = Order(
                        id = order.id,
                        customerId = order.customerId,
                        restaurantId = order.restaurantId,
                        status = order.status,
                        address = order.address,
                        items = order.items,
                        deliveryCharge = order.deliveryCharge,
                        tip = body.tip,
                        metadata = order.metadata
                    )
                }

                if (body is OrderCompleted) {
                    order = Order(
                        id = order.id,
                        customerId = order.customerId,
                        restaurantId = order.restaurantId,
                        status = OrderStatus.COMPLETED,
                        address = order.address,
                        items = order.items,
                        deliveryCharge = order.deliveryCharge,
                        tip = order.tip,
                        metadata = order.metadata
                    )
                }
            }
            return order
        }
    }
}
