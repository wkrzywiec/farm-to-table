package io.wkrzywiec.fooddelivery.ordering.infra.adapters

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody
import io.wkrzywiec.fooddelivery.commons.infra.repository.RedisEventStore
import io.wkrzywiec.fooddelivery.ordering.domain.outgoing.*
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Profile("redis")
@Component
internal class RedisOrderingEventStore(redisTemplate: RedisTemplate<String, String>, objectMapper: ObjectMapper) : RedisEventStore(redisTemplate, objectMapper) {

    override fun streamPrefix(): String {
        return "ordering::"
    }

    override fun getClassType(type: String): Class<out DomainMessageBody?> {
        return when (type) {
            "OrderCreated" -> OrderCreated::class.java
            "OrderCanceled" -> OrderCanceled::class.java
            "OrderInProgress" -> OrderInProgress::class.java
            "TipAddedToOrder" -> TipAddedToOrder::class.java
            "OrderCompleted" -> OrderCompleted::class.java
            else -> {
                val msg = "There is not logic for mapping $type event from a store"
                logger.error { msg }
                throw IllegalArgumentException(msg)
            }
        }
    }
}
