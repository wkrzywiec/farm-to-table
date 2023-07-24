package io.wkrzywiec.fooddelivery.bff.inbox.redis;

import com.github.sonus21.rqueue.core.RqueueMessageEnqueuer;
import io.wkrzywiec.fooddelivery.bff.inbox.Inbox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class RedisInbox implements Inbox {

    private final RqueueMessageEnqueuer redisQueue;

    @Override
    public void storeMessage(String inbox, Object message) {
        redisQueue.enqueue(inbox, message);
    }
}
