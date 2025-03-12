package io.wkrzywiec.fooddelivery.ordering.domain

import io.wkrzywiec.fooddelivery.commons.incoming.CreateOrder
import io.wkrzywiec.fooddelivery.commons.incoming.Item
import io.wkrzywiec.fooddelivery.ordering.domain.outgoing.OrderCreated
import java.math.BigDecimal
import java.util.*
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible


internal data class OrderTestData private constructor(
    val id: String = UUID.randomUUID().toString(),
    val customerId: String = "default-customer-id",
    val restaurantId: String = "default-restaurant-id",
    val status: OrderStatus = OrderStatus.CREATED,
    val address: String = "Pizza street, Naples, Italy",
    val items: List<ItemTestData> = mutableListOf(ItemTestData.anItem()),
    val deliveryCharge: BigDecimal = BigDecimal(5),
    val tip: BigDecimal = BigDecimal(0),
    val metadata: Map<String, String> = HashMap(),
) {

    companion object {
        @JvmStatic
        fun anOrder(): OrderTestData {
            return OrderTestData()
        }
    }

    fun entity(): Order {
        var constructor: KFunction<Order>
        var params: List<KParameter>
        try {
            constructor = Order::class.primaryConstructor ?: throw RuntimeException()
            constructor.isAccessible = true
            params = constructor.parameters
        } catch (e: Exception) {
            throw RuntimeException("Failed to construct Order entity class for tests", e)
        }
        val order = constructor.callBy(mapOf(
            params[0] to id,
            params[1] to customerId,
            params[2] to restaurantId,
            params[3] to status,
            params[4] to address,
            params[5] to items.stream().map { obj: ItemTestData -> obj.entity() }.toList(),
            params[6] to deliveryCharge,
            params[7] to tip,
            params[8] to metadata,)
        )

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
        return this.copy(id = id)
    }

    fun withCustomerId(customerId: String): OrderTestData {
        return this.copy(customerId = customerId)
    }

    fun withRestaurantId(restaurantId: String): OrderTestData {
        return this.copy(restaurantId = restaurantId)
    }

    fun withStatus(status: OrderStatus): OrderTestData {
        return this.copy(status = status)
    }

    fun withAddress(address: String): OrderTestData {
        return this.copy(address = address)
    }

    fun withItems(vararg items: ItemTestData): OrderTestData {
        return this.copy(items = items.toList())
    }

    fun withDeliveryCharge(deliveryCharge: Double): OrderTestData {
        return this.copy(deliveryCharge = deliveryCharge.toBigDecimal())
    }

    fun withTip(tip: BigDecimal): OrderTestData {
        return this.copy(tip = tip)
    }

    fun withMetadata(metadata: Map<String, String>): OrderTestData {
        return this.copy(metadata = metadata)
    }
}
