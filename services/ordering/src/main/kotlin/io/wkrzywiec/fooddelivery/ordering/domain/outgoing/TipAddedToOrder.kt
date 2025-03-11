package io.wkrzywiec.fooddelivery.ordering.domain.outgoing

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody
import java.math.BigDecimal

@JvmRecord
data class TipAddedToOrder(val id: String, val tip: BigDecimal, val total: BigDecimal) : DomainMessageBody {
    override fun orderId(): String = id
}
