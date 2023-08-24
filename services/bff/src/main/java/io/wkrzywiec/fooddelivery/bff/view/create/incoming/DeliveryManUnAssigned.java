package io.wkrzywiec.fooddelivery.bff.view.create.incoming;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

public record DeliveryManUnAssigned(String orderId, int version, String deliveryManId) implements DomainMessageBody {
}
