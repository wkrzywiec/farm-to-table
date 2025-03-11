package io.wkrzywiec.fooddelivery.ordering.domain.outgoing

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody

@JvmRecord
data class OrderCanceled(val id: String, val reason: String) : DomainMessageBody {
    override fun orderId(): String = id
}
