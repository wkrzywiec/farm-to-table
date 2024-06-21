package io.wkrzywiec.fooddelivery.ordering.domain.incoming;

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;

public record FoodDelivered(String orderId, int version) implements IntegrationMessageBody {
}
