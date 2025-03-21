package io.wkrzywiec.fooddelivery.ordering.domain

import io.wkrzywiec.fooddelivery.commons.infra.store.DomainEvent
import java.math.BigDecimal
import java.util.*

interface OrderingEvent : DomainEvent {
    fun orderId(): UUID

    override fun streamId(): UUID {
        return orderId()
    }

    @JvmRecord
    data class OrderCreated(
        @JvmField val orderId: UUID,
        @JvmField val version: Int,
        @JvmField val customerId: String,
        @JvmField val farmId: String,
        @JvmField val address: String,
        @JvmField val items: List<Item>,
        @JvmField val deliveryCharge: BigDecimal,
        @JvmField val total: BigDecimal
    ) : OrderingEvent

    @JvmRecord
    data class OrderCanceled(
        @JvmField val orderId: UUID,
        @JvmField val version: Int,
        @JvmField val reason: String
    ) : OrderingEvent

    @JvmRecord
    data class OrderInProgress(
        @JvmField val orderId: UUID,
        @JvmField val version: Int
    ) : OrderingEvent

    @JvmRecord
    data class TipAddedToOrder(
        @JvmField val orderId: UUID,
        @JvmField val version: Int,
        @JvmField val tip: BigDecimal,
        @JvmField val total: BigDecimal
    ) : OrderingEvent

    @JvmRecord
    data class OrderCompleted(
        @JvmField val orderId: UUID,
        @JvmField val version: Int
    ) : OrderingEvent
}
