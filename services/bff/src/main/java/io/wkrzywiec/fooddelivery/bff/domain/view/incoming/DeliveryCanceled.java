package io.wkrzywiec.fooddelivery.bff.domain.view.incoming;

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;

public record DeliveryCanceled(String orderId, int version, String reason) implements IntegrationMessageBody {
}
