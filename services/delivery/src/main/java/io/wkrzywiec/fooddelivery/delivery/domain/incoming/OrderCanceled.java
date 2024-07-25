package io.wkrzywiec.fooddelivery.delivery.domain.incoming;

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;

import java.util.UUID;

public record OrderCanceled(UUID orderId, int version, String reason) implements IntegrationMessageBody {
}
