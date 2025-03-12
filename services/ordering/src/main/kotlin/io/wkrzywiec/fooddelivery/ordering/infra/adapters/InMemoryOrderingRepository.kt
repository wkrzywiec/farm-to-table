package io.wkrzywiec.fooddelivery.ordering.infra.adapters

import io.wkrzywiec.fooddelivery.ordering.domain.Order
import io.wkrzywiec.fooddelivery.ordering.domain.ports.OrderingRepository
import org.apache.commons.lang3.reflect.FieldUtils
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Component
class InMemoryOrderingRepository : OrderingRepository {
    private val database: MutableMap<String, Order> = ConcurrentHashMap()

    override fun save(newOrder: Order): Order {
        var id: String = newOrder.id
        if (Objects.isNull(id)) {
            id = UUID.randomUUID().toString()

            try {
                FieldUtils.writeField(newOrder, "id", id, true)
            } catch (e: IllegalAccessException) {
                throw RuntimeException("Class 'Order' does not have 'id' field")
            }
        }
        database[id] = newOrder
        return newOrder
    }

    override fun findById(id: String): Order? {
        return database[id]
    }
}
