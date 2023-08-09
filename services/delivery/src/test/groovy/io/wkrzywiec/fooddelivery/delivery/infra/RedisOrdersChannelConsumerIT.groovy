package io.wkrzywiec.fooddelivery.delivery.infra

import com.github.javafaker.Faker
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Header
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Message
import io.wkrzywiec.fooddelivery.delivery.IntegrationTestWithSpring
import io.wkrzywiec.fooddelivery.delivery.infra.stream.RedisOrdersChannelConsumer
import io.wkrzywiec.fooddelivery.delivery.domain.incoming.Item
import io.wkrzywiec.fooddelivery.delivery.domain.incoming.OrderCreated
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.shaded.org.awaitility.Awaitility
import spock.lang.Subject

import java.time.Instant
import java.util.concurrent.TimeUnit

import static io.wkrzywiec.fooddelivery.delivery.domain.DeliveryTestData.aDelivery
import static io.wkrzywiec.fooddelivery.delivery.domain.ItemTestData.anItem

@ActiveProfiles(["redis-stream", "redis-event-store"])
@Subject(RedisOrdersChannelConsumer)
class RedisOrdersChannelConsumerIT extends IntegrationTestWithSpring {

    def "Message is consumed correctly"() {
        given:
        Faker faker = new Faker()
        var delivery = aDelivery()
                .withItems(
                        anItem().withName(faker.food().dish()).withPricePerItem(2.5),
                        anItem().withName(faker.food().dish()).withPricePerItem(3.0)
                )
                .withAddress(faker.address().fullAddress())

        def body = new OrderCreated(
                delivery.getOrderId(), delivery.getCustomerId(), delivery.getFarmId(), delivery.getAddress(),
                delivery.getItems().stream().map(i -> new Item(i.getName(), i.getAmount(), i.getPricePerItem())).toList(),
                delivery.getDeliveryCharge(), delivery.getTotal())

        def header = new Header(UUID.randomUUID().toString(), "orders", body.getClass().getSimpleName(), delivery.orderId, Instant.now())
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
    }
}
