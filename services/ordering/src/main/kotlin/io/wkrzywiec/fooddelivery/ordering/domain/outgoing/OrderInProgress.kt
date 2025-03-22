package io.wkrzywiec.fooddelivery.ordering.domain.outgoing

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody
import java.util.*

@JvmRecord
data class OrderInProgress(val id: UUID, val deliveryVersion: Int) : IntegrationMessageBody {
    override fun orderId(): UUID = id
    override fun version(): Int = deliveryVersion
}
