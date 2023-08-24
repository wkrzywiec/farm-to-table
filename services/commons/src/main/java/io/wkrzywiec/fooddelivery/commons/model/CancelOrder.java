package io.wkrzywiec.fooddelivery.commons.model;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

public record CancelOrder(String orderId, int version, String reason) implements DomainMessageBody {
}
