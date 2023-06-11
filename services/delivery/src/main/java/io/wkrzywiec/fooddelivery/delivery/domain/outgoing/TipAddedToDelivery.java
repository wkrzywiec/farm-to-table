package io.wkrzywiec.fooddelivery.delivery.domain.outgoing;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

import java.math.BigDecimal;

public record TipAddedToDelivery(String orderId, BigDecimal tip, BigDecimal total) implements DomainMessageBody {
}
