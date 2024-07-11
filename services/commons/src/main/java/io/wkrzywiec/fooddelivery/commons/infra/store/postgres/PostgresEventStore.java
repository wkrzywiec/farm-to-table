package io.wkrzywiec.fooddelivery.commons.infra.store.postgres;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wkrzywiec.fooddelivery.commons.infra.store.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Slf4j
public class PostgresEventStore implements EventStore {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final EventPostgresRowMapper eventPostgresRowMapper;
    private final ObjectMapper objectMapper;

    public PostgresEventStore(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper, EventClassTypeProvider eventClassTypeProvider) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.eventPostgresRowMapper = new EventPostgresRowMapper(objectMapper, eventClassTypeProvider);
    }

    @Override
    public void store(EventEntity event) {
        log.info("Saving event in a store. Event: {}", event);

        var bodyAsJsonString = mapEventData(event.data());

        SqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("event_id", event.id())
                .addValue("stream_id", event.streamId())
                .addValue("version", event.version())
                .addValue("channel", event.channel())
                .addValue("type", event.type())
                .addValue("data", bodyAsJsonString)
                .addValue("added_at", Timestamp.from(event.addedAt()));

        int insertedRowsCount = 0;
        try {
            insertedRowsCount = jdbcTemplate.update("""
                WITH latest_version AS (
                    SELECT MAX(version) AS value
                    FROM events
                    WHERE stream_id = :stream_id
                )
                INSERT INTO events(id, stream_id, version, channel, type, data, added_at)
                SELECT :event_id, :stream_id, :version, :channel, :type, :data::jsonb, :added_at
                FROM latest_version
                WHERE :version = value + 1 OR :version = 0
                """,
                    parameters
            );
        } catch (DuplicateKeyException exception) {
            log.error("Failed to persist event in event store due to duplicate key violation", exception);
            throw new InvalidEventVersionException();
        }

        if (insertedRowsCount == 0) {
            log.error("Event was not stored due to invalid event version");
            throw new InvalidEventVersionException();
        }
        log.info("Event was stored");
    }

    private String mapEventData(DomainEvent domainEvent) throws RuntimeException {
        try {
            return objectMapper.writeValueAsString(domainEvent);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to map DomainEvent to JSON before storing event.", e);
        }
    }

    @Override
    public List<EventEntity> loadEvents(String channel, UUID streamId) {

        SqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("stream_id", streamId)
                .addValue("channel", channel);

        return jdbcTemplate.query("""
                SELECT id, stream_id, version, channel, type, data, added_at
                FROM events
                WHERE channel = :channel AND stream_id = :stream_id ORDER BY version ASC;
                """,
                parameters,
                eventPostgresRowMapper);
    }
}
