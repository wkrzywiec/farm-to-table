package io.wkrzywiec.fooddelivery.delivery.domain.incoming;

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;

import java.math.BigDecimal;

public record TipAddedToOrder(String orderId, int version, BigDecimal tip, BigDecimal total) implements IntegrationMessageBody {
}
