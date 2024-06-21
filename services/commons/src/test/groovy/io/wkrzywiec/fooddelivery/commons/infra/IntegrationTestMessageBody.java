package io.wkrzywiec.fooddelivery.commons.infra;

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;

import java.math.BigDecimal;
import java.time.Instant;

public record IntegrationTestMessageBody(String orderId, int version, double number, BigDecimal money, boolean truth, Instant time) implements IntegrationMessageBody {

    public static IntegrationTestMessageBody aSampleEvent(Instant time) {
        return new IntegrationTestMessageBody(
                "some test text", 1, 88.23,
                BigDecimal.valueOf(1.23), true,
                time);
    }
}
