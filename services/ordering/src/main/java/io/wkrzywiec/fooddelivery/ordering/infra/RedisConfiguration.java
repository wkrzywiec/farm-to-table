package io.wkrzywiec.fooddelivery.ordering.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.redis.RedisMessageConsumerConfig;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.redis.RedisStreamListener;
import io.wkrzywiec.fooddelivery.ordering.application.RedisOrdersChannelConsumer;
import io.wkrzywiec.fooddelivery.ordering.domain.OrderingFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.Subscription;

@Slf4j
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
    public RedisOrdersChannelConsumer redisOrdersChannelConsumer(OrderingFacade facade, ObjectMapper objectMapper) {
        return new RedisOrdersChannelConsumer(facade, objectMapper);
    }
}
