package io.wkrzywiec.fooddelivery.commons.model;

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;

import java.math.BigDecimal;

public record AddTip(String orderId, int version, BigDecimal tip) implements IntegrationMessageBody {
}
