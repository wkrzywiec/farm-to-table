package io.wkrzywiec.fooddelivery.bff.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddTipDTO {
    private BigDecimal tip;
}