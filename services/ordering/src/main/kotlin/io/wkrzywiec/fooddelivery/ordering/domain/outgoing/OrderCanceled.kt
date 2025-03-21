package io.wkrzywiec.fooddelivery.ordering.domain.outgoing

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody
import java.util.*

@JvmRecord
data class OrderCanceled(val orderId: UUID, val version: Int, val reason: String) : IntegrationMessageBody
