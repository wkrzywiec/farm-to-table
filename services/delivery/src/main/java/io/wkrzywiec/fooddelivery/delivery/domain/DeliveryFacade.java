package io.wkrzywiec.fooddelivery.delivery.domain;

import io.vavr.CheckedRunnable;
import io.vavr.control.Try;
import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;
import io.wkrzywiec.fooddelivery.commons.infra.store.DomainEvent;
import io.wkrzywiec.fooddelivery.commons.infra.store.EventEntity;
import io.wkrzywiec.fooddelivery.commons.model.*;
import io.wkrzywiec.fooddelivery.commons.infra.store.EventStore;
import io.wkrzywiec.fooddelivery.delivery.domain.incoming.*;
import io.wkrzywiec.fooddelivery.delivery.domain.outgoing.*;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Header;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.IntegrationMessage;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.MessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import static io.wkrzywiec.fooddelivery.commons.infra.messaging.IntegrationMessage.integrationEvents;
import static io.wkrzywiec.fooddelivery.commons.infra.store.EventEntity.newEventEntities;
import static java.lang.String.format;

@RequiredArgsConstructor
@Slf4j
@Component
public class DeliveryFacade {

    public static final String ORDERS_CHANNEL = "orders";
    private final EventStore eventStore;
    private final MessagePublisher publisher;
    private final Clock clock;

    public void handle(OrderCreated orderCreated) {
        log.info("Preparing a delivery for an '{}' order.", orderCreated.orderId());

        Delivery newDelivery = Delivery.from(orderCreated, clock.instant());
        storeAndPublishEvents(newDelivery);

        log.info("New delivery with an orderId: '{}' was created", newDelivery.getOrderId());
    }

    public void handle(TipAddedToOrder tipAddedToOrder) {
        log.info("Starting adding tip for '{}' delivery", tipAddedToOrder.orderId());

        var delivery = findDelivery1(tipAddedToOrder.orderId());
        delivery.addTip(tipAddedToOrder.tip(), tipAddedToOrder.total());
        storeAndPublishEvents(delivery);

        //todo add "Failed to add tip."
        log.info("Tip was added for '{}' delivery", tipAddedToOrder.orderId());
    }

    public void handle(OrderCanceled orderCanceled) {
        log.info("'{}' order was canceled. Canceling delivery", orderCanceled.orderId());

        var delivery = findDelivery1(orderCanceled.orderId());
        delivery.cancel(orderCanceled.reason(), clock.instant());
        storeAndPublishEvents(delivery);

        log.info("Delivery for '{}' order was canceled", orderCanceled.orderId());
    }

    public void handle(PrepareFood prepareFood) {
        log.info("Starting food preparation for '{}' delivery", prepareFood.orderId());

        var delivery = findDelivery(prepareFood.orderId());

        process(
                delivery,
                () -> delivery.foodInPreparation(clock.instant()),
                new FoodInPreparation(delivery.getOrderId(), prepareFood.version() + 1),
                "Failed to start food preparation."
        );
    }

    public void handle(AssignDeliveryMan assignDeliveryMan) {
        log.info("Assigning a delivery man with id: '{}' to an '{}' order", assignDeliveryMan.deliveryManId(), assignDeliveryMan.orderId());

        var delivery = findDelivery(assignDeliveryMan.orderId());

        process(
                delivery,
                () -> delivery.assignDeliveryMan(assignDeliveryMan.deliveryManId()),
                new DeliveryManAssigned(delivery.getOrderId(), assignDeliveryMan.version() + 1, assignDeliveryMan.deliveryManId()),
                "Failed to assign delivery man."
        );
    }

    public void handle(UnAssignDeliveryMan unAssignDeliveryMan) {
        log.info("Unassigning a delivery man from a '{}' delivery", unAssignDeliveryMan.orderId());

        var delivery = findDelivery(unAssignDeliveryMan.orderId());

        process(
                delivery,
                delivery::unAssignDeliveryMan,
                new DeliveryManUnAssigned(delivery.getOrderId(), unAssignDeliveryMan.version() + 1, delivery.getDeliveryManId()),
                "Failed to un assign delivery man."
        );
    }


    public void handle(FoodReady foodReady) {
        log.info("Starting food ready for '{}' delivery", foodReady.orderId());

        var delivery = findDelivery(foodReady.orderId());
        var currentVersion = getLatestVersionOfStream(foodReady.orderId());

        process(
                delivery,
                () -> delivery.foodReady(clock.instant()),
                new FoodIsReady(delivery.getOrderId(), currentVersion + 1),
                "Failed to set food as ready."
        );
    }

    public void handle(PickUpFood pickUpFood) {
        log.info("Starting picking up food for '{}' delivery", pickUpFood.orderId());

        var delivery = findDelivery(pickUpFood.orderId());

        process(
                delivery,
                () -> delivery.pickUpFood(clock.instant()),
                new FoodWasPickedUp(delivery.getOrderId(), pickUpFood.version() + 1),
                "Failed to set food as picked up."
        );
    }

    public void handle(DeliverFood deliverFood) {
        log.info("Starting delivering food for '{}' delivery", deliverFood.orderId());

        var delivery = findDelivery(deliverFood.orderId());

        process(
                delivery,
                () -> delivery.deliverFood(clock.instant()),
                new FoodDelivered(delivery.getOrderId(), deliverFood.version() + 1  ),
                "Failed to set food as delivered."
        );
    }

    private Delivery findDelivery(String orderId) {
        var storedEvents = eventStore.getEventsForOrder(orderId);
        if (storedEvents.isEmpty()) {
            throw new DeliveryException(format("There is no delivery with an orderId '%s'.", orderId));
        }
        return Delivery.from(storedEvents);
    }

    private Delivery findDelivery1(String orderId) {
        var storedEvents = eventStore.fetchEvents(ORDERS_CHANNEL, orderId);
        if (storedEvents.isEmpty()) {
            throw new DeliveryException(format("There is no delivery with an orderId '%s'.", orderId));
        }
        return Delivery.from1(storedEvents.stream().map(eventEntity -> (DeliveryEvent) eventEntity.data()).toList());
    }

    private void storeAndPublishEvents(Delivery delivery) {
        List<EventEntity> eventEntities = storeUncommittedEvents(delivery);
        publishIntegrationEvents(eventEntities);
    }

    private void publishIntegrationEvents(List<EventEntity> eventEntities) {
        List<IntegrationMessage> integrationEvents = integrationEvents(eventEntities, DeliveryEventMapper.INSTANCE);
        publisher.send(integrationEvents);
    }

    private List<EventEntity> storeUncommittedEvents(Delivery delivery) {
        List<DomainEvent> domainEvents = delivery.uncommittedChanges();
        List<EventEntity> eventEntities = newEventEntities(domainEvents, ORDERS_CHANNEL, clock);
        eventStore.store(eventEntities);
        return eventEntities;
    }

    private int getLatestVersionOfStream(String orderId) {
        return eventStore.getEventsForOrder(orderId).size();
    }

    private void process(Delivery delivery, CheckedRunnable runProcess, IntegrationMessageBody successEvent, String failureMessage) {
        Try.run(runProcess)
                .onSuccess(v -> publishSuccessEvent(delivery.getOrderId(), successEvent))
                .onFailure(ex -> publishingFailureEvent(delivery.getOrderId(), failureMessage, ex));
    };

    private void publishSuccessEvent(String orderId, IntegrationMessageBody eventObject) {
        log.info("Publishing success event: {}", eventObject);
        IntegrationMessage event = resultingEvent(orderId, eventObject);
        eventStore.store(event);
        publisher.send(event);
    }

    private void publishingFailureEvent(String id, String message, Throwable ex) {
        log.error(message + " Publishing DeliveryProcessingError event", ex);
        int version = getLatestVersionOfStream(id);
        IntegrationMessage event = resultingEvent(id, new DeliveryProcessingError(id, version, message, ex.getLocalizedMessage()));
        publisher.send(event);
    }

    private IntegrationMessage resultingEvent(String orderId, IntegrationMessageBody eventBody) {
        return new IntegrationMessage(eventHeader(orderId, eventBody.version(), eventBody.getClass().getSimpleName()), eventBody);
    }

//    private IntegrationMessage resultingEvent(DomainEvent domainEvent) {
//        return new IntegrationMessage(eventHeader(domainEvent.streamId(), domainEvent.version(), domainEvent.getClass().getSimpleName()), eventBody);
//    }

    private Header eventHeader(String orderId, int version, String type) {
        return new Header(UUID.randomUUID().toString(), version, ORDERS_CHANNEL, type, orderId, clock.instant());
    }
}
