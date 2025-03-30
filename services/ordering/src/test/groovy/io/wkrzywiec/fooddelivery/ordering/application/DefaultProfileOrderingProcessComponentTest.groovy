//package io.wkrzywiec.fooddelivery.ordering.application
//
//import com.github.javafaker.Faker
//import io.wkrzywiec.fooddelivery.commons.infra.messaging.Header
//import io.wkrzywiec.fooddelivery.commons.infra.messaging.IntegrationMessage
//import io.wkrzywiec.fooddelivery.commons.infra.store.EventStore
//import io.wkrzywiec.fooddelivery.commons.infra.store.postgres.PostgresEventStore
//import io.wkrzywiec.fooddelivery.ordering.IntegrationTest
//import io.wkrzywiec.fooddelivery.ordering.domain.ItemTestData
//import io.wkrzywiec.fooddelivery.ordering.domain.OrderTestData
//import io.wkrzywiec.fooddelivery.ordering.domain.OrderingEvent
//import io.wkrzywiec.fooddelivery.ordering.domain.OrderingFacade
//import io.wkrzywiec.fooddelivery.ordering.infra.stream.RedisOrdersChannelConsumer
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.test.context.ActiveProfiles
//import spock.lang.Subject
//
//import java.time.Instant
//import java.util.concurrent.TimeUnit
//
//import static io.wkrzywiec.fooddelivery.ordering.domain.OrderingFacade.ORDERS_CHANNEL
//import static org.testcontainers.shaded.org.awaitility.Awaitility.await
//
//@ActiveProfiles(["redis-stream", "postgres-event-store"])
//@Subject([RedisOrdersChannelConsumer, OrderingFacade, PostgresEventStore])
//class DefaultProfileOrderingProcessComponentTest extends IntegrationTest {
//
//    @Autowired
//    private EventStore eventStore
//
//    def "Message is consumed and processed correctly"() {
//        given: "CreateOrder command"
//        Faker faker = new Faker()
//        var order = OrderTestData.anOrder()
//                .withItems(
//                        ItemTestData.anItem().withName(faker.food().dish()).withPricePerItem(2.5),
//                        ItemTestData.anItem().withName(faker.food().dish()).withPricePerItem(3.0)
//                )
//                .withAddress(faker.address().fullAddress())
//
//        def body = order.createOrder()
//        def header = new Header(UUID.randomUUID(), 1, "orders", body.getClass().getSimpleName(), order.id, Instant.now())
//        def message = new IntegrationMessage(header, body)
//
//        when: "is published"
//        redisStreamsClient.publishMessage(message)
//
//        then: "resulting event is published"
//        await().atMost(5, TimeUnit.SECONDS)
//                .until {
//                    def event = redisStreamsClient.getLatestMessageFromStreamAsJson("orders")
//                    event.get("header").get("streamId").asText() == order.id.toString()
//                    event.get("header").get("type").asText() == "OrderCreated"
//                }
//
//        and: "event is saved in event store"
//        def events = eventStore.loadEvents(ORDERS_CHANNEL, order.id)
//        events.size() == 1
//        events[0].type() == "OrderCreated"
//
//        def eventBody = events[0].data()
//        eventBody instanceof OrderingEvent.OrderCreated
//        eventBody as OrderingEvent.OrderCreated == new OrderingEvent.OrderCreated(
//                order.id, 0, order.customerId,
//                order.farmId, order.address,
//                order.items.stream().map(i -> i.entity()).toList(),
//                order.deliveryCharge, order.total())
//    }
//}