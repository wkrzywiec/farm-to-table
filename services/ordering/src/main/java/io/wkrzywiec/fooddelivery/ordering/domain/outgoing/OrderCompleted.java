package io.wkrzywiec.fooddelivery.ordering.domain.outgoing;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

public record OrderCompleted(String orderId, int version) implements DomainMessageBody {
}
