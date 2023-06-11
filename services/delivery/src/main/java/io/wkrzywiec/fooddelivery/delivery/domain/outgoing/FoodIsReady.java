package io.wkrzywiec.fooddelivery.delivery.domain.outgoing;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

public record FoodIsReady(String orderId) implements DomainMessageBody {
}
