package io.wkrzywiec.fooddelivery.commons.model;

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;

public record PickUpFood(String orderId, int version) implements IntegrationMessageBody {
}
