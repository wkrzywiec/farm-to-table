package io.wkrzywiec.fooddelivery.bff.domain.view.incoming;

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;

import java.util.UUID;

public record FoodIsReady(UUID orderId, int version) implements IntegrationMessageBody {
}
