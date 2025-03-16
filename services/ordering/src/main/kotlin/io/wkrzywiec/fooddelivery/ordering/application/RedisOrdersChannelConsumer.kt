package io.wkrzywiec.fooddelivery.ordering.application

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import io.wkrzywiec.fooddelivery.commons.infra.messaging.Header
import io.wkrzywiec.fooddelivery.commons.infra.messaging.redis.RedisStreamListener
import io.wkrzywiec.fooddelivery.ordering.domain.OrderingFacade
import io.wkrzywiec.fooddelivery.ordering.domain.incoming.FoodDelivered
import io.wkrzywiec.fooddelivery.ordering.domain.incoming.FoodInPreparation
import org.springframework.data.redis.connection.stream.MapRecord

private val logger = KotlinLogging.logger {}

class RedisOrdersChannelConsumer(private val facade: OrderingFacade, private val objectMapper: ObjectMapper) : RedisStreamListener {

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
        logger.info {"Message received from ${streamName()} stream: $message"}

        val payloadMessage = message.value["payload"]

        try {
            val messageAsJson = objectMapper.readTree(payloadMessage)
            val header = map(messageAsJson["header"], Header::class.java)

            when (header.type) {
                "CreateOrder" -> facade.handle(mapMessageBody(messageAsJson, CreateOrder::class.java))
                "CancelOrder" -> facade.handle(mapMessageBody(messageAsJson, CancelOrder::class.java))
                "FoodInPreparation" -> facade.handle(mapMessageBody(messageAsJson, FoodInPreparation::class.java))
                "AddTip" -> facade.handle(mapMessageBody(messageAsJson, AddTip::class.java))
                "FoodDelivered" -> facade.handle(mapMessageBody(messageAsJson, FoodDelivered::class.java))
                else -> logger.info { "There is not logic for handling ${header.type} message" }
            }
        } catch (e: JsonProcessingException) {
            throw RuntimeException(e)
        }
    }

    @Throws(JsonProcessingException::class)
    private fun <T> mapMessageBody(fullMessage: JsonNode, valueType: Class<T>): T {
        return objectMapper.treeToValue(fullMessage["body"], valueType)
    }

    @Throws(JsonProcessingException::class)
    private fun <T> map(fullMessage: JsonNode, valueType: Class<T>): T {
        return objectMapper.treeToValue(fullMessage, valueType)
    }
}
