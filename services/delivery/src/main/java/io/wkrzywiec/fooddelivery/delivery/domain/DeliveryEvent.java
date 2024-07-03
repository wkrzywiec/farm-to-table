package io.wkrzywiec.fooddelivery.delivery.domain;

import io.wkrzywiec.fooddelivery.commons.infra.store.DomainEvent;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface DeliveryEvent extends DomainEvent {

    UUID orderId();

    @Override
    default UUID streamId() {
        return orderId();
    }

    record DeliveryCreated(
            UUID orderId,
            int version,
            String customerId,
            String farmId,
            String address,
            List<Item> items,
            BigDecimal deliveryCharge,
            BigDecimal total
    ) implements DeliveryEvent {}

    record TipAddedToDelivery(
            UUID orderId,
            int version,
            BigDecimal tip,
            BigDecimal total
    ) implements DeliveryEvent {}

    record DeliveryCanceled(
            UUID orderId,
            int version,
            String reason
    ) implements DeliveryEvent {}

    record DeliveryManAssigned(
            UUID orderId,
            int version,
            String deliveryManId
    ) implements DeliveryEvent {}

    record DeliveryManUnAssigned(
            UUID orderId,
            int version,
            String deliveryManId
    ) implements DeliveryEvent {}

    record FoodInPreparation(
            UUID orderId,
            int version
    ) implements DeliveryEvent {}

    record FoodIsReady(
            UUID orderId,
            int version
    ) implements DeliveryEvent {}

    record FoodWasPickedUp(
            UUID orderId,
            int version
    ) implements DeliveryEvent {}

    record FoodDelivered(
            UUID orderId,
            int version
    ) implements DeliveryEvent {}
}
