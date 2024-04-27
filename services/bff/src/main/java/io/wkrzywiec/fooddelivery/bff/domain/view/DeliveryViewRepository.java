package io.wkrzywiec.fooddelivery.bff.domain.view;

import java.util.List;
import java.util.Optional;

public interface DeliveryViewRepository {
    List<DeliveryView> getAllDeliveryViews();
    Optional<DeliveryView> getDeliveryViewById(String orderId);

}
