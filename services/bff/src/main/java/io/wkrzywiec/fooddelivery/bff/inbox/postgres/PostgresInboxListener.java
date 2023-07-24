package io.wkrzywiec.fooddelivery.bff.inbox.postgres;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wkrzywiec.fooddelivery.bff.controller.model.*;
import io.wkrzywiec.fooddelivery.bff.inbox.InboxMessageProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;

@RequiredArgsConstructor
@Slf4j
public class PostgresInboxListener {

    private final JdbcTemplate jdbcTemplate;
    private final InboxMessageProcessor inboxMessageProcessor;
    private final ObjectMapper objectMapper;

    record RawInboxEntry(String id, String channel, String message) {};

    @Scheduled(fixedRate = 500, initialDelay = 5_000)
    public void checkInbox() throws JsonProcessingException {
        log.debug("Checking if there are any outstanding messages in inbox...");
        var rawInboxEntry = fetchNextMessageFromInbox();

        if (rawInboxEntry == null) {
            log.debug("There are no outstanding messages in inbox...");
            return;
        }
        //todo handle poison pill

        log.info("Publishing message from an inbox... id: '{}'", rawInboxEntry.id);

        switch (rawInboxEntry.channel) {
            case "ordering-inbox:create" ->
                    inboxMessageProcessor.createOrder(mapMessageTo(rawInboxEntry, CreateOrderDTO.class));
            case "ordering-inbox:cancel" ->
                    inboxMessageProcessor.cancelOrder(mapMessageTo(rawInboxEntry, CancelOrderDTO.class));
            case "ordering-inbox:tip" -> inboxMessageProcessor.addTip(mapMessageTo(rawInboxEntry, AddTipDTO.class));
            case "delivery-inbox:update" ->
                    inboxMessageProcessor.updateDelivery(mapMessageTo(rawInboxEntry, UpdateDeliveryDTO.class));
            case "delivery-inbox:delivery-man" ->
                    inboxMessageProcessor.changeDeliveryMan(mapMessageTo(rawInboxEntry, ChangeDeliveryManDTO.class));
            default -> log.warn("Unknown inbox channel: '{}'. Skipping sending it to a queue. Message: {}",
                    rawInboxEntry.channel,
                    rawInboxEntry.message);
        }

        removePublishedMessageFromInbox(rawInboxEntry);
    }

    private RawInboxEntry fetchNextMessageFromInbox() {
        return jdbcTemplate.queryForObject("""
                SELECT id, channel, message
                FROM inbox
                ORDER BY publish_timestamp
                LIMIT 1
                """, (resultSet, i) -> new RawInboxEntry(resultSet.getString(1), resultSet.getString(2), resultSet.getString(3)));
    }

    private <T> T mapMessageTo(RawInboxEntry rawInboxEntry, Class<T> requiredType) throws JsonProcessingException {
        return objectMapper.readValue(rawInboxEntry.message, requiredType);
    }

    private void removePublishedMessageFromInbox(RawInboxEntry rawInboxEntry) {
        jdbcTemplate.update("DELETE FROM inbox WHERE id = ?", rawInboxEntry.id);
        log.info("Message with an id '{}' was removed from inbox.", rawInboxEntry.id);
    }
}
