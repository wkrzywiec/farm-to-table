package io.wkrzywiec.fooddelivery.commons.infra.messaging;

import java.util.List;

public interface MessagePublisher {
    void send(IntegrationMessage message);

    default void send(List<IntegrationMessage> messages) {
        messages.forEach(this::send);
    }
}
