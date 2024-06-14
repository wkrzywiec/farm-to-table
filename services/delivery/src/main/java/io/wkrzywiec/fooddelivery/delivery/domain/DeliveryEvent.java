package io.wkrzywiec.fooddelivery.delivery.domain;

import io.wkrzywiec.fooddelivery.commons.infra.store.DomainEvent;

import java.math.BigDecimal;
import java.util.List;

public interface DeliveryEvent extends DomainEvent {

    String orderId();

    @Override
    default String streamId() {
        return orderId();
    }

    record DeliveryCreated(
            String orderId,
            int version,
            String customerId,
            String farmId,
            String address,
            List<Item> items,
            BigDecimal deliveryCharge,
            BigDecimal total
    ) implements DeliveryEvent {}

    record TipAddedToDelivery(
            String orderId,
            int version,
            BigDecimal tip,
            BigDecimal total
    ) implements DeliveryEvent {}

    record DeliveryCanceled(
            String orderId,
            int version,
            String reason
    ) implements DeliveryEvent {}
}
