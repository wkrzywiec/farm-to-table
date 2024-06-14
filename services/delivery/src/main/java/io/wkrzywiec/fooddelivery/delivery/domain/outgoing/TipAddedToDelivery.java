package io.wkrzywiec.fooddelivery.delivery.domain.outgoing;

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;

import java.math.BigDecimal;

public record TipAddedToDelivery(String orderId, int version, BigDecimal tip, BigDecimal total) implements IntegrationMessageBody {
}
