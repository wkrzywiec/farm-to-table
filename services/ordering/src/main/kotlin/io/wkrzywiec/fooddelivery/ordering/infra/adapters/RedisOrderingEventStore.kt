package io.wkrzywiec.fooddelivery.ordering.infra.adapters

import com.fasterxml.jackson.databind.ObjectMapper
import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody
import io.wkrzywiec.fooddelivery.commons.infra.repository.RedisEventStore
import io.wkrzywiec.fooddelivery.ordering.domain.outgoing.*
import lombok.extern.slf4j.Slf4j
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component


@Profile("redis")
@Component
@Slf4j
internal class RedisOrderingEventStore(redisTemplate: RedisTemplate<String?, String?>?, objectMapper: ObjectMapper?) :
    RedisEventStore(redisTemplate, objectMapper) {
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
                RedisOrderingEventStore.log.error("There is not logic for mapping {} event from a store", type)
                null
            }!!
        }
    }
}
