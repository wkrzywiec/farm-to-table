package io.wkrzywiec.fooddelivery.ordering.domain

import io.wkrzywiec.fooddelivery.commons.infra.store.EventEntity
import io.wkrzywiec.fooddelivery.commons.infra.store.EventEntity.newEventEntity
import io.wkrzywiec.fooddelivery.commons.model.CreateOrder
import io.wkrzywiec.fooddelivery.ordering.domain.OrderingFacade.Companion.ORDERS_CHANNEL
import io.wkrzywiec.fooddelivery.ordering.domain.outgoing.OrderCreated
import java.math.BigDecimal
import java.time.Clock
import java.util.*
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible


data class OrderTestData private constructor(
    val id: UUID = UUID.randomUUID(),
    val version: Int = 0,
    val customerId: String = "default-customer-id",
    val farmId: String = "default-farm-id",
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
        val constructor: KFunction<Order>
        val params: List<KParameter>
        try {
            constructor = Order::class.primaryConstructor ?: throw RuntimeException()
            constructor.isAccessible = true
            params = constructor.parameters
        } catch (e: Exception) {
            throw RuntimeException("Failed to construct Order entity class for tests", e)
        }
        val order = constructor.callBy(mapOf(
            params[0] to id,
            params[1] to version,
            params[2] to customerId,
            params[3] to farmId,
            params[4] to status,
            params[5] to address,
            params[6] to items.stream().map { obj: ItemTestData -> obj.entity() }.toList(),
            params[7] to deliveryCharge,
            params[8] to tip
        )
        )

        return order
    }

    fun createOrder(): CreateOrder {
        return CreateOrder(
            id, version, customerId, farmId,
            items.map { obj: ItemTestData -> obj.dto() },
            address, deliveryCharge)
    }

    fun orderCreatedIntegrationEvent(): OrderCreated {
        val entity = entity()
        return OrderCreated(
            id,
            version,
            customerId,
            farmId,
            address,
            items.stream().map { obj: ItemTestData -> obj.dto() }
                .toList(),
            deliveryCharge,
            entity.total)
    }

    fun orderCreatedEntity(clock: Clock): EventEntity {
        return newEventEntity(orderCreated(), ORDERS_CHANNEL, clock)
    }

    fun orderCreated(): OrderingEvent.OrderCreated {
        val entity = entity()
        return OrderingEvent.OrderCreated(
            id,
            version,
            customerId,
            farmId,
            address,
            items.stream().map { obj: ItemTestData -> obj.entity() }
                .toList(),
            deliveryCharge,
            entity.total)
    }

    fun total(): BigDecimal {
        val entity = entity()
        return entity.total
    }

    fun withId(id: UUID): OrderTestData {
        return this.copy(id = id)
    }

    fun withCustomerId(customerId: String): OrderTestData {
        return this.copy(customerId = customerId)
    }

    fun withRestaurantId(restaurantId: String): OrderTestData {
        return this.copy(farmId = restaurantId)
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
