package io.wkrzywiec.fooddelivery.commons.model;

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;

public record DeliverFood(String orderId, int version) implements IntegrationMessageBody {
}
