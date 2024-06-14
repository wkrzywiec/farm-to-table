package io.wkrzywiec.fooddelivery.ordering.domain.outgoing;

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;

public record OrderCanceled(String orderId, int version, String reason) implements IntegrationMessageBody {
}
