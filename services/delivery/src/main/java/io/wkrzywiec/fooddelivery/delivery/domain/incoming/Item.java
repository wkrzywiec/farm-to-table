package io.wkrzywiec.fooddelivery.delivery.domain.incoming;

import java.math.BigDecimal;

public record Item(String name, int amount, BigDecimal pricePerItem) {
}
