package io.wkrzywiec.fooddelivery.delivery.domain.outgoing;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

public record FoodWasPickedUp(String orderId, int version) implements DomainMessageBody {
}
