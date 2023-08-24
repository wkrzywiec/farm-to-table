package io.wkrzywiec.fooddelivery.commons.infra.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wkrzywiec.fooddelivery.commons.event.DomainMessageBody;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.util.List;

@Slf4j
public class PostgresEventStore implements EventStore {

    private final JdbcTemplate jdbcTemplate;
    private final MessagePostgresRowMapper messagePostgresRowMapper;
    private final ObjectMapper objectMapper;

    public PostgresEventStore(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, EventClassTypeProvider eventClassTypeProvider) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        messagePostgresRowMapper = new MessagePostgresRowMapper(objectMapper, eventClassTypeProvider);
    }

    @Override
    public void store(Message event) throws RuntimeException {
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

    private String mapEventBody(DomainMessageBody body) throws RuntimeException {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to map DomainMessageBody to JSON before storing event.", e);
        }
    }

    @Override
    public List<Message> getEventsForOrder(String orderId) {
        log.info("Fetching events from event store for streamId: '{}'", orderId);
        return getAllMessagesInStream(orderId);
    }

    private List<Message> getAllMessagesInStream(String streamId) {

        return jdbcTemplate.query("""
                SELECT id, stream_id, version, channel, type, body, created_at
                FROM events
                WHERE stream_id = ? ORDER BY version ASC;
                """,
                messagePostgresRowMapper,
                streamId);
    }
}
