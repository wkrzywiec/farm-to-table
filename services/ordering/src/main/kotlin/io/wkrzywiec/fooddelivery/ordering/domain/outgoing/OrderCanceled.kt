package io.wkrzywiec.fooddelivery.ordering.domain.outgoing

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody
import java.util.*

@JvmRecord
data class OrderCanceled(val id: UUID, val deliveryVersion: Int, val reason: String) : IntegrationMessageBody {
    override fun orderId(): UUID = id
    override fun version(): Int = deliveryVersion
}
