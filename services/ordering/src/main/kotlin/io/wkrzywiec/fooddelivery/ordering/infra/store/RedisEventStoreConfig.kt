package io.wkrzywiec.fooddelivery.ordering.infra.store

import com.fasterxml.jackson.databind.ObjectMapper
import io.wkrzywiec.fooddelivery.commons.infra.store.EventStore
import io.wkrzywiec.fooddelivery.commons.infra.store.redis.RedisEventStore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.core.RedisTemplate

@Configuration
@Profile("redis-event-store")
class RedisEventStoreConfig {
    @Bean
    fun eventStore(redisTemplate: RedisTemplate<String?, String?>?, objectMapper: ObjectMapper?): EventStore {
        return RedisEventStore(redisTemplate, objectMapper, OrderingEventClassTypeProvider(), "ordering::")
    }
}
