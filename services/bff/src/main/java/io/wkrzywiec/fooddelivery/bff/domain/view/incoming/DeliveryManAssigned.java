package io.wkrzywiec.fooddelivery.bff.domain.view.incoming;

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;

import java.util.UUID;

public record DeliveryManAssigned(UUID orderId, int version, String deliveryManId) implements IntegrationMessageBody {
}
