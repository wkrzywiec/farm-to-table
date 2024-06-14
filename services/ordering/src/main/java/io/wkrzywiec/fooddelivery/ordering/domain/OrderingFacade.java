package io.wkrzywiec.fooddelivery.ordering.domain;

import io.vavr.control.Try;
import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;
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
import java.util.UUID;

import static java.lang.String.format;

@RequiredArgsConstructor
@Slf4j
@Component
public class OrderingFacade {

    private static final String ORDERS_CHANNEL =  "orders";

    private final EventStore eventStore;
    private final MessagePublisher publisher;
    private final Clock clock;

    public void handle(CreateOrder createOrder) {
        log.info("Creating a new order: {}", createOrder);
        Order newOrder = Order.from(createOrder);
        OrderCreated orderCreated = new OrderCreated(
                newOrder.getId(),
                1,
                newOrder.getCustomerId(),
                newOrder.getFarmId(),
                newOrder.getAddress(),
                createOrder.items(),
                newOrder.getDeliveryCharge(),
                newOrder.getTotal());

        IntegrationMessage event = resultingEvent(
                newOrder.getId(),
                orderCreated
        );

        eventStore.store(event);
        publisher.send(event);
        log.info("New order with an id: '{}' was created", newOrder.getId());
    }

    public void handle(CancelOrder cancelOrder) {
        log.info("Cancelling an order: {}", cancelOrder.orderId());

        var storedEvents = eventStore.getEventsForOrder(cancelOrder.orderId());
        if (storedEvents.size() == 0) {
            throw new OrderingException(format("Failed to cancel an %s order. There is no such order with provided id.", cancelOrder.orderId()));
        }
        var order = Order.from(storedEvents);

        Try.run(() -> order.cancelOrder(cancelOrder.reason()))
                .onSuccess(v -> publishSuccessEvent(order.getId(), new OrderCanceled(cancelOrder.orderId(), incrementVersionAndReturn(cancelOrder), cancelOrder.reason())))
                .onFailure(ex -> publishingFailureEvent(order.getId(), "Failed to cancel an order.", cancelOrder.version(), ex))
                .andFinally(() -> log.info("Cancellation of an order '{}' has been completed", order.getId()));
    }

    public void handle(FoodInPreparation foodInPreparation) {
        log.info("Setting '{}' order to IN_PROGRESS state", foodInPreparation.orderId());

        var storedEvents = eventStore.getEventsForOrder(foodInPreparation.orderId());
        if (storedEvents.size() == 0) {
            throw new OrderingException(format("Failed to set an '%s' order to IN_PROGRESS state. There is no such order with provided id.", foodInPreparation.orderId()));
        }
        var order = Order.from(storedEvents);

        Try.run(order::setInProgress)
                .onSuccess(v -> publishSuccessEvent(order.getId(), new OrderInProgress(foodInPreparation.orderId(), incrementVersionAndReturn(foodInPreparation))))
                .onFailure(ex -> publishingFailureEvent(order.getId(), "Failed to set an order to IN_PROGRESS state.", foodInPreparation.version(), ex))
                .andFinally(() -> log.info("Setting an '{}' order to IN_PROGRESS state has been completed", foodInPreparation.orderId()));
    }

    public void handle(AddTip addTip) {
        log.info("Adding {} tip to '{}' order.", addTip.tip(), addTip.orderId());

        var storedEvents = eventStore.getEventsForOrder(addTip.orderId());
        if (storedEvents.size() == 0) {
            throw new OrderingException(format("Failed add tip an '%s' order. There is no such order with provided id.", addTip.orderId()));
        }
        var order = Order.from(storedEvents);

        Try.run(() -> order.addTip(addTip.tip()))
                .onSuccess(v -> publishSuccessEvent(order.getId(), new TipAddedToOrder(order.getId(), incrementVersionAndReturn(addTip), order.getTip(), order.getTotal())))
                .onFailure(ex -> publishingFailureEvent(order.getId(), "Failed to add tip to an order.", addTip.version(), ex))
                .andFinally(() -> log.info("Adding a tip to '{}' order has been completed", addTip.orderId()));
    }

    public void handle(FoodDelivered foodDelivered) {
        log.info("Setting '{}' order to COMPLETED state", foodDelivered.orderId());

        var storedEvents = eventStore.getEventsForOrder(foodDelivered.orderId());
        if (storedEvents.size() == 0) {
            throw new OrderingException(format("Failed to complete an '%s' order. There is no such order with provided id.", foodDelivered.orderId()));
        }
        var order = Order.from(storedEvents);

        Try.run(order::complete)
                .onSuccess(v -> publishSuccessEvent(order.getId(), new OrderCompleted(foodDelivered.orderId(), incrementVersionAndReturn(foodDelivered))))
                .onFailure(ex -> publishingFailureEvent(order.getId(), "Failed to complete an order.", foodDelivered.version(), ex))
                .andFinally(() -> log.info("Setting an '{}' order to COMPLETED state has been completed", foodDelivered.orderId()));
    }

    private static int incrementVersionAndReturn(IntegrationMessageBody causeMessage) {
        return causeMessage.version() + 1;
    }

    private void publishSuccessEvent(String orderId, IntegrationMessageBody eventObject) {
        log.info("Publishing success event: {}", eventObject);
        IntegrationMessage event = resultingEvent(orderId, eventObject);
        eventStore.store(event);
        publisher.send(event);
    }

    private void publishingFailureEvent(String id, String message, int version, Throwable ex) {
        log.error(message + " Publishing OrderProcessingError event", ex);
        IntegrationMessage event = resultingEvent(id, new OrderProcessingError(id, version, message, ex.getLocalizedMessage()));
        publisher.send(event);
    }

    private IntegrationMessage resultingEvent(String orderId, IntegrationMessageBody eventBody) {
        return new IntegrationMessage(eventHeader(orderId, eventBody.getClass().getSimpleName(), eventBody.version()), eventBody);
    }

    private Header eventHeader(String orderId, String type, int version) {
        return new Header(UUID.randomUUID().toString(), version, ORDERS_CHANNEL, type, orderId, clock.instant());
    }
}
