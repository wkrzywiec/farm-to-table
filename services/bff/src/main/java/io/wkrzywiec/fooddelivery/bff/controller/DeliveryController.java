package io.wkrzywiec.fooddelivery.bff.controller;

import io.wkrzywiec.fooddelivery.bff.inbox.Inbox;
import io.wkrzywiec.fooddelivery.bff.controller.model.ChangeDeliveryManDTO;
import io.wkrzywiec.fooddelivery.bff.controller.model.ResponseDTO;
import io.wkrzywiec.fooddelivery.bff.controller.model.UpdateDeliveryDTO;
import io.wkrzywiec.fooddelivery.bff.view.read.DeliveryViewRepository;
import io.wkrzywiec.fooddelivery.bff.view.create.DeliveryView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryViewRepository repository;
    private final Inbox inbox;
    private static final String DELIVERY_INBOX = "delivery-inbox";

    @GetMapping("/deliveries")
    ResponseEntity<List<DeliveryView>> getAllDeliveries() {
        var deliveryViews = repository.getAllDeliveryViews();
        return ResponseEntity.ok(deliveryViews);
    }

    @GetMapping("/deliveries/{orderId}")
    ResponseEntity<DeliveryView> getDeliveryById(@PathVariable String orderId) {
        var deliveryView = repository.getDeliveryViewById(orderId);

        if (deliveryView.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(deliveryView.get());
    }

    @PatchMapping("/deliveries/{orderId}")
    ResponseEntity<ResponseDTO> updateADelivery(@PathVariable String orderId, @RequestBody UpdateDeliveryDTO updateDelivery) {
        log.info("Received request to update a delivery for an '{}' order, update: {}", orderId, updateDelivery);
        updateDelivery.setOrderId(orderId);
        inbox.storeMessage(DELIVERY_INBOX + ":update", updateDelivery);

        return ResponseEntity.accepted().body(new ResponseDTO(orderId));
    }

    @PostMapping("/deliveries/{orderId}/delivery-man")
    ResponseEntity<ResponseDTO> deliveryMan(@PathVariable String orderId, @RequestBody ChangeDeliveryManDTO changeDeliveryMan) {
        log.info("Received request to assign '{}' delivery man to an '{}' order", changeDeliveryMan.getDeliveryManId(), orderId);
        changeDeliveryMan.setOrderId(orderId);
        inbox.storeMessage(DELIVERY_INBOX + ":delivery-man", changeDeliveryMan);

        return ResponseEntity.accepted().body(new ResponseDTO(orderId));
    }
}
