package io.wkrzywiec.fooddelivery.commons.infra.store;

import java.util.List;

public interface EventStore {

    List<EventEntity> fetchEvents(String channel, String streamId);

    void store(EventEntity event);

    default void store(List<EventEntity> events) {
        events.forEach(this::store);
    }
}
