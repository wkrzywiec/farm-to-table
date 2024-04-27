package io.wkrzywiec.fooddelivery.bff.inbox.postgres

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.wkrzywiec.fooddelivery.bff.IntegrationTest
import io.wkrzywiec.fooddelivery.bff.application.controller.model.AddTipDTO
import io.wkrzywiec.fooddelivery.bff.domain.inbox.InboxMessageProcessor
import io.wkrzywiec.fooddelivery.bff.domain.inbox.postgres.PostgresInbox
import io.wkrzywiec.fooddelivery.bff.domain.inbox.postgres.PostgresInboxListener
import io.wkrzywiec.fooddelivery.commons.infra.messaging.MessagePublisher
import org.springframework.dao.EmptyResultDataAccessException
import spock.lang.Subject

import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Subject([PostgresInbox, PostgresInboxListener])
class PostgresInboxIT extends IntegrationTest {

    private Clock clock
    private ObjectMapper objectMapper
    private InboxMessageProcessor inboxMessageProcessor
    private MessagePublisher messagePublisher = Mock()
    private PostgresInbox inboxPublisher
    private PostgresInboxListener inboxListener

    def setup() {
        clock = Clock.fixed(Instant.parse("2023-07-23T18:15:30.12Z"), ZoneOffset.UTC)
        objectMapper = new ObjectMapper()
        inboxPublisher = new PostgresInbox(jdbcTemplate, clock, objectMapper)
        inboxMessageProcessor = new InboxMessageProcessor(messagePublisher, clock)
        inboxListener = new PostgresInboxListener(jdbcTemplate, inboxMessageProcessor, objectMapper)
    }

    def "Message is stored in inbox"() {
        given: "new message"
        def channel = "ordering-inbox:tip"
        def addTip = new AddTipDTO("any-order-id", 2, BigDecimal.valueOf(10))

        when: "is stored"
        inboxPublisher.storeMessage(channel, addTip)
        def inboxEntryMap = fetchStoredMsgAsMap()

        then: "message id is stored"
        inboxEntryMap.id != null

        and: "channel name is stored"
        inboxEntryMap.channel == channel

        and: "publish timestamp is stored"
        clock.instant() == (Instant) inboxEntryMap.publish_timestamp

        and: "json message is stored"
        def messageAsString =  inboxEntryMap.message as String
        JsonNode storedMsgAsString = objectMapper.readTree(messageAsString)
        storedMsgAsString.get("orderId").asText() == "any-order-id"
        storedMsgAsString.get("tip").asDouble() == 10
    }

    def "A message from inbox is published"() {
        given: "message is in inbox"
        def channel = "ordering-inbox:tip"
        def addTip = new AddTipDTO("any-order-id", 2, BigDecimal.valueOf(10))
        inboxPublisher.storeMessage(channel, addTip)

        when: "check if there are outstanding messages in inbox"
        inboxListener.checkInbox()

        then: "message is no longer in inbox"
        def storedMessages = fetchStoredMsgAsMap()
        storedMessages.isEmpty()

        and: "message was published"
        1 * messagePublisher.send(_)
    }

    private Map<String, Object> fetchStoredMsgAsMap() {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT id, channel, message, publish_timestamp FROM inbox LIMIT 1",
                    (resultSet, i) -> {
                        Map<String, String> inboxEntryMap = new LinkedHashMap<String, String>()
                        inboxEntryMap.id = resultSet.getString("id")
                        inboxEntryMap.channel = resultSet.getString("channel")
                        inboxEntryMap.message = resultSet.getString("message")
                        def publishTimestampAsOffsetDateTime = resultSet.getObject("publish_timestamp", OffsetDateTime)
                        inboxEntryMap.publish_timestamp = publishTimestampAsOffsetDateTime.toInstant()
                        return inboxEntryMap
                    })
        } catch (EmptyResultDataAccessException ignored) {
            return Map.of()
        }

    }
}
