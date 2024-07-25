package io.wkrzywiec.fooddelivery.delivery.domain.incoming;

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;

import java.math.BigDecimal;
import java.util.UUID;

public record TipAddedToOrder(UUID orderId, int version, BigDecimal tip, BigDecimal total) implements IntegrationMessageBody {
}
