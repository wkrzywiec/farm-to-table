package io.wkrzywiec.fooddelivery.bff.domain.inbox.redis;

import com.github.sonus21.rqueue.core.RqueueEndpointManager;
import com.github.sonus21.rqueue.core.RqueueMessageEnqueuer;
import io.wkrzywiec.fooddelivery.bff.domain.inbox.Inbox;
import io.wkrzywiec.fooddelivery.bff.domain.inbox.InboxMessageProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("redis-inbox")
public class RedisInboxConfig {

    @Bean
    public Inbox redisInboxPublisher(RqueueMessageEnqueuer redisQueue) {
        return new RedisInbox(redisQueue);
    }

    @Bean
    public RedisInboxListener redisInboxListener(InboxMessageProcessor inboxMessageProcessor) {
        return new RedisInboxListener(inboxMessageProcessor);
    }

    @Bean
    public RedisQueueCreator redisQueueCreator(RqueueEndpointManager rqueueEndpointManager) {
        return new RedisQueueCreator(rqueueEndpointManager);
    }
}
