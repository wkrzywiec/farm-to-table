package io.wkrzywiec.fooddelivery.commons.infra.store.inmemory;

import io.wkrzywiec.fooddelivery.commons.infra.messaging.Message;
import io.wkrzywiec.fooddelivery.commons.infra.store.EventEntity;
import io.wkrzywiec.fooddelivery.commons.infra.store.EventStore;

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
    public void store(EventEntity event) {
        //todo implement me
    }

    @Override
    public List<Message> getEventsForOrder(String orderId) {
        return store.getOrDefault(orderId, List.of());
    }

    @Override
    public List<EventEntity> fetchEventsForChannelAndStream(String streamId) {
        //todo implement me
        return List.of();
    }
}
