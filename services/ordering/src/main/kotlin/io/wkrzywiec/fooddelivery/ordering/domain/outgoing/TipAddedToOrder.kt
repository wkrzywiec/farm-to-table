package io.wkrzywiec.fooddelivery.ordering.domain.outgoing

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody
import java.math.BigDecimal
import java.util.*

@JvmRecord
data class TipAddedToOrder(val id: UUID, val deliveryVersion: Int, val tip: BigDecimal, val total: BigDecimal) : IntegrationMessageBody {
    override fun orderId(): UUID = id
    override fun version(): Int = deliveryVersion
}
