package io.wkrzywiec.fooddelivery.bff.domain.view.redis.create;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.wkrzywiec.fooddelivery.bff.domain.view.DeliveryViewRepository;
import io.wkrzywiec.fooddelivery.bff.domain.view.redis.read.RedisDeliveryViewRepository;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.redis.RedisMessageConsumerConfig;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.redis.RedisStreamListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.Subscription;

@Configuration
@Profile("redis-search")
public class RedisStreamConsumerConfiguration extends RedisMessageConsumerConfig {

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
}
