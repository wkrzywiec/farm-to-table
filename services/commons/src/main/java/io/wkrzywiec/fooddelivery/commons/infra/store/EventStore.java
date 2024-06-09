package io.wkrzywiec.fooddelivery.commons.infra.store;

import io.wkrzywiec.fooddelivery.commons.infra.messaging.Message;

import java.util.List;

public interface EventStore {

    List<Message> getEventsForOrder(String orderId);
    void store(Message event);
    default void store(List<Message> events) {
        events.forEach(this::store);
    }
}
