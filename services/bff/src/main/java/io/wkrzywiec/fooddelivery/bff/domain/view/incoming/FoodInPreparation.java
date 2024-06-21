package io.wkrzywiec.fooddelivery.bff.domain.view.incoming;

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;

public record FoodInPreparation(String orderId, int version) implements IntegrationMessageBody {
}
