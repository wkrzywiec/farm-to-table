package io.wkrzywiec.fooddelivery.ordering.infra

import com.fasterxml.jackson.databind.ObjectMapper
import io.wkrzywiec.fooddelivery.commons.infra.messaging.redis.RedisMessageConsumerConfig
import io.wkrzywiec.fooddelivery.commons.infra.messaging.redis.RedisStreamListener
import io.wkrzywiec.fooddelivery.ordering.application.RedisOrdersChannelConsumer
import io.wkrzywiec.fooddelivery.ordering.domain.OrderingFacade
import lombok.extern.slf4j.Slf4j
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.stream.Subscription

@Slf4j
@Configuration
@Profile("redis")
class RedisConfiguration : RedisMessageConsumerConfig() {
    @Bean
    fun ordersChannelSubscription(
        factory: RedisConnectionFactory?,
        redisTemplate: RedisTemplate<String?, String?>?,
        streamListener: RedisStreamListener?
    ): Subscription {
        return createSubscription(redisTemplate, factory, streamListener)
    }

    @Bean
    fun redisOrdersChannelConsumer(facade: OrderingFacade?, objectMapper: ObjectMapper?): RedisStreamListener {
        return RedisOrdersChannelConsumer(facade, objectMapper)
    }
}
