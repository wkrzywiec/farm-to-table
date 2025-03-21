package io.wkrzywiec.fooddelivery.ordering.domain.outgoing

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody
import java.util.*

@JvmRecord
data class OrderCompleted(val orderId: UUID, val version: Int) : IntegrationMessageBody
