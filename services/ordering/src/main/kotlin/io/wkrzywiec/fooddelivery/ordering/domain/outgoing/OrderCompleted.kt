package io.wkrzywiec.fooddelivery.ordering.domain.outgoing

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody

@JvmRecord
data class OrderCompleted(val orderId: String) : DomainMessageBody
