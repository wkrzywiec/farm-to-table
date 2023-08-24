package io.wkrzywiec.fooddelivery.bff.inbox;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryInbox implements Inbox {

    public Map<String, Queue<Object>> inboxes = new ConcurrentHashMap<>();

    @Override
    public void storeMessage(String channel, Object message) {
        Queue<Object> inbox =  inboxes.getOrDefault(channel, new LinkedList<>());
        inbox.add(message);
        inboxes.put(channel, inbox);
    }
}
