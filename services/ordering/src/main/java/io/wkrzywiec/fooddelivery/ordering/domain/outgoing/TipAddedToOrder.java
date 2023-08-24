package io.wkrzywiec.fooddelivery.ordering.domain.outgoing;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

import java.math.BigDecimal;

public record TipAddedToOrder(String orderId, int version, BigDecimal tip, BigDecimal total) implements DomainMessageBody {
}
