package io.wkrzywiec.fooddelivery.ordering.domain.outgoing

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody
import java.math.BigDecimal

@JvmRecord
data class TipAddedToOrder(val orderId: String, val tip: BigDecimal, val total: BigDecimal) : DomainMessageBody
