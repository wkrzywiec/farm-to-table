package io.wkrzywiec.fooddelivery.delivery.domain.outgoing;

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;

import java.math.BigDecimal;
import java.util.UUID;

public record TipAddedToDelivery(UUID orderId, int version, BigDecimal tip, BigDecimal total) implements IntegrationMessageBody {
}
