package io.wkrzywiec.fooddelivery.commons.model;

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;

public record FoodReady(String orderId, int version) implements IntegrationMessageBody {
}
