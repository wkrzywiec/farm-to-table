package io.wkrzywiec.fooddelivery

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

class CustomerApplicationSpec extends IntegrationTest {

    @Autowired
    ApplicationContext context

    def "should load full Spring context"() {
        expect: "Spring context is loaded correctly"
        context
    }
}