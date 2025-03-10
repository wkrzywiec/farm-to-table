package io.wkrzywiec.fooddelivery.ordering.domain

import io.wkrzywiec.fooddelivery.commons.incoming.CreateOrder
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Message
import io.wkrzywiec.fooddelivery.ordering.domain.outgoing.*
import lombok.EqualsAndHashCode
import lombok.Getter
import lombok.ToString
import java.math.BigDecimal
import java.util.*
import java.util.function.Function

@Getter
@EqualsAndHashCode
@ToString
class Order {
    private var id: String? = null
    private var customerId: String? = null
    private var restaurantId: String? = null
    private var status: OrderStatus? = null
    private var address: String? = null
    private var items: List<Item>? = null
    private var deliveryCharge: BigDecimal? = null
    private var tip = BigDecimal(0)
    private var total = BigDecimal(0)
    private var metadata: MutableMap<String, String>? = null

    private constructor()

    private constructor(
        id: String,
        customerId: String,
        restaurantId: String,
        items: List<Item>,
        address: String,
        deliveryCharge: BigDecimal
    ) : this(
        id,
        customerId,
        restaurantId,
        OrderStatus.CREATED,
        address,
        items,
        deliveryCharge,
        BigDecimal.ZERO,
        HashMap<String, String>()
    )

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
        this.total = items!!.stream()
            .map<Any>(Function<Item, Any> { item: Item ->
                item.getPricePerItem().multiply(BigDecimal.valueOf(item.getAmount()))
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add)
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
                createOrder.orderId,
                createOrder.customerId,
                createOrder.restaurantId,
                mapItems(createOrder.items),
                createOrder.address,
                createOrder.deliveryCharge
            )

            return order
        }

        private fun mapItems(items: List<io.wkrzywiec.fooddelivery.commons.incoming.Item>): List<Item> {
            return items.stream()
                .map<Any>(Function<io.wkrzywiec.fooddelivery.commons.incoming.Item, Any> { dto: io.wkrzywiec.fooddelivery.commons.incoming.Item ->
                    Item.builder()
                        .name(dto.name)
                        .amount(dto.amount)
                        .pricePerItem(dto.pricePerItem)
                        .build()
                }).toList()
        }

        fun from(events: List<Message>): Order? {
            var order: Order? = null
            for (event in events) {
                if (event.body is OrderCreated) {
                    order = Order(
                        created.orderId, created.customerId,
                        created.restaurantId, mapItems(created.items),
                        created.address, created.deliveryCharge
                    )
                }

                if (event.body is OrderCanceled) {
                    val meta: MutableMap<String, String> = order.getMetadata()
                    meta["cancellationReason"] = canceled.reason
                    order = Order(
                        order.getId(), order.getCustomerId(),
                        order.getRestaurantId(), OrderStatus.CANCELED,
                        order.getAddress(), order.getItems(),
                        order.getDeliveryCharge(), order.getTip(),
                        meta
                    )
                }

                if (event.body is OrderInProgress) {
                    order = Order(
                        order.getId(), order.getCustomerId(),
                        order.getRestaurantId(), OrderStatus.IN_PROGRESS,
                        order.getAddress(), order.getItems(),
                        order.getDeliveryCharge(), order.getTip(),
                        order.getMetadata()
                    )
                }

                if (event.body is TipAddedToOrder) {
                    order = Order(
                        order.getId(), order.getCustomerId(),
                        order.getRestaurantId(), order.getStatus(),
                        order.getAddress(), order.getItems(),
                        order.getDeliveryCharge(), tipAdded.tip,
                        order.getMetadata()
                    )
                }

                if (event.body is OrderCompleted) {
                    order = Order(
                        order.getId(), order.getCustomerId(),
                        order.getRestaurantId(), OrderStatus.COMPLETED,
                        order.getAddress(), order.getItems(),
                        order.getDeliveryCharge(), order.getTip(),
                        order.getMetadata()
                    )
                }
            }
            return order
        }
    }
}
