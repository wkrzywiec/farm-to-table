package io.wkrzywiec.fooddelivery.ordering.infra.adapters

import io.wkrzywiec.fooddelivery.ordering.domain.Order
import io.wkrzywiec.fooddelivery.ordering.domain.ports.OrderingRepository
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class InMemoryOrderingRepository : OrderingRepository {
    private val database: MutableMap<String, Order> = ConcurrentHashMap()

    override fun save(newOrder: Order): Order {
        database[newOrder.id] = newOrder
        return newOrder
    }

    override fun findById(id: String): Order? {
        return database[id]
    }
}
