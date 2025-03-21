package io.wkrzywiec.fooddelivery.ordering.domain.incoming;

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;

import java.util.UUID;

public record FoodDelivered(UUID orderId, int version) implements IntegrationMessageBody {
}
