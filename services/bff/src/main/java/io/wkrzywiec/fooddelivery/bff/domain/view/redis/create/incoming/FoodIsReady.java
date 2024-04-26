package io.wkrzywiec.fooddelivery.bff.domain.view.redis.create.incoming;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

public record FoodIsReady(String orderId, int version) implements DomainMessageBody {
}
