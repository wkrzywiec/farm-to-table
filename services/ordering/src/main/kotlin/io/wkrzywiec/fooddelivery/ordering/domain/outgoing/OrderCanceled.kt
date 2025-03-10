package io.wkrzywiec.fooddelivery.ordering.domain.outgoing;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

public record OrderCanceled(String orderId, String reason) implements DomainMessageBody {
}
