package io.wkrzywiec.fooddelivery.delivery.application

import com.github.javafaker.Faker
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Header
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Message
import io.wkrzywiec.fooddelivery.commons.infra.store.EventStore
import io.wkrzywiec.fooddelivery.commons.infra.store.redis.RedisEventStore
import io.wkrzywiec.fooddelivery.delivery.IntegrationTest
import io.wkrzywiec.fooddelivery.delivery.domain.DeliveryFacade
import io.wkrzywiec.fooddelivery.delivery.domain.outgoing.DeliveryCreated
import io.wkrzywiec.fooddelivery.delivery.infra.stream.RedisOrdersChannelConsumer
import io.wkrzywiec.fooddelivery.delivery.domain.incoming.Item
import io.wkrzywiec.fooddelivery.delivery.domain.incoming.OrderCreated
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.shaded.org.awaitility.Awaitility
import spock.lang.Subject

import java.time.Instant
import java.util.concurrent.TimeUnit

import static io.wkrzywiec.fooddelivery.delivery.domain.DeliveryTestData.aDelivery
import static io.wkrzywiec.fooddelivery.delivery.domain.ItemTestData.anItem

@ActiveProfiles(["redis-stream", "redis-event-store"])
@Subject([RedisOrdersChannelConsumer, DeliveryFacade, RedisEventStore])
class RedisProfileDeliveryProcessComponentTest extends IntegrationTest {

    @Autowired
    private EventStore eventStore

    def "Delivery is created"() {
        given:
        Faker faker = new Faker()
        var delivery = aDelivery()
                .withItems(
                        anItem().withName(faker.food().dish()).withPricePerItem(2.5),
                        anItem().withName(faker.food().dish()).withPricePerItem(3.0)
                )
                .withAddress(faker.address().fullAddress())

        def body = new OrderCreated(
                delivery.getOrderId(), 1, delivery.getCustomerId(), delivery.getFarmId(), delivery.getAddress(),
                delivery.getItems().stream().map(i -> new Item(i.getName(), i.getAmount(), i.getPricePerItem())).toList(),
                delivery.getDeliveryCharge(), delivery.getTotal())

        def header = new Header(UUID.randomUUID().toString(), 1, "orders", body.getClass().getSimpleName(), delivery.orderId, Instant.now())
        def message = new Message(header, body)

        when:
        redisStreamsClient.publishMessage(message)

        then:
        Awaitility.await().atMost(5, TimeUnit.SECONDS)
                .until {
                    def event = redisStreamsClient.getLatestMessageFromStreamAsJson("orders")

                    event.get("header").get("streamId").asText() == delivery.orderId
                    event.get("header").get("type").asText() == "DeliveryCreated"
                }

        and: "event is saved in event store"
        def events = eventStore.getEventsForOrder(delivery.orderId)
        events.size() == 1
        events[0].header().type() == "DeliveryCreated"

        def eventBody = events[0].body()
        eventBody instanceof DeliveryCreated
        eventBody as DeliveryCreated == new DeliveryCreated(
                delivery.orderId, 1, delivery.customerId,
                delivery.farmId, delivery.address,
                delivery.items.stream().map(i -> i.dto()).toList(),
                delivery.deliveryCharge, delivery.total)
    }
}
