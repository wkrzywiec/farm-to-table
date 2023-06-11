package io.wkrzywiec.fooddelivery.delivery.domain.outgoing;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

public record DeliveryManAssigned(String orderId, String deliveryManId) implements DomainMessageBody {
}
