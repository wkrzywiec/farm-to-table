package io.wkrzywiec.fooddelivery.bff.domain.view.redis.create.incoming;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

public record DeliveryCanceled(String orderId, int version, String reason) implements DomainMessageBody {
}
