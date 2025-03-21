package io.wkrzywiec.fooddelivery.ordering.infra.store;

import io.wkrzywiec.fooddelivery.commons.infra.store.DomainEvent;
import io.wkrzywiec.fooddelivery.commons.infra.store.EventClassTypeProvider;
import io.wkrzywiec.fooddelivery.ordering.domain.OrderingEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class OrderingEventClassTypeProvider implements EventClassTypeProvider {

    @Override
    public Class<? extends DomainEvent> getClassType(String type) {
        return switch (type) {
            case "OrderCreated" -> OrderingEvent.OrderCreated.class;
            case "OrderCanceled" -> OrderingEvent.OrderCanceled.class;
            case "OrderInProgress" -> OrderingEvent.OrderInProgress.class;
            case "TipAddedToOrder" -> OrderingEvent.TipAddedToOrder.class;
            case "OrderCompleted" -> OrderingEvent.OrderCompleted.class;
            default -> {
                log.error("There is not logic for mapping {} event from a store", type);
                yield null;
            }
        };
    }
}
