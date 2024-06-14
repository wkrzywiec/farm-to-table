package io.wkrzywiec.fooddelivery.ordering.domain.outgoing;

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;

public record OrderInProgress(String orderId, int version) implements IntegrationMessageBody {
}
