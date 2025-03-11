package io.wkrzywiec.fooddelivery.ordering.domain.outgoing

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody

@JvmRecord
data class OrderCompleted(val id: String) : DomainMessageBody {
    override fun orderId(): String = id
}
