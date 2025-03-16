package io.wkrzywiec.fooddelivery.commons.model;

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;

import java.util.UUID;

public record AssignDeliveryMan(UUID orderId, int version, String deliveryManId) implements IntegrationMessageBody {
}
