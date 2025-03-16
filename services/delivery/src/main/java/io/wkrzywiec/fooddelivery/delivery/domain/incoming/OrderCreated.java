package io.wkrzywiec.fooddelivery.delivery.domain.incoming;

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderCreated(UUID orderId, int version, String customerId, String farmId, String address, List<Item> items, BigDecimal deliveryCharge, BigDecimal total) implements IntegrationMessageBody {
}
