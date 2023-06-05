package io.wkrzywiec.fooddelivery.delivery.domain.outgoing;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

public record DeliveryCanceled(String orderId, String reason) implements DomainMessageBody {
}
