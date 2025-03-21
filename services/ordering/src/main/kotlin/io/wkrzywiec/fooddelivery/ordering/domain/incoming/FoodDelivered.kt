package io.wkrzywiec.fooddelivery.ordering.domain.incoming

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody
import java.util.*

@JvmRecord
data class FoodDelivered(val orderId: UUID, val version: Int) : IntegrationMessageBody
