package io.wkrzywiec.fooddelivery.commons.infra.messaging.redis;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Message;
import io.wkrzywiec.fooddelivery.commons.infra.messaging.MessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;

@Slf4j
@RequiredArgsConstructor
public class RedisStreamPublisher implements MessagePublisher {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper mapper;
    @Override
    public void send(Message message) {
        log.info("Publishing '{}' message on channel: '{}', body: '{}'", message.header().type(), message.header().channel(), message.body());

        String messageJson = mapMessageToJsonString(message);
        ObjectRecord<String, String> record = prepareRedisRecord(message, messageJson);

        RecordId recordId = redisTemplate.opsForStream()
                .add(record);

        log.info("'{}' message was published on channel: '{}', full message: '{}'. Record id: {}",
                message.header().type(), message.header().channel(), message, recordId.getValue());
    }

    private String mapMessageToJsonString(Message message) {
        try {
            var messageJson = mapper.writeValueAsString(message);
            log.info(messageJson);
            return messageJson;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private static ObjectRecord<String, String> prepareRedisRecord(Message message, String messageJson) {
        return StreamRecords.newRecord()
                .ofObject(messageJson)
                .withStreamKey(message.header().channel());
    }
}
