package io.wkrzywiec.fooddelivery.food;

import com.redislabs.modules.rejson.JReJSON;
import io.wkrzywiec.fooddelivery.food.repository.FoodItemRepository;
import io.wkrzywiec.fooddelivery.food.repository.RedisFoodItemRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("redis")
public class RedisConfiguration {

    @Value("${spring.redis.host}")
    private String redisHost;
    @Value("${spring.redis.port}")
    private int redisPort;

    @Bean
    public JReJSON redisJson() {
        return new JReJSON(redisHost, redisPort);
    }

    @Bean
    public FoodItemRepository redisFoodItemRepository(JReJSON jReJSON) {
        return new RedisFoodItemRepository(jReJSON);
    }
}
