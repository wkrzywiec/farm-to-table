package io.wkrzywiec.fooddelivery.bff.domain.inbox;

import io.wkrzywiec.fooddelivery.bff.application.controller.model.*;
import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;
import io.wkrzywiec.fooddelivery.commons.model.*;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Header;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.IntegrationMessage;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.MessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Component
public class InboxMessageProcessor {

    private static final String ORDERS_CHANNEL = "orders";
    private final MessagePublisher messagePublisher;
    private final Clock clock;

    public void createOrder(CreateOrderDTO createOrderDTO) {
        log.info("Received a command to create an order: {}", createOrderDTO);
        var command = command(createOrderDTO.getId(),
                new CreateOrder(
                        createOrderDTO.getId(), 1, createOrderDTO.getCustomerId(), createOrderDTO.getFarmId(),
                        createOrderDTO.getItems().stream().map(i -> new Item(i.name(), i.amount(), i.pricePerItem())).toList(),
                        createOrderDTO.getAddress(), createOrderDTO.getDeliveryCharge()));

        messagePublisher.send(command);
    }

    public void cancelOrder(CancelOrderDTO cancelOrderDTO) {
        log.info("Received a command to update an order: {}", cancelOrderDTO);
        var command = command(cancelOrderDTO.getOrderId(), new CancelOrder(cancelOrderDTO.getOrderId(), cancelOrderDTO.getVersion(), cancelOrderDTO.getReason()));

        messagePublisher.send(command);
    }

    public void addTip(AddTipDTO addTipDTO) {
        log.info("Received a command to change a tip for an order: {}", addTipDTO);
        var command = command(addTipDTO.getOrderId(), new AddTip(addTipDTO.getOrderId(), addTipDTO.getVersion(), addTipDTO.getTip()));

        messagePublisher.send(command);
    }

    public void updateDelivery(UpdateDeliveryDTO updateDeliveryDTO) {
        log.info("Received a command to update a delivery: {}", updateDeliveryDTO);
        var command = command(updateDeliveryDTO.getOrderId(), commandBody(updateDeliveryDTO));

        messagePublisher.send(command);
    }

    private IntegrationMessageBody commandBody(UpdateDeliveryDTO updateDeliveryDTO) {
        return switch (updateDeliveryDTO.getStatus()) {
            case "prepareFood" -> new PrepareFood(updateDeliveryDTO.getOrderId(), updateDeliveryDTO.getVersion());
            case "foodReady" -> new FoodReady(updateDeliveryDTO.getOrderId(), updateDeliveryDTO.getVersion());
            case "pickUpFood" -> new PickUpFood(updateDeliveryDTO.getOrderId(), updateDeliveryDTO.getVersion());
            case "deliverFood" -> new DeliverFood(updateDeliveryDTO.getOrderId(), updateDeliveryDTO.getVersion());
            default -> throw new RuntimeException(updateDeliveryDTO.getStatus() + " delivery status is not supported.");
        };
    }

    public void changeDeliveryMan(ChangeDeliveryManDTO changeDeliveryManDTO) {
        log.info("Received a command to set a delivery man for an order: {}", changeDeliveryManDTO);
        var command = command(changeDeliveryManDTO.getOrderId(), commandBody(changeDeliveryManDTO));

        messagePublisher.send(command);
    }

    private IntegrationMessage command(UUID orderId, IntegrationMessageBody commandBody) {
        return new IntegrationMessage(commandHeader(orderId, commandBody.getClass().getSimpleName()), commandBody);
    }

    private Header commandHeader(UUID orderId, String type) {
        return new Header(UUID.randomUUID(), 1, ORDERS_CHANNEL, type, orderId, clock.instant());
    }

    private IntegrationMessageBody commandBody(ChangeDeliveryManDTO changeDeliveryManDTO) {
        if (changeDeliveryManDTO.getDeliveryManId() == null) {
            return new UnAssignDeliveryMan(changeDeliveryManDTO.getOrderId(), changeDeliveryManDTO.getVersion());
        }
        return new AssignDeliveryMan(changeDeliveryManDTO.getOrderId(), changeDeliveryManDTO.getVersion(), changeDeliveryManDTO.getDeliveryManId());
    }
}
