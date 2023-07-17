package io.wkrzywiec.fooddelivery.bff.view.create.incoming;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

import java.math.BigDecimal;

public record TipAddedToDelivery(String orderId, BigDecimal tip, BigDecimal total) implements DomainMessageBody {
}
