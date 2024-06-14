package io.wkrzywiec.fooddelivery.commons.infra.store;

import io.wkrzywiec.fooddelivery.commons.infra.messaging.IntegrationMessage;

import java.util.List;

public interface EventStore {

    List<IntegrationMessage> getEventsForOrder(String orderId);
    List<EventEntity> fetchEvents(String channel, String streamId);

    void store(IntegrationMessage event);
    default void storeMessages(List<IntegrationMessage> events) {
        events.forEach(this::store);
    }

    void store(EventEntity event);
    default void store(List<EventEntity> events) {
        events.forEach(this::store);
    }
}
