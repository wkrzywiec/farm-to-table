package io.wkrzywiec.fooddelivery.commons.model;

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;

public record CancelOrder(String orderId, int version, String reason) implements IntegrationMessageBody {
}
