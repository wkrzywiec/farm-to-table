package io.wkrzywiec.fooddelivery.bff.view.create.incoming;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;

public record DeliveryCanceled(String orderId, String reason) implements DomainMessageBody {
}
