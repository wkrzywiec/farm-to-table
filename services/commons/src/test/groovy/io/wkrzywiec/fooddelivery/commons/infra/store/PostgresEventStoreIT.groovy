package io.wkrzywiec.fooddelivery.commons.infra.store

import io.wkrzywiec.fooddelivery.commons.CommonsIntegrationTest
import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody
import io.wkrzywiec.fooddelivery.commons.infra.TestDomainEvent
import io.wkrzywiec.fooddelivery.commons.infra.store.postgres.PostgresEventStore
import spock.lang.Subject

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

import static io.wkrzywiec.fooddelivery.commons.infra.TestDomainEvent.aSampleEvent
import static io.wkrzywiec.fooddelivery.commons.infra.store.EventEntity.newEventEntity

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
        def eventBody = aSampleEvent(TEST_TIME)
        def testChannel = "test-channel"
        def event = newEventEntity(eventBody, testChannel, TEST_CLOCK)

        when:
        eventStore.store(event)

        then:
        List<EventEntity> storedEvents = eventStore.fetchEvents(testChannel, eventBody.streamId())
        storedEvents.size() == 1
        def storedEvent = storedEvents.get(0)
        storedEvent == event
    }

    private class IntegrationTestEventTestClassProvider implements EventClassTypeProvider {

        @Override
        Class<? extends IntegrationMessageBody> getClassType(String type) {
            return TestDomainEvent.class
        }
    }
}
