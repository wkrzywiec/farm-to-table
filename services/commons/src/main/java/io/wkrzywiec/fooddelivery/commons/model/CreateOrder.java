package io.wkrzywiec.fooddelivery.commons.model;

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateOrder(
        UUID orderId,
        int version,
        String customerId,
        String farmId,
        List<Item> items,
        String address,
        BigDecimal deliveryCharge) implements IntegrationMessageBody {
}
