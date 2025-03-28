package io.wkrzywiec.fooddelivery.ordering.domain.incoming

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody
import java.util.*

data class FoodInPreparation(val id: UUID, val aggregateVersion: Int) : IntegrationMessageBody {
    override fun orderId(): UUID = id
    override fun version(): Int = aggregateVersion
}
