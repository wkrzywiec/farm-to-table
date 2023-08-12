package io.wkrzywiec.fooddelivery.ordering.domain.incoming;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

public record FoodInPreparation(String orderId, int version) implements DomainMessageBody {
}
