package io.wkrzywiec.fooddelivery.ordering.domain.ports;

import io.wkrzywiec.fooddelivery.ordering.domain.Order;

import java.util.Optional;

public interface OrderingRepository {

    public Order save(Order newOrder);
    public Optional<Order> findById(String id);
}
