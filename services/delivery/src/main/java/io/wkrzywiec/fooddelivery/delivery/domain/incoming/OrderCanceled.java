package io.wkrzywiec.fooddelivery.delivery.domain.incoming;

public record OrderCanceled(String orderId, String reason) {
}
