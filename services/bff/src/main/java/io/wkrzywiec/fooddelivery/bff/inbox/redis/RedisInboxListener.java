package io.wkrzywiec.fooddelivery.bff.inbox.redis;

import com.github.sonus21.rqueue.annotation.RqueueListener;
import io.wkrzywiec.fooddelivery.bff.controller.model.*;
import io.wkrzywiec.fooddelivery.bff.inbox.InboxMessageProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class RedisInboxListener {

    private final InboxMessageProcessor inboxMessageProcessor;

    @RqueueListener(value = "ordering-inbox:create")
    public void createOrder(CreateOrderDTO createOrderDTO) {
        inboxMessageProcessor.createOrder(createOrderDTO);
    }

    @RqueueListener(value = "ordering-inbox:cancel")
    public void cancelOrder(CancelOrderDTO cancelOrderDTO) {
       inboxMessageProcessor.cancelOrder(cancelOrderDTO);
    }

    @RqueueListener(value = "ordering-inbox:tip")
    public void addTip(AddTipDTO addTipDTO) {
        inboxMessageProcessor.addTip(addTipDTO);
    }

    @RqueueListener(value = "delivery-inbox:update")
    public void updateDelivery(UpdateDeliveryDTO updateDeliveryDTO) {
        inboxMessageProcessor.updateDelivery(updateDeliveryDTO);
    }

    @RqueueListener(value = "delivery-inbox:delivery-man")
    public void changeDeliveryMan(ChangeDeliveryManDTO changeDeliveryManDTO) {
        inboxMessageProcessor.changeDeliveryMan(changeDeliveryManDTO);
    }
}
