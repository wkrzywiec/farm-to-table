package io.wkrzywiec.fooddelivery.commons.event;

import java.util.UUID;

public interface IntegrationMessageBody {
    UUID orderId();
    int version();
}
