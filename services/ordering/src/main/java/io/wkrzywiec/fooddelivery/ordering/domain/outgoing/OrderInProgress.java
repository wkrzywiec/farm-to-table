package io.wkrzywiec.fooddelivery.ordering.domain.outgoing;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

public record OrderInProgress(String orderId) implements DomainMessageBody {
}
