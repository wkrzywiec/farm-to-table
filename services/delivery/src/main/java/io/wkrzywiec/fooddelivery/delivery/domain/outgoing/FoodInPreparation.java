package io.wkrzywiec.fooddelivery.delivery.domain.outgoing;

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;

import java.util.UUID;

public record FoodInPreparation(UUID orderId, int version) implements IntegrationMessageBody {
}
