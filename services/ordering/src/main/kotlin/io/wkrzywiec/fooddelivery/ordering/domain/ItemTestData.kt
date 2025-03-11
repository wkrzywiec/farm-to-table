package io.wkrzywiec.fooddelivery.ordering.domain

import java.math.BigDecimal

internal class ItemTestData private constructor() {
    private var name = "Pizza Margherita"
    private var amount = 1
    private var pricePerItem = BigDecimal(10)

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

    fun dto(): io.wkrzywiec.fooddelivery.commons.incoming.Item {
        return io.wkrzywiec.fooddelivery.commons.incoming.Item(name, amount, pricePerItem)
    }

    fun withName(name: String): ItemTestData {
        this.name = name
        return this
    }

    fun withPricePerItem(price: Double): ItemTestData {
        this.pricePerItem = BigDecimal(price)
        return this
    }

    fun withAmount(amount: Int): ItemTestData {
        this.amount = amount
        return this
    }
}
