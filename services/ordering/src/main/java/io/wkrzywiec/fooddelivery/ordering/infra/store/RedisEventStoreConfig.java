package io.wkrzywiec.fooddelivery.ordering.infra.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.wkrzywiec.fooddelivery.commons.infra.store.EventStore;
import io.wkrzywiec.fooddelivery.commons.infra.store.PostgresEventStore;
import io.wkrzywiec.fooddelivery.commons.infra.store.RedisEventStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
@Profile("redis-event-store")
public class RedisEventStoreConfig {

    @Bean
    public EventStore eventStore(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        return new RedisEventStore(redisTemplate, objectMapper, new OrderingEventClassTypeProvider(), "ordering::");
    }
}
