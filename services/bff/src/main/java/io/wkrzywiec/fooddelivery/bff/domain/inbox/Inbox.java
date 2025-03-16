package io.wkrzywiec.fooddelivery.bff.domain.inbox;

public interface Inbox {

    void storeMessage(String channel, Object message) throws RuntimeException;
}
