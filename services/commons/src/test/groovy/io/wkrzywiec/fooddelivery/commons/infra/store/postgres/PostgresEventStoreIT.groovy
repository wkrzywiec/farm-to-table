package io.wkrzywiec.fooddelivery.commons.infra.store.postgres

import io.wkrzywiec.fooddelivery.commons.CommonsIntegrationTest
import io.wkrzywiec.fooddelivery.commons.infra.TestDomainEvent
import io.wkrzywiec.fooddelivery.commons.infra.store.DomainEvent
import io.wkrzywiec.fooddelivery.commons.infra.store.EventClassTypeProvider
import io.wkrzywiec.fooddelivery.commons.infra.store.EventEntity
import io.wkrzywiec.fooddelivery.commons.infra.store.InvalidEventVersionException
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
    final eventPostgresRowMapper = new EventPostgresRowMapper(objectMapper(), new EventForTestClassProvider())

    def setup() {
        eventStore = new PostgresEventStore(namedParameterJdbcTemplate, objectMapper(), new EventForTestClassProvider())
        cleanDb()
    }

    def "Event is saved in event store"() {
        given:
        def testChannel = "test-channel"
        EventEntity event = eventEntity(testChannel, TEST_TIME)

        when:
        eventStore.store(event)

        then:
        List<EventEntity> storedEvents = jdbcTemplate.query("SELECT * FROM events", eventPostgresRowMapper)
        storedEvents.size() == 1
        def storedEvent = storedEvents.get(0)
        storedEvent == event
    }

    def "Next event is saved in the event store if it has the succeeding version in the stream"() {
        given: "Event is already stored for a stream"
        def testChannel = "test-channel"

        EventEntity event = eventEntity(testChannel, TEST_TIME)
        eventStore.store(event)

        and: "there is another event with succeeding version is created"
        EventEntity nextEvent = eventEntity(event.streamId(), 1, testChannel, TEST_TIME.plusSeconds(1))

        when:
        eventStore.store(nextEvent)

        then:
        List<EventEntity> storedEvents = jdbcTemplate.query("SELECT * FROM events", eventPostgresRowMapper)
        storedEvents.size() == 2
        def storedEvent = storedEvents.get(1)
        storedEvent == nextEvent
    }

    def "Next event is not saved in the event store if it has the same version as the latest event in the stream."() {
        given: "Event is already stored for a stream"
        def testChannel = "test-channel"

        EventEntity event = eventEntity(testChannel, TEST_TIME)
        eventStore.store(event)

        and: "another event with the same version is created"
        EventEntity nextEvent = eventEntity(event.streamId(), 0, testChannel, TEST_TIME.plusSeconds(1))

        when:
        eventStore.store(nextEvent)

        then:
        thrown InvalidEventVersionException
    }

    def "Next event is not saved in the event store if it has the same version as one of the events in the stream."() {
        given: "3 events are already stored for a stream"
        def testChannel = "test-channel"
        def streamId = UUID.randomUUID()

        EventEntity event1 = eventEntity(streamId, 0, testChannel, TEST_TIME)
        EventEntity event2 = eventEntity(streamId, 1, testChannel, TEST_TIME.plusSeconds(1))
        EventEntity event3 = eventEntity(streamId, 2, testChannel, TEST_TIME.plusSeconds(2))

        eventStore.store([event1, event2, event3])

        and: "another event with the same version is created"
        EventEntity nextEvent = eventEntity(streamId, 1, testChannel, TEST_TIME.plusSeconds(10))

        when:
        eventStore.store(nextEvent)

        then:
        thrown InvalidEventVersionException
    }

    def "Next event is not saved in the event store if it has a version that skips couple versions in the stream."() {
        given: "Event is already stored for a stream"
        def testChannel = "test-channel"

        EventEntity event = eventEntity(testChannel, TEST_TIME)
        eventStore.store(event)

        and: "another event with the version that skips couple versions is created"
        EventEntity nextEvent = eventEntity(event.streamId(), 10, testChannel, TEST_TIME.plusSeconds(1))

        when:
        eventStore.store(nextEvent)

        then:
        thrown InvalidEventVersionException
    }

    def "Events are loaded from a store and are ordered by version"() {
        given:
        def testChannel = "test-channel"
        def streamId = UUID.randomUUID()

        EventEntity event1 = eventEntity(streamId, 0, testChannel, TEST_TIME)
        EventEntity event2 = eventEntity(streamId, 1, testChannel, TEST_TIME.plusSeconds(1))
        EventEntity event3 = eventEntity(streamId, 2, testChannel, TEST_TIME.plusSeconds(2))

        eventStore.store([event1, event2, event3])

        when:
        List<EventEntity> storedEvents = eventStore.loadEvents(testChannel, streamId)

        then:
        storedEvents.size() == 3
        storedEvents.get(0) == event1
        storedEvents.get(1) == event2
        storedEvents.get(2) == event3
    }

    private static EventEntity eventEntity(String testChannel, Instant addedAt) {
        def eventBody = aSampleEvent(addedAt)
        return newEventEntity(eventBody, testChannel, Clock.fixed(addedAt, ZoneOffset.UTC))
    }

    private static EventEntity eventEntity(UUID streamId, int version, String testChannel, Instant addedAt) {
        def eventBody = aSampleEvent(streamId, version, addedAt)
        return newEventEntity(eventBody, testChannel, Clock.fixed(addedAt, ZoneOffset.UTC))
    }

    private class EventForTestClassProvider implements EventClassTypeProvider {

        @Override
        Class<? extends DomainEvent> getClassType(String type) {
            return TestDomainEvent.class
        }
    }
}
