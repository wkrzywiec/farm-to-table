package io.wkrzywiec.fooddelivery.commons.infra.store.postgres;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.IntegrationMessage;
import io.wkrzywiec.fooddelivery.commons.infra.store.DomainEvent;
import io.wkrzywiec.fooddelivery.commons.infra.store.EventClassTypeProvider;
import io.wkrzywiec.fooddelivery.commons.infra.store.EventEntity;
import io.wkrzywiec.fooddelivery.commons.infra.store.EventStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.util.List;

@Slf4j
public class PostgresEventStore implements EventStore {

    private final JdbcTemplate jdbcTemplate;
    private final MessagePostgresRowMapper messagePostgresRowMapper;
    private final EventPostgresRowMapper eventPostgresRowMapper;
    private final ObjectMapper objectMapper;

    public PostgresEventStore(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, EventClassTypeProvider eventClassTypeProvider) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.messagePostgresRowMapper = new MessagePostgresRowMapper(objectMapper, eventClassTypeProvider);
        this.eventPostgresRowMapper = new EventPostgresRowMapper(objectMapper, eventClassTypeProvider);
    }

    @Override
    public void store(IntegrationMessage event) throws RuntimeException {
        log.info("Saving event in a store. Event: {}", event);

        var bodyAsJsonString = mapEventBody(event.body());

        jdbcTemplate.update("""
                insert into events(id, stream_id, version, channel, type, body, created_at)
                values(?, ?, ?, ?, ?, ?::jsonb, ?)
                """,
                event.header().id(),
                event.header().streamId(),
                event.header().version(),
                event.header().channel(),
                event.header().type(),
                bodyAsJsonString,
                Timestamp.from(event.header().createdAt())
                );
        log.info("Event was stored.");
    }

    @Override
    public void store(EventEntity event) {
        log.info("Saving event in a store. Event: {}", event);

        var bodyAsJsonString = mapEventBody(event.data());

        jdbcTemplate.update("""
                insert into events(id, stream_id, version, channel, type, data)
                values(?, ?, ?, ?, ?, ?::jsonb)
                """,
                event.id(),
                event.streamId(),
                event.version(),
                event.channel(),
                event.type(),
                bodyAsJsonString
        );
        log.info("Event was stored.");
    }

    private String mapEventBody(IntegrationMessageBody body) throws RuntimeException {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to map DomainMessageBody to JSON before storing event.", e);
        }
    }

    private String mapEventBody(DomainEvent domainEvent) throws RuntimeException {
        try {
            return objectMapper.writeValueAsString(domainEvent);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to map DomainMessageBody to JSON before storing event.", e);
        }
    }

    @Override
    public List<IntegrationMessage> getEventsForOrder(String orderId) {
        log.info("Fetching events from event store for streamId: '{}'", orderId);
        return getAllMessagesInStream(orderId);
    }

    @Override
    public List<EventEntity> fetchEvents(String channel, String streamId) {

        return jdbcTemplate.query("""
                SELECT id, stream_id, version, channel, type, data, added_at
                FROM events
                WHERE channel = ? AND stream_id = ? ORDER BY version ASC;
                """,
                eventPostgresRowMapper,
                channel, streamId);
    }

    private List<IntegrationMessage> getAllMessagesInStream(String streamId) {

        return jdbcTemplate.query("""
                SELECT id, stream_id, version, channel, type, data, added_at
                FROM events
                WHERE stream_id = ? ORDER BY version ASC;
                """,
                messagePostgresRowMapper,
                streamId);
    }
}
