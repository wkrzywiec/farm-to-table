package io.wkrzywiec.fooddelivery.commons.infra;

import io.wkrzywiec.fooddelivery.commons.infra.store.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;

public record TestDomainEvent(String streamId, int version, double number, BigDecimal money, boolean truth, Instant time) implements DomainEvent {

    public static TestDomainEvent aSampleEvent(Instant time) {
        return new TestDomainEvent(
                "some test text", 1, 88.23,
                BigDecimal.valueOf(1.23), true,
                time);
    }
}
