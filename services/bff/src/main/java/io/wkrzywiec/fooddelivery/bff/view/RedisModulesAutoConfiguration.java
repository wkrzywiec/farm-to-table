package io.wkrzywiec.fooddelivery.bff.view;

import com.redis.lettucemod.RedisModulesClient;
import com.redis.lettucemod.api.StatefulRedisModulesConnection;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;

import java.time.Duration;

//todo try exclude io.lettuce.core.resource.ClientResources from org/springframework/boot/autoconfigure/data/redis/LettuceConnectionConfiguration.class

/*
    Copied from 'com.redis:spring-lettucemod:3.8.0' library.
    https://github.com/redis/lettucemod/blob/master/subprojects/spring-lettucemod/src/main/java/com/redis/spring/lettucemod/RedisModulesAutoConfiguration.java

    The above library creates a duplicate bean - io.lettuce.core.resource.ClientResources - which can't be configured
    	- lettuceClientResources: defined by method 'lettuceClientResources' in class path resource [org/springframework/boot/autoconfigure/data/redis/LettuceConnectionConfiguration.class]
	    - clientResources: defined by method 'clientResources' in class path resource [com/redis/spring/lettucemod/RedisModulesAutoConfiguration.class]
 */

@Configuration
@Profile("redis-stream")
public class RedisModulesAutoConfiguration {

    @SuppressWarnings("deprecation")
    @Bean
    RedisURI redisURI(RedisProperties properties) {
        RedisURI uri = StringUtils.hasLength(properties.getUrl()) ? RedisURI.create(properties.getUrl())
                : RedisURI.create(properties.getHost(), properties.getPort());
        if (StringUtils.hasLength(properties.getClientName())) {
            uri.setClientName(properties.getClientName());
        }
        if (properties.getDatabase() > 0) {
            uri.setDatabase(properties.getDatabase());
        }
        if (StringUtils.hasLength(properties.getPassword())) {
            uri.setPassword(properties.getPassword());
        }
        if (properties.getSsl().isEnabled()) {
            uri.setSsl(true);
        }
        if (properties.getTimeout() != null) {
            uri.setTimeout(properties.getTimeout());
        }
        if (StringUtils.hasLength(properties.getUsername())) {
            uri.setUsername(properties.getUsername());
        }
        return uri;
    }

    @Bean(destroyMethod = "shutdown")
    ClientResources clientResources() {
        return DefaultClientResources.create();
    }

    @Bean(destroyMethod = "shutdown")
    RedisModulesClient client(RedisURI redisURI, RedisProperties properties, ClientResources clientResources) {
        RedisModulesClient client = RedisModulesClient.create(clientResources, redisURI);
        client.setOptions(clientOptions(ClientOptions.builder(), properties).build());
        return client;
    }

    private <B extends ClientOptions.Builder> B clientOptions(B builder, RedisProperties properties) {
        RedisModulesClient.defaultClientOptions(builder);
        Duration connectTimeout = properties.getConnectTimeout();
        if (connectTimeout != null) {
            builder.socketOptions(SocketOptions.builder().connectTimeout(connectTimeout).build());
        }
        builder.timeoutOptions(TimeoutOptions.enabled());
        return builder;
    }

    @Bean(name = "redisConnection", destroyMethod = "close")
    @ConditionalOnBean(RedisModulesClient.class)
    StatefulRedisModulesConnection<String, String> redisConnection(RedisModulesClient client) {
        return client.connect();
    }
}
