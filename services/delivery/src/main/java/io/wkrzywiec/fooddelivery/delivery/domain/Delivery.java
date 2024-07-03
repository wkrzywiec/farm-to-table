package io.wkrzywiec.fooddelivery.delivery.domain;

import io.wkrzywiec.fooddelivery.commons.infra.store.DomainEvent;
import io.wkrzywiec.fooddelivery.delivery.domain.incoming.OrderCreated;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static java.lang.String.format;

@Getter
@EqualsAndHashCode
@ToString
public class Delivery {
    private UUID orderId;
    private int version;
    private String customerId;
    private String farmId;
    private String deliveryManId;
    private DeliveryStatus status;
    private String address;
    private List<Item> items;
    private BigDecimal deliveryCharge = new BigDecimal(0);
    private BigDecimal tip = new BigDecimal(0);
    private BigDecimal total = new BigDecimal(0);

    List<DomainEvent> changes = new ArrayList<>();

    Delivery() {};

    public static Delivery from(OrderCreated orderCreated) {
        var delivery = new Delivery();

        delivery.version = 0;
        delivery.orderId = orderCreated.orderId();
        delivery.customerId = orderCreated.customerId();
        delivery.farmId = orderCreated.farmId();
        delivery.status = DeliveryStatus.CREATED;
        delivery.address = orderCreated.address();
        delivery.items = mapItems(orderCreated.items());
        delivery.deliveryCharge = orderCreated.deliveryCharge();
        delivery.total = orderCreated.total();

        delivery.changes.add(
                new DeliveryEvent.DeliveryCreated(
                        delivery.orderId, delivery.version,
                        delivery.customerId, delivery.farmId,
                        delivery.address, delivery.items,
                        delivery.deliveryCharge, delivery.total
                )
        );
        return delivery;
    }

    private static List<Item> mapItems(List<io.wkrzywiec.fooddelivery.delivery.domain.incoming.Item> items) {
        return items.stream().map(dto -> Item.builder()
                .name(dto.name())
                .amount(dto.amount())
                .pricePerItem(dto.pricePerItem())
                .build()).toList();
    }

    public static Delivery from(List<DeliveryEvent> events) {
        Delivery delivery = null;
        for (DeliveryEvent event: events) {
            switch (event) {
                case DeliveryEvent.DeliveryCreated created -> {
                    delivery = new Delivery();

                    delivery.version = 0;
                    delivery.orderId = created.orderId();
                    delivery.customerId = created.customerId();
                    delivery.farmId = created.farmId();
                    delivery.status = DeliveryStatus.CREATED;
                    delivery.address = created.address();
                    delivery.items = created.items();
                    delivery.deliveryCharge = created.deliveryCharge();
                    delivery.total = created.total();
                    delivery.version = created.version();
                }

                case DeliveryEvent.TipAddedToDelivery tipAddedToDelivery -> {
                    delivery.tip = tipAddedToDelivery.tip();
                    delivery.total = tipAddedToDelivery.total();
                    delivery.version = tipAddedToDelivery.version();
                }

                case DeliveryEvent.DeliveryCanceled canceled -> {
                    delivery.status = DeliveryStatus.CANCELED;
                    delivery.version = canceled.version();
                }

                case DeliveryEvent.FoodInPreparation foodInPreparation -> {
                    delivery.status = DeliveryStatus.FOOD_IN_PREPARATION;
                    delivery.version = foodInPreparation.version();
                }

                case DeliveryEvent.DeliveryManAssigned deliveryManAssigned -> {
                    delivery.deliveryManId = deliveryManAssigned.deliveryManId();
                    delivery.version = deliveryManAssigned.version();
                }

                case DeliveryEvent.DeliveryManUnAssigned deliveryManUnAssigned -> {
                    delivery.deliveryManId = null;
                    delivery.version = deliveryManUnAssigned.version();
                }

                case DeliveryEvent.FoodIsReady foodIsReady -> {
                    delivery.status = DeliveryStatus.FOOD_READY;
                    delivery.version = foodIsReady.version();
                }

                case DeliveryEvent.FoodWasPickedUp foodWasPickedUp -> {
                    delivery.status = DeliveryStatus.FOOD_PICKED;
                    delivery.version = foodWasPickedUp.version();
                }

                case DeliveryEvent.FoodDelivered foodDelivered -> {
                    delivery.status = DeliveryStatus.FOOD_DELIVERED;
                    delivery.version = foodDelivered.version();
                }

                default -> throw new IllegalArgumentException("Failed to replay events to build delivery object. Unhandled events: " + event.getClass());
            }
        }
        return delivery;
    }

    public List<DomainEvent> uncommittedChanges() {
        return changes;
    }

    public void cancel(String reason, Instant cancellationTimestamp) {
        if (status != DeliveryStatus.CREATED) {
            throw new DeliveryException(format("Failed to cancel a %s delivery. It's not possible do it for a delivery with '%s' status", orderId, status));
        }
        this.status = DeliveryStatus.CANCELED;

        increaseVersion();
        changes.add(new DeliveryEvent.DeliveryCanceled(orderId, version, reason));
    }

    public void foodInPreparation(Instant foodPreparationTimestamp) {
        if (status != DeliveryStatus.CREATED) {
            throw new DeliveryException(format("Failed to start food preparation for an '%s' order. It's not possible do it for a delivery with '%s' status", orderId, status));
        }
        this.status = DeliveryStatus.FOOD_IN_PREPARATION;

        increaseVersion();
        changes.add(new DeliveryEvent.FoodInPreparation(orderId, version));
    }

    public void foodReady(Instant foodReadyTimestamp) {
        if (status != DeliveryStatus.FOOD_IN_PREPARATION) {
            throw new DeliveryException(format("Failed to set food ready for an '%s' order. It's not possible do it for a delivery with '%s' status", orderId, status));
        }
        this.status = DeliveryStatus.FOOD_READY;

        increaseVersion();
        changes.add(new DeliveryEvent.FoodIsReady(orderId, version));
    }

    public void pickUpFood(Instant foodPickedUpTimestamp) {
        if (status != DeliveryStatus.FOOD_READY) {
            throw new DeliveryException(format("Failed to set food as picked up for an '%s' order. It's not possible do it for a delivery with '%s' status", orderId, status));
        }
        this.status = DeliveryStatus.FOOD_PICKED;

        increaseVersion();
        changes.add(new DeliveryEvent.FoodWasPickedUp(orderId, version));
    }

    public void deliverFood(Instant foodDeliveredTimestamp) {
        if (status != DeliveryStatus.FOOD_PICKED) {
            throw new DeliveryException(format("Failed to set food as delivered for an '%s' order. It's not possible do it for a delivery with '%s' status", orderId, status));
        }
        this.status = DeliveryStatus.FOOD_DELIVERED;

        increaseVersion();
        changes.add(new DeliveryEvent.FoodDelivered(orderId, version));
    }

    public void assignDeliveryMan(String deliveryManId) {
        if (this.deliveryManId != null) {
            throw new DeliveryException(format("Failed to assign delivery man to an '%s' order. There is already a delivery man assigned with an orderId %s", orderId, this.deliveryManId));
        }

        if (List.of(DeliveryStatus.CANCELED, DeliveryStatus.FOOD_PICKED, DeliveryStatus.FOOD_DELIVERED).contains(status)) {
            throw new DeliveryException(format("Failed to assign a delivery man to an '%s' order. It's not possible do it for a delivery with '%s' status", orderId, status));
        }

        this.deliveryManId = deliveryManId;

        increaseVersion();
        changes.add(new DeliveryEvent.DeliveryManAssigned(orderId, version, deliveryManId));
    }

    public void unAssignDeliveryMan() {
        if (this.deliveryManId == null) {
            throw new DeliveryException(format("Failed to un assign delivery man from an '%s' order. There is no delivery man assigned to this delivery", orderId));
        }

        if (List.of(DeliveryStatus.CANCELED, DeliveryStatus.FOOD_PICKED, DeliveryStatus.FOOD_DELIVERED).contains(status)) {
            throw new DeliveryException(format("Failed to un assign a delivery man from an '%s' order. It's not possible do it for a delivery with '%s' status", orderId, status));
        }

        var unAssignedDeliveryManId = this.deliveryManId;
        this.deliveryManId = null;

        increaseVersion();
        changes.add(new DeliveryEvent.DeliveryManUnAssigned(orderId, version, unAssignedDeliveryManId));
    }

    public void addTip(BigDecimal tip, BigDecimal total) {
        this.tip = tip;
        this.total = total;

        increaseVersion();
        changes.add(new DeliveryEvent.TipAddedToDelivery(orderId, version, tip, total));
    }

    private void increaseVersion() {
        this.version = this.version + 1;
    }
}
