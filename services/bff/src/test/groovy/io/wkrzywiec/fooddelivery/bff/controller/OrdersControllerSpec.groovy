package io.wkrzywiec.fooddelivery.bff.controller

import io.wkrzywiec.fooddelivery.bff.application.controller.OrdersController
import io.wkrzywiec.fooddelivery.bff.application.controller.model.AddTipDTO
import io.wkrzywiec.fooddelivery.bff.application.controller.model.CancelOrderDTO
import io.wkrzywiec.fooddelivery.bff.application.controller.model.CreateOrderDTO
import io.wkrzywiec.fooddelivery.bff.application.controller.model.ItemDTO
import io.wkrzywiec.fooddelivery.bff.domain.inbox.inmemory.InMemoryInbox
import io.wkrzywiec.fooddelivery.bff.domain.inbox.Inbox
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification
import spock.lang.Subject

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print

@Subject(OrdersController)
@WebMvcTest(OrdersController)
@AutoConfigureMockMvc
class OrdersControllerSpec extends Specification {

    @SpringBean
    private Inbox inboxPublisher = new InMemoryInbox()

    @Autowired
    private MockMvc mockMvc

    def setup() {
        inboxPublisher.inboxes.clear()
    }

    def "Create an order"() {
        given:
        def requestBody = """
            {
              "customerId": "any-customer",
              "farmId": "good-farm",
              "items": [
                {
                  "name": "pizza",
                  "amount": 2,
                  "pricePerItem": 7.99
                }
              ],
              "address": "main road",
              "deliveryCharge": 5.25
            }
        """

        when: "Create an order"
        def result = mockMvc.perform(
                post("/orders")
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                        .andDo(print())

        then: "OrderId is generated"
        result.andExpect(status().isAccepted())
                .andExpect(jsonPath("orderId").isNotEmpty())

        and: "Message was sent to inbox"
        def inbox = inboxPublisher.inboxes.get("ordering-inbox:create")
        with(inbox.peek() as CreateOrderDTO) { it ->
            it.customerId == "any-customer"
            it.farmId == "good-farm"
            it.address == "main road"
            it.deliveryCharge == 5.25
            it.items == [ new ItemDTO("pizza", 2, 7.99)]
        }
    }

    def "Create an order and use provided id"() {
        given:
        def id = UUID.randomUUID()
        def requestBody = """
            {
              "id": "$id",
              "customerId": "any-customer",
              "farmId": "good-farm",
              "items": [
                {
                  "name": "pizza",
                  "amount": 2,
                  "pricePerItem": 7.99
                }
              ],
              "address": "main road",
              "deliveryCharge": 5.25
            }
        """

        when: "Create an order"
        def result = mockMvc.perform(
                post("/orders")
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())

        then: "OrderId is generated"
        result.andExpect(status().isAccepted())
                .andExpect(jsonPath("orderId").value(id.toString()))

        and: "Message was sent to inbox"
        def inbox = inboxPublisher.inboxes.get("ordering-inbox:create")
        with(inbox.peek() as CreateOrderDTO) { it ->
            it.id == id
        }
    }

    def "Cancel an order"() {
        given:
        def orderId = UUID.randomUUID()
        def requestBody = """
            {
              "reason": "not hungry"
            }
        """

        when: "Cancel an order"
        def result = mockMvc.perform(
                patch("/orders/$orderId/status/cancel")
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())

        then:
        result.andExpect(status().isAccepted())
                .andExpect(jsonPath("orderId").value(orderId.toString()))

        and: "Message was sent to inbox"
        def inbox = inboxPublisher.inboxes.get("ordering-inbox:cancel")
        with(inbox.peek() as CancelOrderDTO) { it ->
            it.orderId == orderId
            it.reason == "not hungry"
        }
    }

    def "Add tip to an order"() {
        given:
        def orderId = UUID.randomUUID()
        def requestBody = """
            {
              "tip": 10
            }
        """

        when: "Add tip"
        def result = mockMvc.perform(
                post("/orders/$orderId/tip")
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())

        then:
        result.andExpect(status().isAccepted())
                .andExpect(jsonPath("orderId").value(orderId.toString()))

        and: "Message was sent to inbox"
        def inbox = inboxPublisher.inboxes.get("ordering-inbox:tip")
        with(inbox.peek() as AddTipDTO) { it ->
            it.orderId == orderId
            it.tip == 10
        }
    }
}
