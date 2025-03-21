package io.wkrzywiec.fooddelivery.ordering.domain;

import io.vavr.CheckedRunnable;
import io.vavr.control.Try;
import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;
import io.wkrzywiec.fooddelivery.commons.infra.store.DomainEvent;
import io.wkrzywiec.fooddelivery.commons.infra.store.EventEntity;
import io.wkrzywiec.fooddelivery.commons.model.AddTip;
import io.wkrzywiec.fooddelivery.commons.model.CancelOrder;
import io.wkrzywiec.fooddelivery.commons.model.CreateOrder;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Header;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.IntegrationMessage;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.MessagePublisher;
import io.wkrzywiec.fooddelivery.commons.infra.store.EventStore;
import io.wkrzywiec.fooddelivery.ordering.domain.incoming.FoodDelivered;
import io.wkrzywiec.fooddelivery.ordering.domain.incoming.FoodInPreparation;
import io.wkrzywiec.fooddelivery.ordering.domain.outgoing.*;
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
public class OrderingFacade {

    public static final String ORDERS_CHANNEL =  "orders";

    private final EventStore eventStore;
    private final MessagePublisher publisher;
    private final Clock clock;

    public void handle(CreateOrder createOrder) {
        process(
                createOrder.orderId(),
                () -> createOrder(createOrder),
                "Failed to create an order."
        );

    }

    private void createOrder(CreateOrder createOrder) {
        log.info("Creating a new order: {}", createOrder);

        Order newOrder = Order.from(createOrder);
        storeAndPublishEvents(newOrder);
        log.info("New order with an id: '{}' was created", newOrder.getId());
    }

    public void handle(CancelOrder cancelOrder) {
        process(
                cancelOrder.orderId(),
                () -> cancelOrder(cancelOrder),
                "Failed to cancel an order."
        );
    }

    private void cancelOrder(CancelOrder cancelOrder) {
        log.info("Cancelling an order: {}", cancelOrder.orderId());

        var order = findOrder(cancelOrder.orderId());
        order.cancelOrder(cancelOrder.reason());
        storeAndPublishEvents(order);

        log.info("Cancellation of an order '{}' has been completed", order.getId());
    }

    public void handle(FoodInPreparation foodInPreparation) {
        process(
                foodInPreparation.orderId(),
                () -> foodInPreparation(foodInPreparation),
                "Failed to set an order to IN_PROGRESS state."
        );
    }

    private void foodInPreparation(FoodInPreparation foodInPreparation) {
        log.info("Setting '{}' order to IN_PROGRESS state", foodInPreparation.orderId());

        var order = findOrder(foodInPreparation.orderId());
        order.setInProgress();
        storeAndPublishEvents(order);

        log.info("Setting an '{}' order to IN_PROGRESS state has been completed", foodInPreparation.orderId());
    }

    public void handle(AddTip addTip) {
        process(
                addTip.orderId(),
                () -> addTip(addTip),
                "Failed to add tip to an order."
        );
    }

    private void addTip(AddTip addTip) {
        log.info("Adding {} tip to '{}' order.", addTip.tip(), addTip.orderId());

        var order = findOrder(addTip.orderId());
        order.addTip(addTip.tip());
        storeAndPublishEvents(order);

        log.info("Adding a tip to '{}' order has been completed", addTip.orderId());
    }

    public void handle(FoodDelivered foodDelivered) {
        process(
                foodDelivered.orderId(),
                () -> foodDelivered(foodDelivered),
                "Failed to complete an order."
        );
    }

    private void foodDelivered(FoodDelivered foodDelivered) {
        log.info("Setting '{}' order to COMPLETED state", foodDelivered.orderId());

        var order = findOrder(foodDelivered.orderId());
        order.complete();
        storeAndPublishEvents(order);

        log.info("Setting an '{}' order to COMPLETED state has been completed", foodDelivered.orderId());
    }

    private void process(UUID streamId, CheckedRunnable runProcess, String failureMessage) {
        Try.run(runProcess)
                .onFailure(ex -> publishingFailureEvent(streamId, failureMessage, ex));
    };

    private void publishingFailureEvent(UUID id, String message, Throwable ex) {
        log.error(message + " Publishing OrderProcessingError event", ex);
        IntegrationMessage event = resultingEvent(id, new OrderProcessingError(id, -1, message, ex.getLocalizedMessage()));
        publisher.send(event);
    }

    private IntegrationMessage resultingEvent(UUID orderId, IntegrationMessageBody eventBody) {
        return new IntegrationMessage(eventHeader(orderId, eventBody.getClass().getSimpleName(), eventBody.version()), eventBody);
    }

    private Header eventHeader(UUID orderId, String type, int version) {
        return new Header(UUID.randomUUID(), version, ORDERS_CHANNEL, type, orderId, clock.instant());
    }

    private Order findOrder(UUID orderId) {
        var storedEvents = eventStore.loadEvents(ORDERS_CHANNEL, orderId);
        if (storedEvents.isEmpty()) {
            throw new OrderingException(format("There is no order with an orderId '%s'.", orderId));
        }
        return Order.from(storedEvents.stream().map(eventEntity -> (OrderingEvent) eventEntity.data()).toList());
    }

    private void storeAndPublishEvents(Order order) {
        List<EventEntity> eventEntities = storeUncommittedEvents(order);
        prepareAndPublishIntegrationEvents(eventEntities);
    }

    private List<EventEntity> storeUncommittedEvents(Order order) {
        List<DomainEvent> domainEvents = order.uncommittedChanges();
        List<EventEntity> eventEntities = newEventEntities(domainEvents, ORDERS_CHANNEL, clock);
        eventStore.store(eventEntities);
        return eventEntities;
    }

    private void prepareAndPublishIntegrationEvents(List<EventEntity> eventEntities) {
        List<IntegrationMessage> integrationEvents = integrationEvents(eventEntities, OrderingEventMapper.INSTANCE);
        publisher.send(integrationEvents);
    }
}
