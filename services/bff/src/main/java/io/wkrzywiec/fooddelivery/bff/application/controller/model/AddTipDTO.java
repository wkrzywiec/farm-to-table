package io.wkrzywiec.fooddelivery.bff.application.controller.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddTipDTO {
    private UUID orderId;
    private int version;
    private BigDecimal tip;
}