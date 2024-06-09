package io.wkrzywiec.fooddelivery.delivery.domain;

import io.wkrzywiec.fooddelivery.commons.infra.messaging.Message;
import io.wkrzywiec.fooddelivery.delivery.domain.incoming.OrderCreated;
import io.wkrzywiec.fooddelivery.delivery.domain.outgoing.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static java.lang.String.format;

@Getter
@EqualsAndHashCode
@ToString
public class Delivery {
    private String orderId;
    private String customerId;
    private String farmId;
    private String deliveryManId;
    private DeliveryStatus status;
    private String address;
    private List<Item> items;
    private BigDecimal deliveryCharge = new BigDecimal(0);
    private BigDecimal tip = new BigDecimal(0);
    private BigDecimal total = new BigDecimal(0);
    private Map<String, String> metadata = new HashMap<>();

    Delivery() {};

    public static Delivery from(OrderCreated orderCreated, Instant creationTimestamp) {
        var delivery = new Delivery();

        delivery.orderId = orderCreated.orderId();
        delivery.customerId = orderCreated.customerId();
        delivery.farmId = orderCreated.farmId();
        delivery.status = DeliveryStatus.CREATED;
        delivery.address = orderCreated.address();
        delivery.items = mapItems(orderCreated.items());
        delivery.deliveryCharge = orderCreated.deliveryCharge();
        delivery.total = orderCreated.total();

        Map<String, String> metadata = new HashMap<>();
        metadata.put("creationTimestamp", creationTimestamp.toString());
        delivery.metadata = metadata;
        return delivery;
    }

    private static List<Item> mapItems(List<io.wkrzywiec.fooddelivery.delivery.domain.incoming.Item> items) {
        return items.stream().map(dto -> Item.builder()
                .name(dto.name())
                .amount(dto.amount())
                .pricePerItem(dto.pricePerItem())
                .build()).toList();
    }

    public static Delivery from(List<Message> events) {
        Delivery delivery = null;
        for (Message event: events) {
            switch (event.body()) {
                case DeliveryCreated created -> {
                    delivery = new Delivery();

                    delivery.orderId = created.orderId();
                    delivery.customerId = created.customerId();
                    delivery.farmId = created.farmId();
                    delivery.status = DeliveryStatus.CREATED;
                    delivery.address = created.address();
                    delivery.items = mapItems(created.items());
                    delivery.deliveryCharge = created.deliveryCharge();
                    delivery.total = created.total();

                    Map<String, String> metadata = new HashMap<>();
                    metadata.put("creationTimestamp", event.header().createdAt().toString());
                    delivery.metadata = metadata;
                }

                case TipAddedToDelivery tipAddedToDelivery -> {
                    delivery.tip = tipAddedToDelivery.tip();
                    delivery.total = tipAddedToDelivery.total();
                }

                case DeliveryCanceled canceled -> {
                    var metadata = delivery.getMetadata();
                    metadata.put("cancellationReason", canceled.reason());
                    metadata.put("cancellationTimestamp", event.header().createdAt().toString());

                    delivery.metadata = metadata;
                    delivery.status = DeliveryStatus.CANCELED;
                }

                case FoodInPreparation foodInPreparation -> {
                    var metadata = delivery.getMetadata();
                    metadata.put("foodPreparationTimestamp", event.header().createdAt().toString());

                    delivery.metadata = metadata;
                    delivery.status = DeliveryStatus.FOOD_IN_PREPARATION;
                }

                case DeliveryManAssigned deliveryManAssigned -> {
                    delivery.deliveryManId = deliveryManAssigned.deliveryManId();
                }

                case DeliveryManUnAssigned deliveryManUnAssigned -> {
                    delivery.deliveryManId = null;
                }

                case FoodIsReady foodIsReady -> {
                    var metadata = delivery.getMetadata();
                    metadata.put("foodReadyTimestamp", event.header().createdAt().toString());

                    delivery.metadata = metadata;
                    delivery.status = DeliveryStatus.FOOD_READY;
                }

                case FoodWasPickedUp foodWasPickedUp -> {
                    var metadata = delivery.getMetadata();
                    metadata.put("foodPickedUpTimestamp", event.header().createdAt().toString());

                    delivery.metadata = metadata;
                    delivery.status = DeliveryStatus.FOOD_PICKED;
                }

                case FoodDelivered foodDelivered -> {
                    var metadata = delivery.getMetadata();
                    metadata.put("foodDeliveredTimestamp", event.header().createdAt().toString());

                    delivery.metadata = metadata;
                    delivery.status = DeliveryStatus.FOOD_DELIVERED;
                }

                default -> throw new IllegalStateException("Failed to replay events to build delivery object. Unhandled events: " + event.body().getClass());
            }
        }
        return delivery;
    }

    public void cancel(String reason, Instant cancellationTimestamp) {
        if (status != DeliveryStatus.CREATED) {
            throw new DeliveryException(format("Failed to cancel a %s delivery. It's not possible do it for a delivery with '%s' status", orderId, status));
        }
        this.status = DeliveryStatus.CANCELED;
        metadata.put("cancellationTimestamp", cancellationTimestamp.toString());

        if (reason != null) {
            metadata.put("cancellationReason", reason);
        }
    }

    public void foodInPreparation(Instant foodPreparationTimestamp) {
        if (status != DeliveryStatus.CREATED) {
            throw new DeliveryException(format("Failed to start food preparation for an '%s' order. It's not possible do it for a delivery with '%s' status", orderId, status));
        }
        this.status = DeliveryStatus.FOOD_IN_PREPARATION;
        metadata.put("foodPreparationTimestamp", foodPreparationTimestamp.toString());
    }

    public void foodReady(Instant foodReadyTimestamp) {
        if (status != DeliveryStatus.FOOD_IN_PREPARATION) {
            throw new DeliveryException(format("Failed to set food ready for an '%s' order. It's not possible do it for a delivery with '%s' status", orderId, status));
        }
        this.status = DeliveryStatus.FOOD_READY;
        metadata.put("foodReadyTimestamp", foodReadyTimestamp.toString());
    }

    public void pickUpFood(Instant foodPickedUpTimestamp) {
        if (status != DeliveryStatus.FOOD_READY) {
            throw new DeliveryException(format("Failed to set food as picked up for an '%s' order. It's not possible do it for a delivery with '%s' status", orderId, status));
        }
        this.status = DeliveryStatus.FOOD_PICKED;
        metadata.put("foodPickedUpTimestamp", foodPickedUpTimestamp.toString());
    }

    public void deliverFood(Instant foodDeliveredTimestamp) {
        if (status != DeliveryStatus.FOOD_PICKED) {
            throw new DeliveryException(format("Failed to set food as delivered for an '%s' order. It's not possible do it for a delivery with '%s' status", orderId, status));
        }
        this.status = DeliveryStatus.FOOD_DELIVERED;
        metadata.put("foodDeliveredTimestamp", foodDeliveredTimestamp.toString());
    }

    public void assignDeliveryMan(String deliveryManId) {
        if (this.deliveryManId != null) {
            throw new DeliveryException(format("Failed to assign delivery man to an '%s' order. There is already a delivery man assigned with an orderId %s", orderId, this.deliveryManId));
        }

        if (List.of(DeliveryStatus.CANCELED, DeliveryStatus.FOOD_PICKED, DeliveryStatus.FOOD_DELIVERED).contains(status)) {
            throw new DeliveryException(format("Failed to assign a delivery man to an '%s' order. It's not possible do it for a delivery with '%s' status", orderId, status));
        }

        this.deliveryManId = deliveryManId;
    }

    public void unAssignDeliveryMan() {
        if (this.deliveryManId == null) {
            throw new DeliveryException(format("Failed to un assign delivery man from an '%s' order. There is no delivery man assigned to this delivery", orderId));
        }

        if (List.of(DeliveryStatus.CANCELED, DeliveryStatus.FOOD_PICKED, DeliveryStatus.FOOD_DELIVERED).contains(status)) {
            throw new DeliveryException(format("Failed to un assign a delivery man from an '%s' order. It's not possible do it for a delivery with '%s' status", orderId, status));
        }

        this.deliveryManId = null;
    }

    public void addTip(BigDecimal tip, BigDecimal total) {
        this.tip = tip;
        this.total = total;
    }
}
