package io.wkrzywiec.fooddelivery.bff.inbox.redis;

import com.github.sonus21.rqueue.core.RqueueMessageEnqueuer;
import io.wkrzywiec.fooddelivery.bff.inbox.InboxPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Slf4j
public class RedisInboxPublisher implements InboxPublisher {

    private final RqueueMessageEnqueuer redisQueue;

    @Override
    public void storeMessage(String inbox, Object message) {
        redisQueue.enqueue(inbox, message);
    }
}
