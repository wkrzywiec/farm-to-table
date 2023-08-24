package io.wkrzywiec.fooddelivery.delivery.domain.incoming;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

public record OrderCanceled(String orderId, int version, String reason) implements DomainMessageBody {
}
