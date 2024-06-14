package io.wkrzywiec.fooddelivery.delivery.domain.outgoing;

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;

public record DeliveryCanceled(String orderId, int version, String reason) implements IntegrationMessageBody {
}
