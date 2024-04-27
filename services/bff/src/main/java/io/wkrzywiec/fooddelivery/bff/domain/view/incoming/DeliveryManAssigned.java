package io.wkrzywiec.fooddelivery.bff.domain.view.incoming;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

public record DeliveryManAssigned(String orderId, int version, String deliveryManId) implements DomainMessageBody {
}
