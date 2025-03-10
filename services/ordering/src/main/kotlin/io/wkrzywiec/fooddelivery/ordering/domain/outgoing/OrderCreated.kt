package io.wkrzywiec.fooddelivery.ordering.domain.outgoing

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody
import io.wkrzywiec.fooddelivery.commons.incoming.Item
import java.math.BigDecimal

@JvmRecord
data class OrderCreated(
    val orderId: String,
    val customerId: String,
    val restaurantId: String,
    val address: String,
    val items: List<Item>,
    val deliveryCharge: BigDecimal,
    val total: BigDecimal
) : DomainMessageBody
