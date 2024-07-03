package io.wkrzywiec.fooddelivery.commons.infra;

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record IntegrationTestMessageBody(UUID orderId, int version, double number, BigDecimal money, boolean truth, Instant time, String text) implements IntegrationMessageBody {

    public static IntegrationTestMessageBody aSampleEvent(Instant time) {
        return new IntegrationTestMessageBody(
                UUID.randomUUID(), 1, 88.23,
                BigDecimal.valueOf(1.23), true,
                time, "some text");
    }
}
