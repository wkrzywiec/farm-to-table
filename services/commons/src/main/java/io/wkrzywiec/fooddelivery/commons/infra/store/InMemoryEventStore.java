package io.wkrzywiec.fooddelivery.commons.infra.store;

import io.wkrzywiec.fooddelivery.commons.infra.messaging.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryEventStore implements EventStore {

    Map<String, List<Message>> store = new ConcurrentHashMap<>();

    @Override
    public void store(Message event) {
        var stream = store.getOrDefault(event.body().orderId(), new ArrayList<>());
        stream.add(event);
        store.put(event.body().orderId(), stream);
    }

    @Override
    public List<Message> getEventsForOrder(String orderId) {
        return store.getOrDefault(orderId, List.of());
    }
}
