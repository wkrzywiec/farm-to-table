package io.wkrzywiec.fooddelivery.ordering.domain.ports;

import io.wkrzywiec.fooddelivery.ordering.domain.Order;

import java.util.Optional;

public interface OrderingRepository {

    Order save(Order newOrder);
    Optional<Order> findById(String id);
}
