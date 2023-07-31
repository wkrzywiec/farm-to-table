package io.wkrzywiec.fooddelivery.ordering

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("prod")
class OrderingApplicationProdProfileSpec extends IntegrationTestWithSpring {

    @Autowired
    ApplicationContext context

    def "should load full Spring context"() {
        expect: "Spring context is loaded correctly"
        context
    }
}
