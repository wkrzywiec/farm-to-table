package io.wkrzywiec.fooddelivery.bff.domain.view.incoming;

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;

public record DeliveryManUnAssigned(String orderId, int version, String deliveryManId) implements IntegrationMessageBody {
}
