package io.wkrzywiec.fooddelivery.commons.model;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

public record FoodReady(String orderId, int version) implements DomainMessageBody {
}
