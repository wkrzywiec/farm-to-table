package io.wkrzywiec.fooddelivery.ordering.infra.stream

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Header
import io.wkrzywiec.fooddelivery.commons.infra.messaging.redis.RedisStreamListener
import io.wkrzywiec.fooddelivery.commons.model.AddTip
import io.wkrzywiec.fooddelivery.commons.model.CancelOrder
import io.wkrzywiec.fooddelivery.commons.model.CreateOrder
import io.wkrzywiec.fooddelivery.ordering.domain.OrderingFacade
import io.wkrzywiec.fooddelivery.ordering.domain.incoming.FoodDelivered
import io.wkrzywiec.fooddelivery.ordering.domain.incoming.FoodInPreparation
import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import org.springframework.data.redis.connection.stream.MapRecord

@Slf4j
@RequiredArgsConstructor
class RedisOrdersChannelConsumer : RedisStreamListener {
    private val facade: OrderingFacade? = null
    private val objectMapper: ObjectMapper? = null

    override fun streamName(): String {
        return "orders"
    }

    override fun group(): String {
        return "ordering"
    }

    override fun consumer(): String {
        return "1"
    }

    override fun onMessage(message: MapRecord<String?, String?, String?>) {
        RedisOrdersChannelConsumer.log.info("Message received from {} stream: {}", streamName(), message)

        val payloadMessage = message.value["payload"]

        try {
            val messageAsJson = objectMapper!!.readTree(payloadMessage)
            val header = map(messageAsJson["header"], Header::class.java)

            when (header.type) {
                "CreateOrder" -> facade!!.handle(mapMessageBody(messageAsJson, CreateOrder::class.java))
                "CancelOrder" -> facade!!.handle(mapMessageBody(messageAsJson, CancelOrder::class.java))
                "FoodInPreparation" -> facade!!.handle(mapMessageBody(messageAsJson, FoodInPreparation::class.java))
                "AddTip" -> facade!!.handle(mapMessageBody(messageAsJson, AddTip::class.java))
                "FoodDelivered" -> facade!!.handle(mapMessageBody(messageAsJson, FoodDelivered::class.java))
                else -> RedisOrdersChannelConsumer.log.info("There is no logic for handling {} message", header.type)
            }
        } catch (e: JsonProcessingException) {
            throw RuntimeException(e)
        }
    }

    @Throws(JsonProcessingException::class)
    private fun <T> mapMessageBody(fullMessage: JsonNode, valueType: Class<T>): T {
        return objectMapper!!.treeToValue(fullMessage["body"], valueType)
    }

    @Throws(JsonProcessingException::class)
    private fun <T> map(fullMessage: JsonNode, valueType: Class<T>): T {
        return objectMapper!!.treeToValue(fullMessage, valueType)
    }
}
