package io.wkrzywiec.fooddelivery.delivery.infra.store;

import io.wkrzywiec.fooddelivery.commons.infra.store.DomainEvent;
import io.wkrzywiec.fooddelivery.commons.infra.store.EventClassTypeProvider;
import io.wkrzywiec.fooddelivery.delivery.domain.DeliveryEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class DeliveryEventClassTypeProvider implements EventClassTypeProvider {

    @Override
    public Class<? extends DomainEvent> getClassType(String type) {
        return switch (type) {
            case "DeliveryCreated" -> DeliveryEvent.DeliveryCreated.class;
            case "TipAddedToDelivery" -> DeliveryEvent.TipAddedToDelivery.class;
            case "DeliveryCanceled" -> DeliveryEvent.DeliveryCanceled.class;
            case "FoodInPreparation" -> DeliveryEvent.FoodInPreparation.class;
            case "DeliveryManAssigned" -> DeliveryEvent.DeliveryManAssigned.class;
            case "DeliveryManUnAssigned" -> DeliveryEvent.DeliveryManUnAssigned.class;
            case "FoodIsReady" -> DeliveryEvent.FoodIsReady.class;
            case "FoodWasPickedUp" -> DeliveryEvent.FoodWasPickedUp.class;
            case "FoodDelivered" -> DeliveryEvent.FoodDelivered.class;
            default -> {
                log.error("There is not logic for mapping {} event from a store", type);
                throw new IllegalArgumentException();
            }
        };
    }
}
