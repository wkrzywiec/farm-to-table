package io.wkrzywiec.fooddelivery.bff.domain.view.incoming;

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;

import java.util.UUID;

public record DeliveryManUnAssigned(UUID orderId, int version, String deliveryManId) implements IntegrationMessageBody {
}
