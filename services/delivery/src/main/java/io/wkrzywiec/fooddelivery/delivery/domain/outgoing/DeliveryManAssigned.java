package io.wkrzywiec.fooddelivery.delivery.domain.outgoing;

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;

import java.util.UUID;

public record DeliveryManAssigned(UUID orderId, int version, String deliveryManId) implements IntegrationMessageBody {
}
