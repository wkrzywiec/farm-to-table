package io.wkrzywiec.fooddelivery.ordering.domain.outgoing

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody
import io.wkrzywiec.fooddelivery.commons.model.Item
import java.math.BigDecimal
import java.util.*

@JvmRecord
data class OrderCreated(
    val id: UUID,
    val deliveryVersion: Int,
    val customerId: String,
    val farmId: String,
    val address: String,
    val items: List<Item>,
    val deliveryCharge: BigDecimal,
    val total: BigDecimal
) : IntegrationMessageBody {
    override fun orderId(): UUID = id
    override fun version(): Int = deliveryVersion
}
