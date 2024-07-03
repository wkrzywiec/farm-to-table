package io.wkrzywiec.fooddelivery.delivery.domain;

import io.wkrzywiec.fooddelivery.commons.infra.store.EventEntity;
import lombok.Getter;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.*;

import static io.wkrzywiec.fooddelivery.commons.infra.store.EventEntity.newEventEntity;
import static io.wkrzywiec.fooddelivery.delivery.domain.DeliveryFacade.DELIVERY_CHANNEL;
import static java.lang.String.format;

@Getter
 public class DeliveryTestData {

    private UUID orderId = UUID.randomUUID();
    private String customerId = "default-customerId";
    private String farmId = "default-farmId";
    private String deliveryManId = null;
    private DeliveryStatus status = DeliveryStatus.CREATED;
    private String address = "Farm street, Naples, Italy";
    private List<ItemTestData> items = List.of(ItemTestData.anItem());
    private BigDecimal deliveryCharge = new BigDecimal(5);
    private BigDecimal tip = new BigDecimal(0);
    private BigDecimal total = new BigDecimal(0);
    private Map<String, String> metadata = new HashMap<>();

    private DeliveryTestData() {};

     public static DeliveryTestData aDelivery() {
        return new DeliveryTestData();
    }

     public Delivery entity() {
        Delivery delivery = createAnEmptyDelivery();
        setValue(delivery, "orderId", orderId);
        setValue(delivery, "customerId", customerId);
        setValue(delivery, "farmId", farmId);
        setValue(delivery, "deliveryManId", deliveryManId);
        setValue(delivery, "status", status);
        setValue(delivery, "address", address);
        setValue(delivery, "items", items.stream().map(ItemTestData::entity).toList());
        setValue(delivery, "deliveryCharge", deliveryCharge);
        setValue(delivery, "tip", tip);
         setValue(delivery, "total", total);
        setValue(delivery, "metadata", metadata);

        return delivery;
    }

    public EventEntity deliveryCreatedEntity(Clock clock) {
        return newEventEntity(deliveryCreated(), DELIVERY_CHANNEL, clock);
    }

    public DeliveryEvent deliveryCreated() {
         return new DeliveryEvent.DeliveryCreated(orderId, 0, customerId, farmId, address, items.stream().map(ItemTestData::entity).toList(), deliveryCharge, total);
    }

    public DeliveryTestData withOrderId(UUID orderId) {
        this.orderId = orderId;
        return this;
    }

     public DeliveryTestData withCustomerId(String customerId) {
        this.customerId = customerId;
        return this;
    }

     public DeliveryTestData withFarmId(String farmId) {
        this.farmId = farmId;
        return this;
    }

     public DeliveryTestData withDeliveryManId(String deliveryManId) {
        this.deliveryManId = deliveryManId;
        return this;
    }

     public DeliveryTestData withStatus(DeliveryStatus status) {
        this.status = status;
        return this;
    }

     public DeliveryTestData withAddress(String address) {
        this.address = address;
        return this;
    }

     public DeliveryTestData withItems(ItemTestData... items) {
        this.items = Arrays.asList(items);
        return this;
    }

     public DeliveryTestData withDeliveryCharge(double deliveryCharge) {
        this.deliveryCharge = new BigDecimal(deliveryCharge);
        return this;
    }

     public DeliveryTestData withTip(BigDecimal tip) {
        this.tip = tip;
        return this;
    }

    public DeliveryTestData withTotal(BigDecimal total) {
        this.total = total;
        return this;
    }

     public DeliveryTestData withMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
        return this;
    }

    private Delivery createAnEmptyDelivery() {
        try {
            var constructor = Delivery.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to construct Delivery entity class for tests", e);
        }
    }

    private void setValue(Delivery Delivery, String fieldName, Object value) {
        try {
            FieldUtils.writeField(Delivery, fieldName, value, true);

        } catch (IllegalAccessException e) {
            throw new RuntimeException(format("Failed to set a %s field in Delivery entity class for tests", fieldName), e);
        }
    }
}
