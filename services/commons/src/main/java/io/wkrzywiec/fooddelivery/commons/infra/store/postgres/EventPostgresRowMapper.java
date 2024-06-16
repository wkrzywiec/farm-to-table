package io.wkrzywiec.fooddelivery.commons.infra.store.postgres;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wkrzywiec.fooddelivery.commons.infra.store.DomainEvent;
import io.wkrzywiec.fooddelivery.commons.infra.store.EventClassTypeProvider;
import io.wkrzywiec.fooddelivery.commons.infra.store.EventEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

@RequiredArgsConstructor
@Slf4j
class EventPostgresRowMapper implements RowMapper<EventEntity> {

    private final ObjectMapper objectMapper;
    private final EventClassTypeProvider caster;

    @Override
    public EventEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
        String eventType = rs.getString("type");
        DomainEvent event = extractData(rs, eventType);
        return new EventEntity(
                rs.getString("id"),
                rs.getString("stream_id"),
                rs.getInt("version"),
                rs.getString("channel"),
                eventType,
                event,
                extractInstant(rs, "added_at")
        );
    }

    private static Instant extractInstant(ResultSet rs, String columnName) throws SQLException {
        if (rs.getTimestamp(columnName) == null) {
            return null;
        }
        return rs.getTimestamp(columnName).toInstant();
    }

    private DomainEvent extractData(ResultSet rs, String eventType) throws SQLException {
        String bodyAsString = rs.getString("data");
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

    private DomainEvent mapEventBody(JsonNode eventBody, String eventType) {
        Class<? extends DomainEvent> classType = caster.getClassType1(eventType);
        return mapEventBody(eventBody, classType);
    }

    private <T extends DomainEvent> T mapEventBody(JsonNode eventBody, Class<T> valueType) {
        try {
            return objectMapper.treeToValue(eventBody, valueType);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse event json: {} to '{}' class", eventBody, valueType.getCanonicalName());
            throw new RuntimeException("Parsing error", e);
        }
    }
}
