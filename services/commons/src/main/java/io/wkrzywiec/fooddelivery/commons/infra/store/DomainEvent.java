package io.wkrzywiec.fooddelivery.commons.infra.store;

import java.util.UUID;

public interface DomainEvent {
    UUID streamId();
    int version();
}

