package io.wkrzywiec.fooddelivery.delivery.domain.outgoing;

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;

public record DeliveryManAssigned(String orderId, int version, String deliveryManId) implements IntegrationMessageBody {
}
