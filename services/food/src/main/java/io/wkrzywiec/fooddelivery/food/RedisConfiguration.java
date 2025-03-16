package io.wkrzywiec.fooddelivery.food;

import io.wkrzywiec.fooddelivery.food.repository.FoodItemRepository;
import io.wkrzywiec.fooddelivery.food.repository.RedisFoodItemRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.json.commands.RedisJsonV2Commands;

@Configuration
@Profile("redis")
public class RedisConfiguration {

    @Value("${spring.data.redis.host}")
    private String redisHost;
    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Bean
    public RedisJsonV2Commands redisJson() {
        return new UnifiedJedis(HostAndPort.from(redisHost + ":" + redisPort), DefaultJedisClientConfig.builder().build());
    }

    @Bean
    public FoodItemRepository redisFoodItemRepository(RedisJsonV2Commands redisJson) {
        return new RedisFoodItemRepository(redisJson);
    }
}
