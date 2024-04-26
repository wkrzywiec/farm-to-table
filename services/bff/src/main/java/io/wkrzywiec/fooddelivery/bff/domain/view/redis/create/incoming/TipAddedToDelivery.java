package io.wkrzywiec.fooddelivery.bff.domain.view.redis.create.incoming;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

import java.math.BigDecimal;

public record TipAddedToDelivery(String orderId, int version, BigDecimal tip, BigDecimal total) implements DomainMessageBody {
}
