package io.wkrzywiec.fooddelivery.bff.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redislabs.lettusearch.StatefulRediSearchConnection;
import io.wkrzywiec.fooddelivery.bff.view.read.DeliveryViewRepository;
import io.wkrzywiec.fooddelivery.bff.view.read.RedisDeliveryViewRepository;
import io.wkrzywiec.fooddelivery.bff.view.read.RedisFoodItemRepository;
import io.wkrzywiec.fooddelivery.bff.view.create.RedisDeliveryViewProcessor;
import io.wkrzywiec.fooddelivery.bff.view.create.RedisOrdersChannelConsumer;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.redis.RedisMessageConsumerConfig;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.redis.RedisStreamListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.Subscription;

@Configuration
@Profile("redis")
public class RedisConfiguration extends RedisMessageConsumerConfig {

    @Bean
    public Subscription ordersChannelSubscription(RedisConnectionFactory factory,
                                                  RedisTemplate<String, String> redisTemplate,
                                                  RedisStreamListener streamListener) {
        return createSubscription(redisTemplate, factory, streamListener);
    }

    @Bean
    public RedisStreamListener redisOrdersChannelConsumer(RedisDeliveryViewProcessor processor, ObjectMapper objectMapper) {
        return new RedisOrdersChannelConsumer(processor, objectMapper);
    }

    @Bean
    public RedisDeliveryViewProcessor redisDeliveryViewProcessor(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        return new RedisDeliveryViewProcessor(redisTemplate, objectMapper);
    }

    @Bean
    public DeliveryViewRepository redisDeliveryViewRepository(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        return new RedisDeliveryViewRepository(redisTemplate, objectMapper);
    }

    @Bean
    public RedisFoodItemRepository redisFoodItemRepository(StatefulRediSearchConnection<String, String> searchConnection, ObjectMapper objectMapper) {
        return new RedisFoodItemRepository(searchConnection, objectMapper);
    }

    //todo prevent redisQueue from creating if in memory-profile
}
