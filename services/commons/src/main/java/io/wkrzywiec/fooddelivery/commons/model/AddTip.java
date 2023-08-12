package io.wkrzywiec.fooddelivery.commons.model;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

import java.math.BigDecimal;

public record AddTip(String orderId, int version, BigDecimal tip) implements DomainMessageBody {
}
