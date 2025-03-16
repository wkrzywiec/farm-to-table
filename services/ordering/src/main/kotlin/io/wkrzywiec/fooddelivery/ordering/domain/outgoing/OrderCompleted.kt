package io.wkrzywiec.fooddelivery.ordering.domain.outgoing

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody
import java.util.*

@JvmRecord
data class OrderCompleted(val id: UUID, val version: Int) : IntegrationMessageBody {
    override fun orderId(): UUID = id
    override fun version(): Int = version
}
