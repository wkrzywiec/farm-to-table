package io.wkrzywiec.fooddelivery.commons.model;

import java.math.BigDecimal;

public record Item(String name, int amount, BigDecimal pricePerItem) {
}
