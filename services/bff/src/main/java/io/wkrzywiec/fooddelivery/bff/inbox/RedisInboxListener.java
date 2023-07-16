package io.wkrzywiec.fooddelivery.bff.inbox;

import com.github.sonus21.rqueue.annotation.RqueueListener;
import io.wkrzywiec.fooddelivery.bff.controller.model.*;
import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;
import io.wkrzywiec.fooddelivery.commons.incoming.*;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Header;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Message;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.MessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
public class RedisInboxListener {

    private static final String ORDERS_CHANNEL = "orders";
    private final MessagePublisher redisStreamPublisher;
    private final Clock clock;

    @RqueueListener(value = "ordering-inbox:create")
    public void createOrder(CreateOrderDTO createOrderDTO) {
        log.info("Received a command to create an order: {}", createOrderDTO);
        var command = command(createOrderDTO.getId(),
                new CreateOrder(
                        createOrderDTO.getId(), createOrderDTO.getCustomerId(), createOrderDTO.getFarmId(),
                        createOrderDTO.getItems().stream().map(i -> new Item(i.name(), i.amount(), i.pricePerItem())).toList(),
                        createOrderDTO.getAddress(), createOrderDTO.getDeliveryCharge()));

        redisStreamPublisher.send(command);
    }

    @RqueueListener(value = "ordering-inbox:cancel")
    public void updateOrder(CancelOrderDTO cancelOrderDTO) {
        log.info("Received a command to update an order: {}", cancelOrderDTO);
        var command = command(cancelOrderDTO.getOrderId(), new CancelOrder(cancelOrderDTO.getOrderId(), cancelOrderDTO.getReason()));

        redisStreamPublisher.send(command);
    }

    @RqueueListener(value = "ordering-inbox:tip")
    public void addTip(AddTipDTO addTipDTO) {
        log.info("Received a command to change a tip for an order: {}", addTipDTO);
        var command = command(addTipDTO.getOrderId(), new AddTip(addTipDTO.getOrderId(), addTipDTO.getTip()));

        redisStreamPublisher.send(command);
    }

    @RqueueListener(value = "delivery-inbox:update")
    public void updateDelivery(UpdateDeliveryDTO updateDeliveryDTO) {
        log.info("Received a command to update a delivery: {}", updateDeliveryDTO);
        var command = command(updateDeliveryDTO.getOrderId(), commandBody(updateDeliveryDTO));

        redisStreamPublisher.send(command);
    }

    private DomainMessageBody commandBody(UpdateDeliveryDTO updateDeliveryDTO) {
        return switch (updateDeliveryDTO.getStatus()) {
            case "prepareFood" -> new PrepareFood(updateDeliveryDTO.getOrderId());
            case "foodReady" -> new FoodReady(updateDeliveryDTO.getOrderId());
            case "pickUpFood" -> new PickUpFood(updateDeliveryDTO.getOrderId());
            case "deliverFood" -> new DeliverFood(updateDeliveryDTO.getOrderId());
            default -> throw new RuntimeException(updateDeliveryDTO.getStatus() + " delivery status is not supported.");
        };
    }

    @RqueueListener(value = "delivery-inbox:delivery-man")
    public void changeDeliveryMan(ChangeDeliveryManDTO changeDeliveryManDTO) {
        log.info("Received a command to set a delivery man for an order: {}", changeDeliveryManDTO);
        var command = command(changeDeliveryManDTO.getOrderId(), commandBody(changeDeliveryManDTO));

        redisStreamPublisher.send(command);
    }

    private Message command(String orderId, DomainMessageBody commandBody) {
        return new Message(commandHeader(orderId, commandBody.getClass().getSimpleName()), commandBody);
    }

    private Header commandHeader(String orderId, String type) {
        return new Header(UUID.randomUUID().toString(), ORDERS_CHANNEL, type, orderId, clock.instant());
    }

    private DomainMessageBody commandBody(ChangeDeliveryManDTO changeDeliveryManDTO) {
        if (changeDeliveryManDTO.getDeliveryManId() == null) {
            return new UnAssignDeliveryMan(changeDeliveryManDTO.getOrderId());
        }
        return new AssignDeliveryMan(changeDeliveryManDTO.getOrderId(), changeDeliveryManDTO.getDeliveryManId());
    }
}
