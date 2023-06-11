package io.wkrzywiec.fooddelivery.delivery.domain.incoming;

public record CancelDelivery(String orderId, String reason) {
}
