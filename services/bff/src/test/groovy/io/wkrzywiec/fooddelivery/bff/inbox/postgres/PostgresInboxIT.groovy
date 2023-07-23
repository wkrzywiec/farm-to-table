package io.wkrzywiec.fooddelivery.bff.inbox.postgres

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.wkrzywiec.fooddelivery.bff.controller.model.AddTipDTO
import io.wkrzywiec.fooddelivery.bff.inbox.InboxPublisher
import io.wkrzywiec.fooddelivery.commons.IntegrationTest
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import spock.lang.Subject

import java.sql.Timestamp
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Subject([PostgresInboxPublisher, PostgresInboxListener])
@ActiveProfiles(["postgres-inbox", "redis-stream"])
class PostgresInboxIT extends IntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate
    private PostgresInboxPublisher publisher
    private Clock clock
    private ObjectMapper objectMapper

    def setup() {
        clock = Clock.fixed(Instant.parse("2023-07-23T18:15:30.12Z"), ZoneOffset.UTC)
        objectMapper = new ObjectMapper()
        publisher = new PostgresInboxPublisher(jdbcTemplate, clock, objectMapper)

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

    private Map<String, Object> fetchStoredMsgAsMap() {
        jdbcTemplate.queryForObject(
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
    }
}
