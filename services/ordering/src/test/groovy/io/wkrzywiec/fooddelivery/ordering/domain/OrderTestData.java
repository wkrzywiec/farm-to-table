package io.wkrzywiec.fooddelivery.ordering.domain;

import io.wkrzywiec.fooddelivery.commons.model.CreateOrder;
import io.wkrzywiec.fooddelivery.ordering.domain.outgoing.OrderCreated;
import lombok.Getter;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.math.BigDecimal;
import java.util.*;

import static io.wkrzywiec.fooddelivery.ordering.domain.OrderStatus.CREATED;
import static java.lang.String.format;

@Getter
 class OrderTestData {

    private String id = UUID.randomUUID().toString();
    private int version = 1;
    private String customerId = "default-customer-id";
    private String farmId = "default-farm-id";
    private OrderStatus status = CREATED;
    private String address = "Pizza street, Naples, Italy";
    private List<ItemTestData> items = List.of(ItemTestData.anItem());
    private BigDecimal deliveryCharge = new BigDecimal(5);
    private BigDecimal tip = new BigDecimal(0);
    private Map<String, String> metadata = new HashMap<>();

    private OrderTestData() {};

     public static OrderTestData anOrder() {
        return new OrderTestData();
    }

     public Order entity() {
        Order order = createAnEmptyOrder();
        setValue(order, "id", id);
        setValue(order, "customerId", customerId);
        setValue(order, "farmId", farmId);
        setValue(order, "status", status);
        setValue(order, "address", address);
        setValue(order, "items", items.stream().map(ItemTestData::entity).toList());
        setValue(order, "deliveryCharge", deliveryCharge);
        setValue(order, "tip", tip);
        setValue(order, "metadata", metadata);

        order.calculateTotal();
        return order;
    }

     public CreateOrder createOrder() {
        return new CreateOrder(id, version, customerId, farmId, items.stream().map(ItemTestData::dto).toList(), address, deliveryCharge);
    }

    public OrderCreated orderCreated() {
         var entity = entity();
         return new OrderCreated(id, version, customerId, farmId, address, items.stream().map(ItemTestData::dto).toList(), deliveryCharge, entity.getTotal());
    }

    public BigDecimal total() {
         var entity = entity();
         return entity.getTotal();
    }

     public OrderTestData withId(String id) {
        this.id = id;
        return this;
    }

    public OrderTestData withVersion(int version) {
        this.version = version;
        return this;
    }

     public OrderTestData withCustomerId(String customerId) {
        this.customerId = customerId;
        return this;
    }

     public OrderTestData withFarmId(String farmId) {
        this.farmId = farmId;
        return this;
    }

    public OrderTestData withStatus(OrderStatus status) {
        this.status = status;
        return this;
    }

    public OrderTestData withAddress(String address) {
        this.address = address;
        return this;
    }

     public OrderTestData withItems(ItemTestData... items) {
        this.items = Arrays.asList(items);
        return this;
    }

    public OrderTestData withDeliveryCharge(double deliveryCharge) {
        this.deliveryCharge = new BigDecimal(deliveryCharge);
        return this;
    }

    public OrderTestData withTip(BigDecimal tip) {
        this.tip = tip;
        return this;
    }

    public OrderTestData withMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
        return this;
    }

    private Order createAnEmptyOrder() {
        try {
            var constructor = Order.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to construct Order entity class for tests", e);
        }
    }

    private void setValue(Order order, String fieldName, Object value) {
        try {
            FieldUtils.writeField(order, fieldName, value, true);

        } catch (IllegalAccessException e) {
            throw new RuntimeException(format("Failed to set a %s field in Order entity class for tests", fieldName), e);
        }
    }
}
