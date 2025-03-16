package io.wkrzywiec.fooddelivery.commons.model;

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;

import java.util.UUID;

public record CancelOrder(UUID orderId, int version, String reason) implements IntegrationMessageBody {
}
