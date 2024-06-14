package io.wkrzywiec.fooddelivery.commons.infra.store.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wkrzywiec.fooddelivery.commons.event.IntegrationMessageBody;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Header;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.IntegrationMessage;
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

@Slf4j
@RequiredArgsConstructor
public class RedisEventStore implements EventStore {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final EventClassTypeProvider eventClassTypeProvider;
    private final String streamPrefix;

    @Override
    public void store(IntegrationMessage event) {
        log.info("Storing event in a stream '{}', body: '{}'", streamPrefix + event.body().orderId(), event);

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
                .withStreamKey(streamPrefix + event.body().orderId());

        RecordId recordId = redisTemplate.opsForStream()
                .add(record);

        log.info("Event was stored in stream: '{}', full message: '{}'. Record id: {}",
                streamPrefix + event.body().orderId(), messageJsonAsString, recordId.getValue());
    }

    @Override
    public void store(EventEntity event) {
        //todo implement me
    }

    @Override
    public List<IntegrationMessage> getEventsForOrder(String orderId) {
        log.info("Fetching events from '{}{}' Redis stream", streamPrefix, orderId);
        return getAllMessagesInStream(streamPrefix + orderId);
    }

    @Override
    public List<EventEntity> fetchEvents(String channel, String streamId) {
        //todo implement me
        return List.of();
    }

    private List<IntegrationMessage> getAllMessagesInStream(String stream) {

        var streamReadOptions = StreamReadOptions.empty()
                .block(Duration.ofMillis(1000))
                .count(100);

        List<ObjectRecord<String, String>> objectRecords = redisTemplate.opsForStream()
                .read(String.class, streamReadOptions, StreamOffset.create(stream, ReadOffset.from("0")));

        return objectRecords.stream()
                .map(Record::getValue)
                .map(this::mapToJsonNode)
                .map(this::mapToDomainEvent)
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

    private IntegrationMessage mapToDomainEvent(JsonNode eventAsJson) {
        var eventType = eventAsJson.get("header").get("type").asText();
        var eventBody =  eventAsJson.get("body");
        Class<? extends IntegrationMessageBody> classType = eventClassTypeProvider.getClassType(eventType);
        return new IntegrationMessage(mapEventHeader(eventAsJson), mapEventBody(eventBody, classType));
    }

    private Header mapEventHeader(JsonNode eventAsJson) {
        try {
            return objectMapper.treeToValue(eventAsJson.get("header"), Header.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse event header json: {}", eventAsJson);
            throw new RuntimeException("Parsing error", e);
        }
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
