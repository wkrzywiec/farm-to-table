package io.wkrzywiec.fooddelivery.ordering.domain;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

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
