package io.wkrzywiec.fooddelivery.ordering.domain;

import io.wkrzywiec.fooddelivery.commons.infra.store.DomainEvent;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface OrderingEvent extends DomainEvent {

    UUID orderId();

    @Override
    default UUID streamId() {
        return orderId();
    }

    record OrderCreated(
            UUID orderId,
            int version,
            String customerId,
            String farmId,
            String address,
            List<Item> items,
            BigDecimal deliveryCharge,
            BigDecimal total
    ) implements OrderingEvent {}

    record OrderCanceled(
            UUID orderId,
            int version,
            String reason
    ) implements OrderingEvent {}

    record OrderInProgress(
            UUID orderId,
            int version
    ) implements OrderingEvent {}

    record TipAddedToOrder(
            UUID orderId,
            int version,
            BigDecimal tip,
            BigDecimal total
    ) implements OrderingEvent {}

    record OrderCompleted(
            UUID orderId,
            int version
    ) implements OrderingEvent {}
}
