package io.wkrzywiec.fooddelivery.delivery.domain.outgoing;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;
import io.wkrzywiec.fooddelivery.delivery.domain.incoming.Item;

import java.math.BigDecimal;
import java.util.List;

public record DeliveryCreated (String orderId, String customerId, String farmId, String address, List<Item> items, BigDecimal deliveryCharge, BigDecimal total) implements DomainMessageBody {
}
