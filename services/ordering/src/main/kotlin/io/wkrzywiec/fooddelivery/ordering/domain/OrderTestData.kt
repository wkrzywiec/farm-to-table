package io.wkrzywiec.fooddelivery.ordering.domain

import io.wkrzywiec.fooddelivery.commons.incoming.CreateOrder
import io.wkrzywiec.fooddelivery.commons.incoming.Item
import io.wkrzywiec.fooddelivery.ordering.domain.outgoing.OrderCreated
import org.apache.commons.lang3.reflect.FieldUtils
import java.math.BigDecimal
import java.util.*


internal class OrderTestData private constructor() {
    var id = UUID.randomUUID().toString()
        private set
    var customerId = "default-customer-id"
        private set
    var restaurantId = "default-restaurant-id"
        private set
    var status = OrderStatus.CREATED
        private set
    var address = "Pizza street, Naples, Italy"
        private set
    var items: List<ItemTestData> = mutableListOf(ItemTestData.anItem())
        private set
    var deliveryCharge: BigDecimal = BigDecimal(5)
        private set
    var tip: BigDecimal = BigDecimal(0)
        private set
    var metadata: Map<String, String> = HashMap()
        private set

    companion object {
        @JvmStatic
        fun anOrder(): OrderTestData {
            return OrderTestData()
        }
    }

    fun entity(): Order {
        val order = createAnEmptyOrder()
        setValue(order, "id", id)
        setValue(order, "customerId", customerId)
        setValue(order, "restaurantId", restaurantId)
        setValue(order, "status", status)
        setValue(order, "address", address)
        setValue(order, "items", items.stream().map { obj: ItemTestData -> obj.entity() }
            .toList())
        setValue(order, "deliveryCharge", deliveryCharge)
        setValue(order, "tip", tip)
        setValue(order, "metadata", metadata)

        order.calculateTotal()
        return order
    }

    fun createOrder(): CreateOrder {
        return CreateOrder(id, customerId, restaurantId, items.stream().map<Item> { obj: ItemTestData -> obj.dto() }
            .toList(), address, deliveryCharge)
    }

    fun orderCreated(): OrderCreated {
        val entity = entity()
        return OrderCreated(id, customerId, restaurantId, address, items.stream().map { obj: ItemTestData -> obj.dto() }
            .toList(), deliveryCharge, entity.total)
    }

    fun withId(id: String): OrderTestData {
        this.id = id
        return this
    }

    fun withCustomerId(customerId: String): OrderTestData {
        this.customerId = customerId
        return this
    }

    fun withRestaurantId(restaurantId: String): OrderTestData {
        this.restaurantId = restaurantId
        return this
    }

    fun withStatus(status: OrderStatus): OrderTestData {
        this.status = status
        return this
    }

    fun withAddress(address: String): OrderTestData {
        this.address = address
        return this
    }

    fun withItems(vararg items: ItemTestData): OrderTestData {
        this.items = items.asList()
        return this
    }

    fun withDeliveryCharge(deliveryCharge: Double): OrderTestData {
        this.deliveryCharge = BigDecimal(deliveryCharge)
        return this
    }

    fun withTip(tip: BigDecimal): OrderTestData {
        this.tip = tip
        return this
    }

    fun withMetadata(metadata: Map<String, String>): OrderTestData {
        this.metadata = metadata
        return this
    }

    private fun createAnEmptyOrder(): Order {
        try {
            val constructor = Order::class.java.getDeclaredConstructor()
            constructor.isAccessible = true
            return constructor.newInstance()
        } catch (e: Exception) {
            throw RuntimeException("Failed to construct Order entity class for tests", e)
        }
    }

    private fun setValue(order: Order, fieldName: String, value: Any) {
        try {
            FieldUtils.writeField(order, fieldName, value, true)
        } catch (e: IllegalAccessException) {
            throw RuntimeException(
                String.format("Failed to set a %s field in Order entity class for tests", fieldName),
                e
            )
        }
    }
}
