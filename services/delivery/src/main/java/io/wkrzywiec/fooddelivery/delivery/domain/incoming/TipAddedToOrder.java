package io.wkrzywiec.fooddelivery.delivery.domain.incoming;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

import java.math.BigDecimal;

public record TipAddedToOrder(String orderId, BigDecimal tip, BigDecimal total) implements DomainMessageBody {
}
