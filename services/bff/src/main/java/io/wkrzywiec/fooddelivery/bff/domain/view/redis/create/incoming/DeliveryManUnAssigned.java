package io.wkrzywiec.fooddelivery.bff.domain.view.redis.create.incoming;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

public record DeliveryManUnAssigned(String orderId, int version, String deliveryManId) implements DomainMessageBody {
}
