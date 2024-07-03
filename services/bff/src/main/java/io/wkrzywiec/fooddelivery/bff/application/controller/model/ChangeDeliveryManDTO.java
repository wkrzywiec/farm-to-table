package io.wkrzywiec.fooddelivery.bff.application.controller.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeDeliveryManDTO {
    private UUID orderId;
    private int version;
    private String deliveryManId;
}
