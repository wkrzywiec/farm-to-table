package io.wkrzywiec.fooddelivery.delivery.domain.outgoing;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

public record DeliveryCanceled(String orderId, int version, String reason) implements DomainMessageBody {
}
