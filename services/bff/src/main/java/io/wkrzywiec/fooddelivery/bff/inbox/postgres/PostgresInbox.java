package io.wkrzywiec.fooddelivery.bff.inbox.postgres;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wkrzywiec.fooddelivery.bff.inbox.Inbox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
public class PostgresInbox implements Inbox {

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    @Override
    public void storeMessage(String channel, Object message) throws RuntimeException {
        log.info("Storing a new message in '{}' inbox: ...", channel);

        String jsonMessage = mapToJson(message);
        String id = UUID.randomUUID().toString();
        jdbcTemplate.update(
                """
                INSERT INTO inbox (id, channel, message, publish_timestamp)
                VALUES (?, ?, ?::jsonb, ?)
                """,
                id, channel, jsonMessage, clock.instant().atOffset(ZoneOffset.UTC));

        log.info("Message was stored in inbox. Id: '{}', channel: '{}', message: {}", id, channel, jsonMessage);
    }

    private String mapToJson(Object message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Failed to parse message and put it into the inbox", ex);
        }
    }
}
