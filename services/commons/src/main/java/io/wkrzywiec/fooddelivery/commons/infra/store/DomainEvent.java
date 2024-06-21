package io.wkrzywiec.fooddelivery.commons.infra.store;

public interface DomainEvent {
    String streamId();
    int version();
}

