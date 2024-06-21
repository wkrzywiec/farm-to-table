package io.wkrzywiec.fooddelivery.commons.model;

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;

public record AssignDeliveryMan(String orderId, int version, String deliveryManId) implements IntegrationMessageBody {
}
