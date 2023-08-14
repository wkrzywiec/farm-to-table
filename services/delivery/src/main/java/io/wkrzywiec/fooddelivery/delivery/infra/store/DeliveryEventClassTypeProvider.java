package io.wkrzywiec.fooddelivery.delivery.infra.store;

import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;
import io.wkrzywiec.fooddelivery.commons.infra.store.EventClassTypeProvider;
import io.wkrzywiec.fooddelivery.delivery.domain.outgoing.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class DeliveryEventClassTypeProvider implements EventClassTypeProvider {

    @Override
    public Class<? extends DomainMessageBody> getClassType(String type) {
        return switch (type) {
            case "DeliveryCreated" -> DeliveryCreated.class;
            case "TipAddedToDelivery" -> TipAddedToDelivery.class;
            case "DeliveryCanceled" -> DeliveryCanceled.class;
            case "FoodInPreparation" -> FoodInPreparation.class;
            case "DeliveryManAssigned" -> DeliveryManAssigned.class;
            case "DeliveryManUnAssigned" -> DeliveryManUnAssigned.class;
            case "FoodIsReady" -> FoodIsReady.class;
            case "FoodWasPickedUp" -> FoodWasPickedUp.class;
            case "FoodDelivered" -> FoodDelivered.class;
            default -> {
                log.error("There is not logic for mapping {} event from a store", type);
                yield null;
            }
        };
    }
}
