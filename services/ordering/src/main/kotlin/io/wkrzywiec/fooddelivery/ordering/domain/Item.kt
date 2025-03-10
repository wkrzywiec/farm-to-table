package io.wkrzywiec.fooddelivery.ordering.domain

import lombok.Builder
import lombok.EqualsAndHashCode
import lombok.Getter
import lombok.ToString
import java.math.BigDecimal

@Getter
@EqualsAndHashCode
@ToString
@Builder
class Item {
    private val name: String? = null
    private val amount = 0
    private val pricePerItem: BigDecimal? = null
}
