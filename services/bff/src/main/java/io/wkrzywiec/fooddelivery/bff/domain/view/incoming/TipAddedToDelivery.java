package io.wkrzywiec.fooddelivery.bff.domain.view.incoming;

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;

import java.math.BigDecimal;
import java.util.UUID;

public record TipAddedToDelivery(UUID orderId, int version, BigDecimal tip, BigDecimal total) implements IntegrationMessageBody {
}
