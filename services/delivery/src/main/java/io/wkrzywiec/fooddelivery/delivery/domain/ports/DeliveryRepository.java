package io.wkrzywiec.fooddelivery.delivery.domain.ports;

import io.wkrzywiec.fooddelivery.delivery.domain.Delivery;

import java.util.Optional;

public interface DeliveryRepository {

    Delivery save(Delivery delivery);
    Optional<Delivery> findByOrderId(String orderId);
}
