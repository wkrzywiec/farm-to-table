package io.wkrzywiec.fooddelivery.commons.model;

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;

public record UnAssignDeliveryMan(String orderId, int version) implements IntegrationMessageBody {
}
