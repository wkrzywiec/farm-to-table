package io.wkrzywiec.fooddelivery.commons.infra.store.postgres

import io.wkrzywiec.fooddelivery.commons.CommonsIntegrationTest
import io.wkrzywiec.fooddelivery.commons.infra.TestDomainEvent
import io.wkrzywiec.fooddelivery.commons.infra.store.DomainEvent
import io.wkrzywiec.fooddelivery.commons.infra.store.EventClassTypeProvider
import io.wkrzywiec.fooddelivery.commons.infra.store.EventEntity
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
    final eventPostgresRowMapper = new EventPostgresRowMapper(objectMapper(), new EventForTestClassProvider())

    def setup() {
        eventStore = new PostgresEventStore(jdbcTemplate, objectMapper(), new EventForTestClassProvider())
        cleanDb()
    }

    def "Event is saved in event store"() {
        given:
        def eventBody = aSampleEvent(TEST_TIME)
        def testChannel = "test-channel"
        def event = newEventEntity(eventBody, testChannel, TEST_CLOCK)

        when:
        eventStore.store(event)

        then:
        List<EventEntity> storedEvents = jdbcTemplate.query("SELECT * FROM events", eventPostgresRowMapper)
        storedEvents.size() == 1
        def storedEvent = storedEvents.get(0)
        storedEvent == event
    }

    def "Events are loaded from a store and are ordered by version"() {
        given:
        def testChannel = "test-channel"
        def streamId = UUID.randomUUID()

        def eventBody1 = aSampleEvent(streamId, TEST_TIME)
        def event1 = newEventEntity(eventBody1, testChannel, TEST_CLOCK)

        def eventBody2 = aSampleEvent(streamId, 1, TEST_TIME)
        def event2 = newEventEntity(eventBody2, testChannel, Clock.fixed(TEST_TIME.plusSeconds(1), ZoneOffset.UTC))

        def eventBody3 = aSampleEvent(streamId, 2, TEST_TIME)
        def event3 = newEventEntity(eventBody3, testChannel, Clock.fixed(TEST_TIME.plusSeconds(2), ZoneOffset.UTC))

        eventStore.store(event1)
        eventStore.store(event2)
        eventStore.store(event3)

        when:
        List<EventEntity> storedEvents = eventStore.loadEvents(testChannel, streamId)

        then:
        storedEvents.size() == 3
        storedEvents.get(0) == event1
        storedEvents.get(1) == event2
        storedEvents.get(2) == event3
    }

    private class EventForTestClassProvider implements EventClassTypeProvider {

        @Override
        Class<? extends DomainEvent> getClassType(String type) {
            return TestDomainEvent.class
        }
    }
}
