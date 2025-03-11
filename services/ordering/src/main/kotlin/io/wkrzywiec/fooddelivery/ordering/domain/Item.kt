package io.wkrzywiec.fooddelivery.ordering.domain

import java.math.BigDecimal


data class Item(val name: String, val amount: Int = 0, val pricePerItem: BigDecimal = BigDecimal(0)) {
}
