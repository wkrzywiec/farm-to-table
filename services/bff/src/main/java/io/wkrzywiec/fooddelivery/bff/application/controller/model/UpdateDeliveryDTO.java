package io.wkrzywiec.fooddelivery.bff.application.controller.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDeliveryDTO {

    private UUID orderId;
    private int version;
    @Schema(allowableValues = {"prepareFood", "foodReady", "pickUpFood", "deliverFood"}, required = true)
    private String status;
}
