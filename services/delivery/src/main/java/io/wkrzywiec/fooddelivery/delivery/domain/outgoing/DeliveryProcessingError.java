package io.wkrzywiec.fooddelivery.delivery.domain.outgoing;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

public record DeliveryProcessingError(String orderId, String message, String details) implements DomainMessageBody {
}
