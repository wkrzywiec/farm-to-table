package io.wkrzywiec.fooddelivery.delivery.domain.outgoing;

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;
import io.wkrzywiec.fooddelivery.delivery.domain.incoming.Item;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record DeliveryCreated (UUID orderId, int version, String customerId, String farmId, String address, List<Item> items, BigDecimal deliveryCharge, BigDecimal total) implements IntegrationMessageBody {
}
