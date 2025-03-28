package io.wkrzywiec.fooddelivery.ordering.domain.outgoing

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody
import java.util.*

data class OrderCanceled(val id: UUID, val aggregateVersion: Int, val reason: String) : IntegrationMessageBody {
    override fun orderId(): UUID = id
    override fun version(): Int = aggregateVersion
}
