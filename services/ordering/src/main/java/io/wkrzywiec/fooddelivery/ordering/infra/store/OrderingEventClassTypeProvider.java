package io.wkrzywiec.fooddelivery.ordering.infra.store;

import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;
import io.wkrzywiec.fooddelivery.commons.infra.store.EventClassTypeProvider;
import io.wkrzywiec.fooddelivery.ordering.domain.outgoing.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class OrderingEventClassTypeProvider implements EventClassTypeProvider {
    @Override
    public Class<? extends IntegrationMessageBody> getClassType(String type) {
        return switch (type) {
            case "OrderCreated" -> OrderCreated.class;
            case "OrderCanceled" -> OrderCanceled.class;
            case "OrderInProgress" -> OrderInProgress.class;
            case "TipAddedToOrder" -> TipAddedToOrder.class;
            case "OrderCompleted" -> OrderCompleted.class;
            default -> {
                log.error("There is not logic for mapping {} event from a store", type);
                yield null;
            }
        };
    }
}
