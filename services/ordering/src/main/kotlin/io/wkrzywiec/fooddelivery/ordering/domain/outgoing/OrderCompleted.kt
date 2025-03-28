package io.wkrzywiec.fooddelivery.ordering.domain.outgoing

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody
import java.util.*

data class OrderCompleted(val id: UUID, val aggregateVersion: Int) : IntegrationMessageBody {
    override fun orderId(): UUID = id
    override fun version(): Int = aggregateVersion
}
