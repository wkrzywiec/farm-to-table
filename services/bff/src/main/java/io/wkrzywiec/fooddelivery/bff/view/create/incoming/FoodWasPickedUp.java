package io.wkrzywiec.fooddelivery.bff.view.create.incoming;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

public record FoodWasPickedUp(String orderId) implements DomainMessageBody {
}
