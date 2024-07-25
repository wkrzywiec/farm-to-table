package io.wkrzywiec.fooddelivery.bff.application.controller.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderDTO {

    private UUID id;
    private String customerId;
    private String farmId;
    private List<ItemDTO> items;
    private String address;
    private BigDecimal deliveryCharge;
}
