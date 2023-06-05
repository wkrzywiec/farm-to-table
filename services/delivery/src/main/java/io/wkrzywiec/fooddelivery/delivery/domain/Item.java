package io.wkrzywiec.fooddelivery.delivery.domain;

import lombok.*;

import java.math.BigDecimal;

@Getter
@EqualsAndHashCode
@ToString
@Builder
public class Item {
    private String name;
    private int amount;
    private BigDecimal pricePerItem;
}
