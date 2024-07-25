package io.wkrzywiec.fooddelivery.commons.infra.store.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wkrzywiec.fooddelivery.commons.infra.store.DomainEvent;
import io.wkrzywiec.fooddelivery.commons.infra.store.EventClassTypeProvider;
import io.wkrzywiec.fooddelivery.commons.infra.store.EventEntity;
import io.wkrzywiec.fooddelivery.commons.infra.store.EventStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.Record;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class RedisEventStore implements EventStore {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final EventClassTypeProvider eventClassTypeProvider;
    private final String streamPrefix;

    @Override
    public void store(EventEntity event) {
        log.info("Storing event in a stream '{}', body: '{}'", streamPrefix + event.streamId(), event);

        String messageJsonAsString;
        try {
            messageJsonAsString = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse event to json: {}", event);
            throw new RuntimeException("Parsing error", e);
        }

        log.info("Storing event: {}", messageJsonAsString);

        ObjectRecord<String, String> record = StreamRecords.newRecord()
                .ofObject(messageJsonAsString)
                .withStreamKey(streamPrefix + event.streamId());

        RecordId recordId = redisTemplate.opsForStream()
                .add(record);

        log.info("Event was stored in stream: '{}', full message: '{}'. Record id: {}",
                streamPrefix + event.streamId(), messageJsonAsString, recordId.getValue());
    }

    @Override
    public List<EventEntity> loadEvents(String channel, UUID streamId) {
        var streamReadOptions = StreamReadOptions.empty()
                .block(Duration.ofMillis(1000))
                .count(100);

        List<ObjectRecord<String, String>> objectRecords = redisTemplate.opsForStream()
                .read(String.class, streamReadOptions, StreamOffset.create(streamPrefix + streamId, ReadOffset.from("0")));

        return objectRecords.stream()
                .map(Record::getValue)
                .map(this::mapToJsonNode)
                .map(this::mapToEventEvent)
                .toList();
    }

    private JsonNode mapToJsonNode(String eventAsString) {
        try {
            return objectMapper.readTree(eventAsString);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse event to json: {}", eventAsString);
            throw new RuntimeException("Parsing error", e);
        }
    }

    private EventEntity mapToEventEvent(JsonNode eventAsJson) {
        var eventType = eventAsJson.get("type").asText();
        var eventData =  eventAsJson.get("data");
        Class<? extends DomainEvent> classType = eventClassTypeProvider.getClassType(eventType);
        return new EventEntity(
                UUID.fromString(eventAsJson.get("id").asText()),
                UUID.fromString(eventAsJson.get("streamId").asText()),
                eventAsJson.get("version").asInt(),
                eventAsJson.get("channel").asText(),
                eventType,
                mapEventData(eventData, classType),
                null);
    }

    private <T extends DomainEvent> T mapEventData(JsonNode eventData, Class<T> classType) {
        try {
            return objectMapper.treeToValue(eventData, classType);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse event json: {} to '{}' class", eventData, classType.getCanonicalName());
            throw new RuntimeException("Parsing error", e);
        }
    }
}
