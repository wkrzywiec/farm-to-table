package io.wkrzywiec.fooddelivery.delivery.domain.outgoing;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

public record DeliveryManUnAssigned(String orderId, int version, String deliveryManId) implements DomainMessageBody {
}
