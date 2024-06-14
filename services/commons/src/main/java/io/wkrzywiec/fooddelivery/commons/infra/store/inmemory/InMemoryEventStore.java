package io.wkrzywiec.fooddelivery.commons.infra.store.inmemory;

import io.wkrzywiec.fooddelivery.commons.infra.messaging.IntegrationMessage;
import io.wkrzywiec.fooddelivery.commons.infra.store.EventEntity;
import io.wkrzywiec.fooddelivery.commons.infra.store.EventStore;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class InMemoryEventStore implements EventStore {

    Map<String, List<IntegrationMessage>> storeOld = new ConcurrentHashMap<>();
    Map<String, List<EventEntity>> store = new ConcurrentHashMap<>();

    @Override
    public void store(IntegrationMessage event) {
        var stream = storeOld.getOrDefault(event.body().orderId(), new ArrayList<>());
        stream.add(event);
        storeOld.put(event.body().orderId(), stream);
    }

    @Override
    public void store(EventEntity event) {
        log.info("Persisting '{}' event in event store. {}", event.type(), event);
        var stream = store.getOrDefault(event.streamId(), new ArrayList<>());
        stream.add(event);
        store.put(event.streamId(), stream);
    }

    @Override
    public List<IntegrationMessage> getEventsForOrder(String orderId) {
        return storeOld.getOrDefault(orderId, List.of());
    }

    @Override
    public List<EventEntity> fetchEvents(String channel, String streamId) {
        return store.getOrDefault(streamId, List.of());
    }
}
