package io.wkrzywiec.fooddelivery.ordering.domain.outgoing;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

public record OrderCanceled(String orderId, int version, String reason) implements DomainMessageBody {
}
