package io.wkrzywiec.fooddelivery.ordering.domain.outgoing

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody

@JvmRecord
data class OrderProcessingError(val orderId: String, val message: String, val details: String) : DomainMessageBody
