package io.wkrzywiec.fooddelivery.commons.infra.store

import io.wkrzywiec.fooddelivery.commons.CommonsIntegrationTest
import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Message
import spock.lang.Subject

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

import static io.wkrzywiec.fooddelivery.commons.infra.messaging.Message.firstMessage

@Subject(PostgresEventStore)
class PostgresEventStoreIT extends CommonsIntegrationTest {

    PostgresEventStore eventStore
    final Instant TEST_TIME = Instant.parse("2023-08-09T06:15:30.12Z")
    final Clock TEST_CLOCK = Clock.fixed(TEST_TIME, ZoneOffset.UTC)

    def setup() {
        eventStore = new PostgresEventStore(jdbcTemplate, objectMapper(), new IntegrationTestEventTestClassProvider())
        cleanDb()
    }

    def "One event is saved in event store"() {
        given:
        def eventBody = new IntegrationTestEventBody(
                "some test text", 88.23,
                BigDecimal.valueOf(1.23), true as boolean,
                TEST_TIME)
        def testChannel = "test-channel"
        def event = firstMessage(testChannel, TEST_CLOCK, eventBody)

        when:
        eventStore.store(event)

        then:
        List<Message> storedEvents = eventStore.getEventsForOrder(eventBody.orderId())
        storedEvents.size() == 1
        def storedEvent = storedEvents.get(0)
        storedEvent == event
    }

    private class IntegrationTestEventTestClassProvider implements EventClassTypeProvider {

        @Override
        Class<? extends DomainMessageBody> getClassType(String type) {
            return IntegrationTestEventBody.class
        }
    }
}

record IntegrationTestEventBody(String text, double number, BigDecimal money, boolean truth, Instant time) implements DomainMessageBody {
    @Override
    String orderId() {
        return text
    }
}
