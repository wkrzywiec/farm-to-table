package io.wkrzywiec.fooddelivery.commons.infra.store;

import java.util.List;
import java.util.UUID;

public interface EventStore {

    List<EventEntity> loadEvents(String channel, UUID streamId);

    void store(EventEntity event) throws InvalidEventVersionException;

    default void store(List<EventEntity> events) throws InvalidEventVersionException {
        events.forEach(this::store);
    }
}
