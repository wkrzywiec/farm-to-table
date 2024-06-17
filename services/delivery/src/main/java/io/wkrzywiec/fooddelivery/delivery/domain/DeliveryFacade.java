package io.wkrzywiec.fooddelivery.delivery.domain;

import io.vavr.CheckedRunnable;
import io.vavr.control.Try;
import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;
import io.wkrzywiec.fooddelivery.commons.infra.store.DomainEvent;
import io.wkrzywiec.fooddelivery.commons.infra.store.EventEntity;
import io.wkrzywiec.fooddelivery.commons.model.*;
import io.wkrzywiec.fooddelivery.commons.infra.store.EventStore;
import io.wkrzywiec.fooddelivery.delivery.domain.incoming.*;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Header;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.IntegrationMessage;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.MessagePublisher;
import io.wkrzywiec.fooddelivery.delivery.domain.outgoing.DeliveryProcessingError;
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
        process(
                orderCreated.orderId(),
                () -> createDelivery(orderCreated),
                "Failed to create a delivery."
        );
    }

    private void createDelivery(OrderCreated orderCreated) {
        log.info("Preparing a delivery for an '{}' order.", orderCreated.orderId());

        Delivery newDelivery = Delivery.from(orderCreated);
        storeAndPublishEvents(newDelivery);
        log.info("New delivery with an orderId: '{}' was created", newDelivery.getOrderId());
    }

    public void handle(TipAddedToOrder tipAddedToOrder) {
        process(
                tipAddedToOrder.orderId(),
                () -> addTip(tipAddedToOrder),
                "Failed to add tip."
        );
    }

    private void addTip(TipAddedToOrder tipAddedToOrder) {
        log.info("Starting adding tip for '{}' delivery", tipAddedToOrder.orderId());

        var delivery = findDelivery(tipAddedToOrder.orderId());
        delivery.addTip(tipAddedToOrder.tip(), tipAddedToOrder.total());
        storeAndPublishEvents(delivery);

        log.info("Tip was added for '{}' delivery", tipAddedToOrder.orderId());
    }

    public void handle(OrderCanceled orderCanceled) {
        process(
                orderCanceled.orderId(),
                () -> cancelOrder(orderCanceled),
                "Failed to cancel a delivery"
        );
    }

    private void cancelOrder(OrderCanceled orderCanceled) {
        log.info("'{}' order was canceled. Canceling delivery", orderCanceled.orderId());

        var delivery = findDelivery(orderCanceled.orderId());
        delivery.cancel(orderCanceled.reason(), clock.instant());
        storeAndPublishEvents(delivery);

        log.info("Delivery for '{}' order was canceled", orderCanceled.orderId());
    }

    public void handle(PrepareFood prepareFood) {
        process(
                prepareFood.orderId(),
                () -> prepareFood(prepareFood),
                "Failed to start food preparation."
        );
    }

    private void prepareFood(PrepareFood prepareFood) {
        log.info("Starting food preparation for '{}' delivery", prepareFood.orderId());

        var delivery = findDelivery(prepareFood.orderId());
        delivery.foodInPreparation(clock.instant());
        storeAndPublishEvents(delivery);

        log.info("Food in preparation for '{}' delivery", prepareFood.orderId());
    }

    public void handle(AssignDeliveryMan assignDeliveryMan) {
        process(
                assignDeliveryMan.orderId(),
                () -> assignDeliveryMan(assignDeliveryMan),
                "Failed to assign delivery man."
        );
    }

    private void assignDeliveryMan(AssignDeliveryMan assignDeliveryMan) {
        log.info("Assigning a delivery man with id: '{}' to an '{}' order", assignDeliveryMan.deliveryManId(), assignDeliveryMan.orderId());

        var delivery = findDelivery(assignDeliveryMan.orderId());
        delivery.assignDeliveryMan(assignDeliveryMan.deliveryManId());
        storeAndPublishEvents(delivery);

        log.info("A delivery man was assigned for '{}' delivery", assignDeliveryMan.orderId());
    }

    public void handle(UnAssignDeliveryMan unAssignDeliveryMan) {
        process(
                unAssignDeliveryMan.orderId(),
                () -> unAssignDeliveryMan(unAssignDeliveryMan),
                "Failed to un assign delivery man."
        );
    }

    private void unAssignDeliveryMan(UnAssignDeliveryMan unAssignDeliveryMan) {
        log.info("Unassigning a delivery man from a '{}' delivery", unAssignDeliveryMan.orderId());

        var delivery = findDelivery(unAssignDeliveryMan.orderId());
        delivery.unAssignDeliveryMan();
        storeAndPublishEvents(delivery);

        log.info("A delivery man was unassigned from '{}' delivery", unAssignDeliveryMan.orderId());
    }


    public void handle(FoodReady foodReady) {
        process(
                foodReady.orderId(),
                () -> foodReady(foodReady),
                "Failed to set food as ready."
        );
    }

    private void foodReady(FoodReady foodReady) {
        log.info("Starting food ready for '{}' delivery", foodReady.orderId());

        var delivery = findDelivery(foodReady.orderId());
        delivery.foodReady(clock.instant());
        storeAndPublishEvents(delivery);

        log.info("A food is ready for '{}' delivery", foodReady.orderId());
    }

    public void handle(PickUpFood pickUpFood) {
        process(
                pickUpFood.orderId(),
                () -> pickUpFood(pickUpFood),
                "Failed to set food as picked up."
        );
    }

    private void pickUpFood(PickUpFood pickUpFood) {
        log.info("Starting picking up food for '{}' delivery", pickUpFood.orderId());

        var delivery = findDelivery(pickUpFood.orderId());
        delivery.pickUpFood(clock.instant());
        storeAndPublishEvents(delivery);

        log.info("A food was picked up for '{}' delivery", pickUpFood.orderId());
    }

    public void handle(DeliverFood deliverFood) {
        process(
                deliverFood.orderId(),
                () -> deliverFood(deliverFood),
                "Failed to set food as delivered."
        );
    }

    private void deliverFood(DeliverFood deliverFood) {
        log.info("Starting delivering food for '{}' delivery", deliverFood.orderId());

        var delivery = findDelivery(deliverFood.orderId());
        delivery.deliverFood(clock.instant());
        storeAndPublishEvents(delivery);

        log.info("A food was delivered for '{}' order", deliverFood.orderId());
    }

    private Delivery findDelivery(String orderId) {
        var storedEvents = eventStore.fetchEvents(ORDERS_CHANNEL, orderId);
        if (storedEvents.isEmpty()) {
            throw new DeliveryException(format("There is no delivery with an orderId '%s'.", orderId));
        }
        return Delivery.from(storedEvents.stream().map(eventEntity -> (DeliveryEvent) eventEntity.data()).toList());
    }

    private void storeAndPublishEvents(Delivery delivery) {
        List<EventEntity> eventEntities = storeUncommittedEvents(delivery);
        prepareAndPublishIntegrationEvents(eventEntities);
    }

    private void prepareAndPublishIntegrationEvents(List<EventEntity> eventEntities) {
        List<IntegrationMessage> integrationEvents = integrationEvents(eventEntities, DeliveryEventMapper.INSTANCE);
        publisher.send(integrationEvents);
    }

    private List<EventEntity> storeUncommittedEvents(Delivery delivery) {
        List<DomainEvent> domainEvents = delivery.uncommittedChanges();
        List<EventEntity> eventEntities = newEventEntities(domainEvents, ORDERS_CHANNEL, clock);
        eventStore.store(eventEntities);
        return eventEntities;
    }

    private void process(String streamId, CheckedRunnable runProcess, String failureMessage) {
        Try.run(runProcess)
                .onFailure(ex -> publishingFailureEvent(streamId, failureMessage, ex));
    };

    private void publishingFailureEvent(String id, String message, Throwable ex) {
        log.error(message + " Publishing DeliveryProcessingError event", ex);
        IntegrationMessage event = resultingEvent(id, new DeliveryProcessingError(id, -1, message, ex.getLocalizedMessage()));
        publisher.send(event);
    }

    private IntegrationMessage resultingEvent(String orderId, IntegrationMessageBody eventBody) {
        return new IntegrationMessage(eventHeader(orderId, eventBody.version(), eventBody.getClass().getSimpleName()), eventBody);
    }

    private Header eventHeader(String orderId, int version, String type) {
        return new Header(UUID.randomUUID().toString(), version, ORDERS_CHANNEL, type, orderId, clock.instant());
    }
}
