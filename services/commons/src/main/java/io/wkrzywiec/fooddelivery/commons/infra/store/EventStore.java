package io.wkrzywiec.fooddelivery.commons.infra.store;

import io.wkrzywiec.fooddelivery.commons.infra.messaging.Message;

import java.util.List;

public interface EventStore {

    List<Message> getEventsForOrder(String orderId);
    List<EventEntity> fetchEventsForChannelAndStream(String streamId);

    void store(Message event);
    default void storeMessages(List<Message> events) {
        events.forEach(this::store);
    }

    void store(EventEntity event);
    default void store(List<EventEntity> events) {
        events.forEach(this::store);
    }
}
