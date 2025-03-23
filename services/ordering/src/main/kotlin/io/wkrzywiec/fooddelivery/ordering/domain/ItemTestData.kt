package io.wkrzywiec.fooddelivery.ordering.domain

import java.math.BigDecimal

data class ItemTestData private constructor(
    val name: String = "Pizza Margherita",
    val amount: Int = 1,
    val pricePerItem: BigDecimal = BigDecimal(10)
) {

    companion object {
        @JvmStatic
        fun anItem(): ItemTestData {
            return ItemTestData()
        }
    }

    fun entity(): Item {
        return Item(
            name = name,
            amount = amount,
            pricePerItem = pricePerItem
        )
    }

    fun dto(): io.wkrzywiec.fooddelivery.commons.model.Item {
        return io.wkrzywiec.fooddelivery.commons.model.Item(name, amount, pricePerItem)
    }

    fun withName(name: String): ItemTestData {
        return this.copy(name = name)
    }

    fun withPricePerItem(price: Double): ItemTestData {
        return this.copy(pricePerItem = BigDecimal(price))
    }

    fun withAmount(amount: Int): ItemTestData {
        return this.copy(amount = amount)
    }
}
