package io.wkrzywiec.fooddelivery.commons.model;

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;

import java.util.UUID;

public record UnAssignDeliveryMan(UUID orderId, int version) implements IntegrationMessageBody {
}
