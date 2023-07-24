package io.wkrzywiec.fooddelivery.bff.inbox.postgres

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.wkrzywiec.fooddelivery.bff.controller.model.AddTipDTO
import io.wkrzywiec.fooddelivery.bff.inbox.InboxMessageProcessor
import io.wkrzywiec.fooddelivery.commons.IntegrationTest
import io.wkrzywiec.fooddelivery.commons.infra.messaging.MessagePublisher
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import spock.lang.Subject

import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Subject([PostgresInbox, PostgresInboxListener])
@ActiveProfiles(["postgres-inbox", "redis-stream"])
class PostgresInboxIT extends IntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate
    private Clock clock
    private ObjectMapper objectMapper
    @Autowired
    private InboxMessageProcessor inboxMessageProcessor
    @SpringBean
    private MessagePublisher messagePublisher = Mock()
    private PostgresInbox publisher
    private PostgresInboxListener listener

    def setup() {
        clock = Clock.fixed(Instant.parse("2023-07-23T18:15:30.12Z"), ZoneOffset.UTC)
        objectMapper = new ObjectMapper()
        publisher = new PostgresInbox(jdbcTemplate, clock, objectMapper)
        listener = new PostgresInboxListener(jdbcTemplate, inboxMessageProcessor, objectMapper)

        jdbcTemplate.execute("TRUNCATE inbox")
    }

    def "Message is stored in inbox"() {
        given: "new message"
        def channel = "ordering-inbox:tip"
        def addTip = new AddTipDTO("any-order-id", BigDecimal.valueOf(10))

        when: "is stored"
        publisher.storeMessage(channel, addTip)
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
        def addTip = new AddTipDTO("any-order-id", BigDecimal.valueOf(10))
        publisher.storeMessage(channel, addTip)

        when: "check if there are outstanding messages in inbox"
        listener.checkInbox()

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
        } catch (EmptyResultDataAccessException ex) {
            return Map.of()
        }

    }
}
