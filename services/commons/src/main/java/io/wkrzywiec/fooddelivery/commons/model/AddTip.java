package io.wkrzywiec.fooddelivery.commons.model;

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;

import java.math.BigDecimal;
import java.util.UUID;

public record AddTip(UUID orderId, int version, BigDecimal tip) implements IntegrationMessageBody {
}
