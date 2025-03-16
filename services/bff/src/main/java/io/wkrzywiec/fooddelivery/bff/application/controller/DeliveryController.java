package io.wkrzywiec.fooddelivery.bff.application.controller;

import io.wkrzywiec.fooddelivery.bff.application.controller.model.ChangeDeliveryManDTO;
import io.wkrzywiec.fooddelivery.bff.application.controller.model.ResponseDTO;
import io.wkrzywiec.fooddelivery.bff.application.controller.model.UpdateDeliveryDTO;
import io.wkrzywiec.fooddelivery.bff.domain.inbox.Inbox;
import io.wkrzywiec.fooddelivery.bff.domain.view.DeliveryViewRepository;
import io.wkrzywiec.fooddelivery.bff.domain.view.DeliveryView;
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
    ResponseEntity<ResponseDTO> updateADelivery(@PathVariable UUID orderId, @RequestBody UpdateDeliveryDTO updateDelivery) {
        log.info("Received request to update a delivery for an '{}' order, update: {}", orderId, updateDelivery);
        updateDelivery.setOrderId(orderId);
        inbox.storeMessage(DELIVERY_INBOX + ":update", updateDelivery);

        return ResponseEntity.accepted().body(new ResponseDTO(orderId));
    }

    @PostMapping("/deliveries/{orderId}/delivery-man")
    ResponseEntity<ResponseDTO> deliveryMan(@PathVariable UUID orderId, @RequestBody ChangeDeliveryManDTO changeDeliveryMan) {
        log.info("Received request to assign '{}' delivery man to an '{}' order", changeDeliveryMan.getDeliveryManId(), orderId);
        changeDeliveryMan.setOrderId(orderId);
        inbox.storeMessage(DELIVERY_INBOX + ":delivery-man", changeDeliveryMan);

        return ResponseEntity.accepted().body(new ResponseDTO(orderId));
    }
}
