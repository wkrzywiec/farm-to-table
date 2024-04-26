package io.wkrzywiec.fooddelivery.bff.application.controller.model;

import java.math.BigDecimal;

public record ItemDTO(String name, int amount, BigDecimal pricePerItem) {
}
