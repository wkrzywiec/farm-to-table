package io.wkrzywiec.fooddelivery.bff.controller.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancelOrderDTO {

    private String orderId;
    private int version;
    private String reason;
}
