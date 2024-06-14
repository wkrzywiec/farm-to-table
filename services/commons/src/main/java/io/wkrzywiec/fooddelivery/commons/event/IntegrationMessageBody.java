package io.wkrzywiec.fooddelivery.commons.event;

public interface IntegrationMessageBody {
    String orderId();
    int version();
}
