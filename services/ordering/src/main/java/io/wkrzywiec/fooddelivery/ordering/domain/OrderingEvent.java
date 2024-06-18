package io.wkrzywiec.fooddelivery.ordering.domain;

import io.wkrzywiec.fooddelivery.commons.infra.store.DomainEvent;

import java.math.BigDecimal;
import java.util.List;

public interface OrderingEvent extends DomainEvent {

    String orderId();

    @Override
    default String streamId() {
        return orderId();
    }

    record OrderCreated(
            String orderId,
            int version,
            String customerId,
            String farmId,
            String address,
            List<Item> items,
            BigDecimal deliveryCharge,
            BigDecimal total
    ) implements OrderingEvent {}

    record OrderCanceled(
            String orderId,
            int version,
            String reason
    ) implements OrderingEvent {}

    record OrderInProgress(
            String orderId,
            int version
    ) implements OrderingEvent {}

    record TipAddedToOrder(
            String orderId,
            int version,
            BigDecimal tip,
            BigDecimal total
    ) implements OrderingEvent {}

    record OrderCompleted(
            String orderId,
            int version
    ) implements OrderingEvent {}
}
