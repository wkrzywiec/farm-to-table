package io.wkrzywiec.fooddelivery.ordering.infra.store

import io.github.oshai.kotlinlogging.KotlinLogging
import io.wkrzywiec.fooddelivery.commons.infra.store.DomainEvent
import io.wkrzywiec.fooddelivery.commons.infra.store.EventClassTypeProvider
import io.wkrzywiec.fooddelivery.ordering.domain.OrderingEvent
import lombok.extern.slf4j.Slf4j

private val logger = KotlinLogging.logger {}

@Slf4j
internal class OrderingEventClassTypeProvider : EventClassTypeProvider {
    override fun getClassType(type: String): Class<out DomainEvent>? {
        return when (type) {
            "OrderCreated" -> OrderingEvent.OrderCreated::class.java
            "OrderCanceled" -> OrderingEvent.OrderCanceled::class.java
            "OrderInProgress" -> OrderingEvent.OrderInProgress::class.java
            "TipAddedToOrder" -> OrderingEvent.TipAddedToOrder::class.java
            "OrderCompleted" -> OrderingEvent.OrderCompleted::class.java

            else -> {
                logger.error { "${"There is not logic for mapping {} event from a store"} $type" }
                return null
            }
        }
    }
}
