package io.wkrzywiec.fooddelivery.ordering.domain.outgoing;

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;

import java.util.UUID;

public record OrderCompleted(UUID orderId, int version) implements IntegrationMessageBody {
}
