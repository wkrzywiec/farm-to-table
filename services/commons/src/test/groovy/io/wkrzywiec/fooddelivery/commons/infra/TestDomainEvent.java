package io.wkrzywiec.fooddelivery.commons.infra;

import io.wkrzywiec.fooddelivery.commons.infra.store.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TestDomainEvent(UUID streamId, int version, double number, BigDecimal money, boolean truth, Instant time, String text) implements DomainEvent {

    public static TestDomainEvent aSampleEvent(Instant time) {
        return aSampleEvent(UUID.randomUUID(), 0, time);
    }

    public static TestDomainEvent aSampleEvent(UUID streamId, Instant time) {
        return aSampleEvent(streamId, 0, time);
    }

    public static TestDomainEvent aSampleEvent(UUID streamId, int version, Instant time) {
        return new TestDomainEvent(
                streamId, version, 88.23,
                BigDecimal.valueOf(1.23), true,
                time, "some text");
    }
}
