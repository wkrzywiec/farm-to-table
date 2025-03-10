package io.wkrzywiec.fooddelivery.bff.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sonus21.rqueue.core.RqueueEndpointManager;
import com.github.sonus21.rqueue.core.RqueueMessageEnqueuer;
import com.redislabs.lettusearch.RediSearchClient;
import com.redislabs.lettusearch.StatefulRediSearchConnection;
import io.lettuce.core.RedisURI;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import io.wkrzywiec.fooddelivery.bff.inbox.InboxPublisher;
import io.wkrzywiec.fooddelivery.bff.inbox.RedisInboxListener;
import io.wkrzywiec.fooddelivery.bff.inbox.RedisInboxPublisher;
import io.wkrzywiec.fooddelivery.bff.inbox.RedisQueueCreator;
import io.wkrzywiec.fooddelivery.bff.repository.DeliveryViewRepository;
import io.wkrzywiec.fooddelivery.bff.repository.RedisDeliveryViewRepository;
import io.wkrzywiec.fooddelivery.bff.repository.RedisFoodItemRepository;
import io.wkrzywiec.fooddelivery.bff.view.RedisDeliveryViewProcessor;
import io.wkrzywiec.fooddelivery.bff.view.RedisOrdersChannelConsumer;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.MessagePublisher;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.redis.RedisMessageConsumerConfig;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.redis.RedisStreamListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.Subscription;

import java.time.Clock;
import java.time.Duration;

@Slf4j
@Configuration
@Profile("redis")
public class RedisConfiguration extends RedisMessageConsumerConfig {

    @Bean
    RedisURI redisURI(RedisProperties properties) {
        RedisURI redisURI = RedisURI.create(properties.getHost(), properties.getPort());
        if (properties.getPassword() != null) {
            redisURI.setPassword(properties.getPassword().toCharArray());
        }
        Duration timeout = properties.getTimeout();
        if (timeout != null) {
            redisURI.setTimeout(timeout);
        }
        return redisURI;
    }

    @Bean
    ClientResources clientResources() {
        return DefaultClientResources.create();
    }

    @Bean
    RediSearchClient client(RedisURI redisURI, ClientResources clientResources) {
        return RediSearchClient.create(clientResources, redisURI);
    }

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
    StatefulRediSearchConnection<String, String> searchConnection(RediSearchClient rediSearchClient) {
        return rediSearchClient.connect();
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
