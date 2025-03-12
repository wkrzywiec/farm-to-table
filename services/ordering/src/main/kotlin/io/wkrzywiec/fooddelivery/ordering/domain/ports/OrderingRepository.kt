package io.wkrzywiec.fooddelivery.ordering.domain.ports

import io.wkrzywiec.fooddelivery.ordering.domain.Order
import java.util.*

interface OrderingRepository {
    fun save(newOrder: Order): Order
    fun findById(id: String): Order?
}
