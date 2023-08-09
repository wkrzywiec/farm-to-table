package io.wkrzywiec.fooddelivery.commons.infra.messaging;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

import java.math.BigDecimal;
import java.time.Instant;

public record MessageTestBody(String orderId, Instant createdAt, BigDecimal value) implements DomainMessageBody {
}
