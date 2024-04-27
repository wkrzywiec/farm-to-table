package io.wkrzywiec.fooddelivery.bff.domain.view.incoming;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

public record FoodWasPickedUp(String orderId, int version) implements DomainMessageBody {
}
