package io.wkrzywiec.fooddelivery.ordering.domain.outgoing

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody
import java.util.*

@JvmRecord
data class OrderProcessingError(val id: UUID, val version: Int, val message: String, val details: String) : IntegrationMessageBody {
    override fun orderId(): UUID = id
    override fun version(): Int = version
}