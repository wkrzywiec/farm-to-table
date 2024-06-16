package io.wkrzywiec.fooddelivery.commons.infra.store.postgres;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final EventPostgresRowMapper eventPostgresRowMapper;
    private final ObjectMapper objectMapper;

    public PostgresEventStore(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, EventClassTypeProvider eventClassTypeProvider) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.eventPostgresRowMapper = new EventPostgresRowMapper(objectMapper, eventClassTypeProvider);
    }

    @Override
    public void store(EventEntity event) {
        log.info("Saving event in a store. Event: {}", event);

        var bodyAsJsonString = mapEventData(event.data());

        jdbcTemplate.update("""
                insert into events(id, stream_id, version, channel, type, data, added_at)
                values(?, ?, ?, ?, ?, ?::jsonb, ?)
                """,
                event.id(),
                event.streamId(),
                event.version(),
                event.channel(),
                event.type(),
                bodyAsJsonString,
                Timestamp.from(event.addedAt())
        );
        log.info("Event was stored.");
    }

    private String mapEventData(DomainEvent domainEvent) throws RuntimeException {
        try {
            return objectMapper.writeValueAsString(domainEvent);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to map DomainMessageBody to JSON before storing event.", e);
        }
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
}
