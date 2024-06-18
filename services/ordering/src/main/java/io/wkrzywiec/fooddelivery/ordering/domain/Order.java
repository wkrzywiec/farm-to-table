package io.wkrzywiec.fooddelivery.ordering.domain;

import io.wkrzywiec.fooddelivery.commons.infra.store.DomainEvent;
import io.wkrzywiec.fooddelivery.commons.model.CreateOrder;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.IntegrationMessage;
import io.wkrzywiec.fooddelivery.ordering.domain.outgoing.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.*;

import static io.wkrzywiec.fooddelivery.ordering.domain.OrderStatus.*;
import static java.lang.String.format;

@Getter
@EqualsAndHashCode
@ToString
public class Order {
    private String id;
    private int version;
    private String customerId;
    private String farmId;
    private OrderStatus status;
    private String address;
    private List<Item> items;
    private BigDecimal deliveryCharge;
    private BigDecimal tip = new BigDecimal(0);
    private BigDecimal total = new BigDecimal(0);

    List<DomainEvent> changes = new ArrayList<>();

    private Order() {}

    private Order(String id, String customerId, String farmId, List<Item> items, String address, BigDecimal deliveryCharge) {
        this(id, customerId, farmId, CREATED, address, items, deliveryCharge, BigDecimal.ZERO);
    }

    private Order(String id, String customerId, String farmId, OrderStatus status, String address, List<Item> items, BigDecimal deliveryCharge, BigDecimal tip) {
        if (id == null) {
            this.id = UUID.randomUUID().toString();
        } else {
            this.id = id;
        }
        this.customerId = customerId;
        this.farmId = farmId;
        this.status = status;
        this.address = address;
        this.items = items;
        this.deliveryCharge = deliveryCharge;
        this.tip = tip;
        this.calculateTotal();
    }

    static Order from(CreateOrder createOrder) {
        var order = new Order(
                createOrder.orderId(),
                createOrder.customerId(),
                createOrder.farmId(),
                mapItems(createOrder.items()),
                createOrder.address(),
                createOrder.deliveryCharge());

        order.changes.add(
                new OrderingEvent.OrderCreated(
                        order.id, 0,
                        order.customerId, order.farmId,
                        order.address, order.items,
                        order.deliveryCharge, order.total
                )
        );
        return order;
    }

    private static List<Item> mapItems(List<io.wkrzywiec.fooddelivery.commons.model.Item> items) {
        return items.stream().map(dto -> Item.builder()
                .name(dto.name())
                .amount(dto.amount())
                .pricePerItem(dto.pricePerItem())
                .build()).toList();
    }

    static Order from(List<OrderingEvent> events) {
        Order order = null;
        for (OrderingEvent event: events) {
            switch (event) {
                case OrderingEvent.OrderCreated created -> {
                    order = new Order();

                    order.id = created.orderId();
                    order.version = 0;
                    order.status = CREATED;
                    order.customerId = created.customerId();
                    order.farmId = created.farmId();
                    order.items = created.items();
                    order.address = created.address();
                    order.deliveryCharge = created.deliveryCharge();
                    order.tip = BigDecimal.ZERO;
                }

                case OrderingEvent.OrderCanceled canceled -> {
                    order.status = CANCELED;
                    order.version = canceled.version();
                }

                case OrderingEvent.OrderInProgress inProgress -> {
                    order.status = IN_PROGRESS;
                    order.version = inProgress.version();
                }

                case OrderingEvent.TipAddedToOrder tipAdded -> {
                    order.tip = tipAdded.tip();
                    order.version = tipAdded.version();
                }

                case OrderingEvent.OrderCompleted completed -> {
                    order.status = COMPLETED;
                    order.version = completed.version();
                }

                default -> throw new IllegalArgumentException("Failed to replay events to build order object. Unhandled events: " + event.getClass());
            }
        }
        return order;
    }

    List<DomainEvent> uncommittedChanges() {
        return changes;
    }

    void calculateTotal() {
        this.total = items.stream()
                .map(item -> item.getPricePerItem().multiply(BigDecimal.valueOf(item.getAmount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(deliveryCharge)
                .add(tip);
    }

    void cancelOrder(String reason) {
        if (status != OrderStatus.CREATED) {
            throw new OrderingException(format("Failed to cancel an %s order. It's not possible to cancel an order with '%s' status", id, status));
        }
        this.status = CANCELED;

        increaseVersion();
        changes.add(new OrderingEvent.OrderCanceled(id, version, reason));
    }

    void setInProgress() {
        if (status == CREATED) {
            this.status = IN_PROGRESS;

            increaseVersion();
            changes.add(new OrderingEvent.OrderInProgress(id, version));
            return;
        }
        throw new OrderingException(format("Failed to set an '%s' order to IN_PROGRESS. It's not allowed to do it for an order with '%s' status", id, status));
    }

    public void addTip(BigDecimal tip) {
        this.tip = tip;
        this.calculateTotal();

        increaseVersion();
        changes.add(new OrderingEvent.TipAddedToOrder(id, version, tip, total));
    }

    public void complete() {
        if (status == IN_PROGRESS) {
            this.status = COMPLETED;

            increaseVersion();
            changes.add(new OrderingEvent.OrderCompleted(id, version));
            return;
        }
        throw new OrderingException(format("Failed to set an '%s' order to COMPLETED. It's not allowed to do it for an order with '%s' status", id, status));
    }

    private void increaseVersion() {
        this.version = this.version + 1;
    }
}
