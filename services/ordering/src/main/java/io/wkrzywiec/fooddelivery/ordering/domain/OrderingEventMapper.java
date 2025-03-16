package io.wkrzywiec.fooddelivery.ordering.domain;

import io.wkrzywiec.fooddelivery.commons.event.IntegrationEventMapper;
import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;
import io.wkrzywiec.fooddelivery.commons.infra.store.DomainEvent;
import io.wkrzywiec.fooddelivery.ordering.domain.outgoing.*;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import static java.lang.String.format;

@Mapper
public interface OrderingEventMapper extends IntegrationEventMapper {

    OrderingEventMapper INSTANCE = Mappers.getMapper( OrderingEventMapper.class );

    default IntegrationMessageBody map(DomainEvent domainEvent) {
        return switch (domainEvent) {
            case OrderingEvent.OrderCreated de -> map(de);
            case OrderingEvent.OrderCanceled de -> map(de);
            case OrderingEvent.OrderInProgress de -> map(de);
            case OrderingEvent.TipAddedToOrder de -> map(de);
            case OrderingEvent.OrderCompleted de -> map(de);

            default -> throw new IllegalArgumentException(format("Failed to map DomainEvent to IntegrationMessageBody. Unknown DomainEvent: %s", domainEvent.getClass()));
        };
    }

    OrderCreated map(OrderingEvent.OrderCreated created);
    OrderCanceled map(OrderingEvent.OrderCanceled canceled);
    OrderInProgress map(OrderingEvent.OrderInProgress inProgress);
    TipAddedToOrder map(OrderingEvent.TipAddedToOrder tipAdded);
    OrderCompleted map(OrderingEvent.OrderCompleted completed);
}
