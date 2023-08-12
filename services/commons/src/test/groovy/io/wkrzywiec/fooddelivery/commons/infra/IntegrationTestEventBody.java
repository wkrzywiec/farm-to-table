package io.wkrzywiec.fooddelivery.commons.infra;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

import java.math.BigDecimal;
import java.time.Instant;

public record IntegrationTestEventBody(String orderId, int version, double number, BigDecimal money, boolean truth, Instant time) implements DomainMessageBody {

    public static IntegrationTestEventBody aSampleEvent(Instant time) {
        return new IntegrationTestEventBody(
                "some test text", 1, 88.23,
                BigDecimal.valueOf(1.23), true,
                time);
    }
}
