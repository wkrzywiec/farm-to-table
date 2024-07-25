package io.wkrzywiec.fooddelivery.delivery.domain.outgoing;

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;

import java.util.UUID;

public record FoodWasPickedUp(UUID orderId, int version) implements IntegrationMessageBody {
}
