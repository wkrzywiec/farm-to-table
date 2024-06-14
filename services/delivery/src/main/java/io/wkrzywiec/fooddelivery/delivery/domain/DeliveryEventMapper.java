package io.wkrzywiec.fooddelivery.delivery.domain;

import io.wkrzywiec.fooddelivery.commons.event.IntegrationEventMapper;
import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;
import io.wkrzywiec.fooddelivery.commons.infra.store.DomainEvent;
import io.wkrzywiec.fooddelivery.delivery.domain.outgoing.DeliveryCanceled;
import io.wkrzywiec.fooddelivery.delivery.domain.outgoing.DeliveryCreated;
import io.wkrzywiec.fooddelivery.delivery.domain.outgoing.TipAddedToDelivery;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import static java.lang.String.format;

@Mapper
public interface DeliveryEventMapper extends IntegrationEventMapper {

    DeliveryEventMapper INSTANCE = Mappers.getMapper( DeliveryEventMapper.class );

    default IntegrationMessageBody map(DomainEvent domainEvent) {
        return switch (domainEvent) {
            case DeliveryEvent.DeliveryCreated de -> map(de);
            case DeliveryEvent.TipAddedToDelivery de -> map(de);
            case DeliveryEvent.DeliveryCanceled de -> map(de);
            default -> throw new IllegalArgumentException(format("Failed to map DomainEvent to IntegrationMessageBody. Unknown DomainEvent: %s", domainEvent.getClass()));
        };
    }

    DeliveryCreated map(DeliveryEvent.DeliveryCreated deliveryCreated);
    TipAddedToDelivery map(DeliveryEvent.TipAddedToDelivery tipAdded);
    DeliveryCanceled map(DeliveryEvent.DeliveryCanceled canceled);
}
