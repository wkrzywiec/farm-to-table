package io.wkrzywiec.fooddelivery.commons.infra.store.postgres;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Header;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.IntegrationMessage;
import io.wkrzywiec.fooddelivery.commons.infra.store.EventClassTypeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

@RequiredArgsConstructor
@Slf4j
class MessagePostgresRowMapper implements RowMapper<IntegrationMessage> {

    private final ObjectMapper objectMapper;
    private final EventClassTypeProvider caster;

    @Override
    public IntegrationMessage mapRow(ResultSet rs, int rowNum) throws SQLException {
        String eventType = rs.getString("type");
        IntegrationMessageBody body = extractBody(rs, eventType);
        return new IntegrationMessage(
                new Header(
                        rs.getString("id"),
                        rs.getInt("version"),
                        rs.getString("channel"),
                        eventType,
                        rs.getString("stream_id"),
                        extractInstant(rs, "created_at")
                ),
                body

        );
    }

    private static Instant extractInstant(ResultSet rs, String columnName) throws SQLException {
        if (rs.getTimestamp(columnName) == null) {
            return null;
        }
        return rs.getTimestamp(columnName).toInstant();
    }

    private IntegrationMessageBody extractBody(ResultSet rs, String eventType) throws SQLException {
        String bodyAsString = rs.getString("body");
        JsonNode bodyAsJsonNode = mapToJsonNode(bodyAsString);
        return mapEventBody(bodyAsJsonNode, eventType);
    }

    private JsonNode mapToJsonNode(String eventAsString) {
        try {
            return objectMapper.readTree(eventAsString);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse event to json: {}", eventAsString);
            throw new RuntimeException("Parsing error", e);
        }
    }

    private IntegrationMessageBody mapEventBody(JsonNode eventBody, String eventType) {
        Class<? extends IntegrationMessageBody> classType = caster.getClassType(eventType);
        return mapEventBody(eventBody, classType);
    }

    private <T extends IntegrationMessageBody> T mapEventBody(JsonNode eventBody, Class<T> valueType) {
        try {
            return objectMapper.treeToValue(eventBody, valueType);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse event json: {} to '{}' class", eventBody, valueType.getCanonicalName());
            throw new RuntimeException("Parsing error", e);
        }
    }
}
