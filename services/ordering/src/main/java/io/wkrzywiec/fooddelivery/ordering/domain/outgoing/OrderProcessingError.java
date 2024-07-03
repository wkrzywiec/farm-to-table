package io.wkrzywiec.fooddelivery.ordering.domain.outgoing;

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;

import java.util.UUID;

public record OrderProcessingError(UUID orderId, int version, String message, String details) implements IntegrationMessageBody {
}
