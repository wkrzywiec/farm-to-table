package io.wkrzywiec.fooddelivery.bff.view.read;

import io.wkrzywiec.fooddelivery.bff.view.create.DeliveryView;

import java.util.List;
import java.util.Optional;

public interface DeliveryViewRepository {
    List<DeliveryView> getAllDeliveryViews();
    Optional<DeliveryView> getDeliveryViewById(String orderId);

}
