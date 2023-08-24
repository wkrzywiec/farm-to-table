package io.wkrzywiec.fooddelivery.commons.infra.store;

import io.wkrzywiec.fooddelivery.commons.infra.messaging.Message;

import java.util.List;

public interface EventStore {

    void store(Message event);
    List<Message> getEventsForOrder(String orderId);
}
