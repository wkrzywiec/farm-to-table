package io.wkrzywiec.fooddelivery.ordering.domain.outgoing;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

public record OrderProcessingError(String orderId, int version, String message, String details) implements DomainMessageBody {
}
