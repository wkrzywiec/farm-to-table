package io.wkrzywiec.fooddelivery.delivery.domain.outgoing;

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;

public record FoodInPreparation(String orderId, int version) implements IntegrationMessageBody {
}
