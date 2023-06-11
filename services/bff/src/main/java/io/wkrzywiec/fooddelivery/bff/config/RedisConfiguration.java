package io.wkrzywiec.fooddelivery.bff.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sonus21.rqueue.core.RqueueEndpointManager;
import com.github.sonus21.rqueue.core.RqueueMessageEnqueuer;
import com.redislabs.lettusearch.StatefulRediSearchConnection;
import io.wkrzywiec.fooddelivery.bff.inbox.InboxPublisher;
import io.wkrzywiec.fooddelivery.bff.inbox.RedisInboxListener;
import io.wkrzywiec.fooddelivery.bff.inbox.RedisInboxPublisher;
import io.wkrzywiec.fooddelivery.bff.inbox.RedisQueueCreator;
import io.wkrzywiec.fooddelivery.bff.repository.DeliveryViewRepository;
import io.wkrzywiec.fooddelivery.bff.repository.RedisDeliveryViewRepository;
import io.wkrzywiec.fooddelivery.bff.repository.RedisFoodItemRepository;
import io.wkrzywiec.fooddelivery.bff.view.DeliveryStatus;
import io.wkrzywiec.fooddelivery.bff.view.RedisDeliveryViewProcessor;
import io.wkrzywiec.fooddelivery.bff.view.RedisOrdersChannelConsumer;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.MessagePublisher;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.redis.RedisMessageConsumerConfig;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.redis.RedisStreamListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.Subscription;

import java.time.Clock;

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
    public RedisStreamListener redisOrdersChannelConsumer(RedisDeliveryViewProcessor processor, ObjectMapper objectMapper) {
        return new RedisOrdersChannelConsumer(processor, objectMapper);
    }

    @Bean
    public InboxPublisher redisInboxPublisher(RqueueMessageEnqueuer redisQueue) {
        return new RedisInboxPublisher(redisQueue);
    }

    @Bean
    public RedisInboxListener redisInboxListener(MessagePublisher messagePublisher, Clock clock) {
        return new RedisInboxListener(messagePublisher, clock);
    }

    @Bean
    public RedisQueueCreator redisQueueCreator(RqueueEndpointManager rqueueEndpointManager) {
        return new RedisQueueCreator(rqueueEndpointManager);
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
