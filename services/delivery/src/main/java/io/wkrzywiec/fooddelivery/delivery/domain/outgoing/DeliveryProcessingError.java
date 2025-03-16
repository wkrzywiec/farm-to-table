package io.wkrzywiec.fooddelivery.delivery.domain.outgoing;

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;

import java.util.UUID;

public record DeliveryProcessingError(UUID orderId, int version, String message, String details) implements IntegrationMessageBody {
}
