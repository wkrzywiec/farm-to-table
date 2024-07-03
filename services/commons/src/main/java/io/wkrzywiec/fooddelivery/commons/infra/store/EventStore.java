package io.wkrzywiec.fooddelivery.commons.infra.store;

import java.util.List;
import java.util.UUID;

public interface EventStore {

    List<EventEntity> fetchEvents(String channel, UUID streamId);

    void store(EventEntity event);

    default void store(List<EventEntity> events) {
        events.forEach(this::store);
    }
}
